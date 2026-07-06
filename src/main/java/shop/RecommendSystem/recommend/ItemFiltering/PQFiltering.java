package shop.RecommendSystem.recommend.ItemFiltering;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import shop.RecommendSystem.dto.ItemFilteringVo;
import shop.RecommendSystem.dto.PqEntry;
import shop.RecommendSystem.dto.SearchResult;
import shop.RecommendSystem.repository.mapper.SearchMapper;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * ResNet-50 + PQ 기반 1차 후보 축소 + Phase 2 코사인 재랭킹.
 * <p>
 * Phase 1: 메모리 PQ 인덱스 → 비대칭 거리(asymmetric distance) → Top-K 후보
 * Phase 2: DB 에서 후보들의 fp16 임베딩 로드 → 정밀 코사인 유사도 → 최종 정렬
 */
@Component("resnet")
@Primary
@RequiredArgsConstructor
@Slf4j
public class PQFiltering implements ItemFiltering {

    private final SearchMapper searchMapper;

    @Value("${pq.codebook.path}")
    private String codebookPath;

    @Value("${pq.candidate.size:350}")
    private int candidateSize;

    // IVF coarse centroid 파일 (float[nlist][D]). 미설정/미존재 시 IVF 비활성 → 전수 스캔 폴백.
    @Value("${pq.coarse.path:}")
    private String coarsePath;

    // 질의당 탐색할 coarse cell 개수 (nprobe). 클수록 recall↑ 속도↓.
    @Value("${pq.nprobe:16}")
    private int nProbe;

    // 코드북: float[M][K][D_sub] — (64, 256, 32)
    private float[][][] codebook;
    private int M;
    private int K;
    private int dSub;
    private int dim;   // = M * dSub (전체 임베딩 차원, coarse centroid 차원과 일치해야 함)

    // IVF coarse centroids: float[nlist][dim]. ivfEnabled=false 면 null.
    private float[][] coarseCentroids;
    private boolean ivfEnabled;

    private List<PqEntry> pqIndex;
    private Map<Integer, List<PqEntry>> pqIndexByCoarse;   // IVF inverted list

    /**
     * 코드북만 파일에서 로드. PQ 인덱스(DB 적재) 는 InitializeSearchData 가
     * Redis 캐시와 함께 관리하므로 여기선 손대지 않음.
     */
    @PostConstruct
    public void init() throws IOException {
        log.info("===== PQFiltering init =====");
        long t0 = System.currentTimeMillis();
        codebook = loadCodebook(codebookPath);
        M = codebook.length;
        K = codebook[0].length;
        dSub = codebook[0][0].length;
        dim = M * dSub;
        log.info("[PQFilter] Codebook loaded: M={}, K={}, D_sub={} ({}ms)",
                M, K, dSub, System.currentTimeMillis() - t0);

        // ── IVF coarse centroids (선택적) ──────────────────────────────
        // 파일이 지정/존재하면 IVF 활성, 아니면 기존 전수 스캔으로 폴백.
        // 설정값을 따옴표로 감싸 로깅 → 경로 뒤에 공백/주석이 붙은 경우가 바로 드러남.
        log.info("[PQFilter] IVF 설정 확인 — pq.coarse.path=[{}], pq.nprobe={}", coarsePath, nProbe);
        if (coarsePath == null || coarsePath.isBlank()) {
            ivfEnabled = false;
            log.info("[PQFilter] IVF 비활성 — coarse 경로 미설정(빈 값). 전수 ADC 스캔으로 동작.");
        } else {
            java.io.File coarseFile = new java.io.File(coarsePath);
            if (!coarseFile.exists()) {
                ivfEnabled = false;
                log.warn("[PQFilter] IVF 비활성 — coarse 파일을 찾지 못함: 절대경로=[{}] "
                        + "(경로 오타/치환 실패/값 뒤 공백 여부 확인). 전수 ADC 스캔으로 동작.",
                        coarseFile.getAbsolutePath());
            } else {
                coarseCentroids = loadCoarseCentroids(coarsePath);
                if (coarseCentroids[0].length != dim) {
                    throw new IOException("Coarse centroid dim(" + coarseCentroids[0].length
                            + ") != embedding dim(" + dim + ")");
                }
                ivfEnabled = true;
                log.info("[PQFilter] IVF 활성 — coarse centroid 로드 완료: nlist={}, dim={}, nprobe={}, 파일=[{}]",
                        coarseCentroids.length, coarseCentroids[0].length, nProbe, coarseFile.getAbsolutePath());
            }
        }
    }

    /**
     * 외부(InitializeSearchData)에서 적재된 PQ 인덱스를 주입.
     * 호출 시점에 클래스별 파티션도 함께 빌드.
     * <p>
     * Redis 캐시 hit/miss 양쪽 경로 모두 이 메서드를 거치므로
     * pqIndex/pqIndexByClass 의 setup 은 항상 일관됨.
     */
    public void loadIndex(List<PqEntry> entries) {
        this.pqIndex = entries;
        // IVF inverted list: coarse_id → 해당 셀에 속한 엔트리들.
        // coarse_id 미적재(-1) 엔트리는 어떤 셀에도 안 잡혀 검색에서 누락되므로 개수를 경고.
        this.pqIndexByCoarse = entries.stream()
                .collect(Collectors.groupingBy(PqEntry::getCoarseId));
        long unassigned = pqIndexByCoarse.getOrDefault(-1, List.of()).size();
        if (unassigned > 0) {
            log.warn("[PQFilter] {} entries have no coarse_id (-1) — invisible under IVF. "
                    + "coarse_id backfill 필요.", unassigned);
        }
        log.info("[PQFilter] Index loaded: {} entries, coarse cells: {}",
                entries.size(), pqIndexByCoarse.size());
    }

    /**
     *  queryEmbedding 질의 임베딩 (float[2048])
     *  classFilter    null 이면 전체 검색, 아니면 해당 detected_class 만
     * @param resultSize     최종 반환 개수
     * @param id             본인 상품 제외 (null 허용)
     */


//    @Override
//    public List<SearchResult> searchSimilarItems(Map searchCondition, int resultSize, Long id) {
//        return List.of();
//    }
    @Override
    public List<SearchResult> searchSimilarItem(ItemFilteringVo searchParam, int resultSize, Long id) {

        float[] queryEmbedding = decodeFp16(searchParam.getEmbeddingBytes());
        String classFilter = searchParam.getClassFilter();

        long t0 = System.currentTimeMillis();

        // 1. Lookup 테이블 생성 — float[M][K]
        float[][] lookup = buildLookupTable(queryEmbedding);
        long t1 = System.currentTimeMillis();

        // 2. IVF — coarse cell nprobe개 선택 후 해당 셀 엔트리만 스캔 대상으로.
        //    class 는 셀 선택에 관여하지 않고 후처리(스캔 시)에서만 필터링.
        List<PqEntry> scanList = collectScanList(queryEmbedding, classFilter);
        log.info("[PQFilter] 검색 모드={}, 전체 인덱스={}건 → 스캔 대상={}건 (nprobe={}, classFilter={})",
                ivfEnabled ? "IVF" : "전수(폴백)",
                pqIndex.size(), scanList.size(),
                ivfEnabled ? nProbe : "-",
                classFilter == null ? "없음" : classFilter);

        // 3. 비대칭 거리 + Top-N 후보 추출 (max-heap 으로 K-th 미만만 유지)
        PriorityQueue<float[]> heap = new PriorityQueue<>(
                candidateSize, (a, b) -> Float.compare(b[0], a[0]));

        for (int i = 0; i < scanList.size(); i++) {
            float dist = asymmetricDistance(lookup, scanList.get(i).getPqCode());
            if (heap.size() < candidateSize) {
                heap.offer(new float[]{dist, i});
            } else if (dist < heap.peek()[0]) {
                heap.poll();
                heap.offer(new float[]{dist, i});
            }
        }
        List<String> topUuids = new ArrayList<>(heap.size());
        for (float[] item : heap) {
            topUuids.add(scanList.get((int) item[1]).getImageUuid());
        }
        long t2 = System.currentTimeMillis();

        // 4. Phase 2 — DB 에서 fp16 임베딩 + 상품 정보 로드
        if (topUuids.isEmpty()) {
            log.warn("[PQFilter] No candidates found (filter={})", classFilter);
            return List.of();
        }
        List<SearchResult> candidates = searchMapper.findResnet50Phase2Targets(topUuids, id);
        long t3 = System.currentTimeMillis();

        // 5. 코사인 유사도 계산
        for (SearchResult c : candidates) {
            float[] emb = decodeFp16(c.getEmbeddingValue());
            c.setCosineSimilarity(cosineSimilarity(queryEmbedding, emb));
            c.setEmbeddingValue(null); // 응답에서 BLOB 제거 (페이로드 절감)
        }

        // 6. 코사인 내림차순 정렬
        candidates.sort(Comparator.comparing(SearchResult::getCosineSimilarity).reversed());
        long t4 = System.currentTimeMillis();

        log.info("[PQFilter] scan={}, lookup={}ms, phase1={}ms, db={}ms, phase2={}ms, total={}ms",
                scanList.size(), t1 - t0, t2 - t1, t3 - t2, t4 - t3, t4 - t0);

        return candidates.subList(0, Math.min(resultSize, candidates.size()));
    }

    // ════════════════════════════════════════
    // 내부 계산 — lookup / asymmetric distance / cosine
    // ════════════════════════════════════════

    /**
     * 스캔 대상 엔트리 목록 구성.
     * <p>
     * IVF 활성: 질의와 가까운 coarse cell nprobe개를 골라 그 셀들의 엔트리만 합침.
     * IVF 비활성: 전체 인덱스(전수 스캔 폴백).
     * <p>
     * classFilter 가 있으면 셀 선택과 무관하게 detected_class 가 일치하는 엔트리만 남김(후처리).
     */
    private List<PqEntry> collectScanList(float[] queryEmbedding, String classFilter) {
        List<PqEntry> base;
        if (ivfEnabled) {
            int[] probes = selectProbes(queryEmbedding);
            base = new ArrayList<>();
            for (int cell : probes) {
                List<PqEntry> bucket = pqIndexByCoarse.get(cell);
                if (bucket != null) base.addAll(bucket);
            }
        } else {
            base = pqIndex;
        }
        if (classFilter == null || classFilter.isEmpty()) {
            return base;
        }
        List<PqEntry> filtered = new ArrayList<>(base.size());
        for (PqEntry e : base) {
            if (classFilter.equals(e.getDetectedClass())) filtered.add(e);
        }
        return filtered;
    }

    /**
     * 질의 임베딩과 가장 가까운 coarse centroid nProbe개의 cell id 반환.
     * 전체 nlist 에 대해 L2² 거리를 구한 뒤 상위 nProbe개 선택(부분 정렬).
     */
    private int[] selectProbes(float[] query) {
        int nList = coarseCentroids.length;
        int p = Math.min(nProbe, nList);
        // (distance, cellId) 를 max-heap 으로 유지하며 가장 가까운 p개만 보존
        PriorityQueue<float[]> heap = new PriorityQueue<>(p, (a, b) -> Float.compare(b[0], a[0]));
        for (int c = 0; c < nList; c++) {
            float[] centroid = coarseCentroids[c];
            float d = 0f;
            for (int i = 0; i < dim; i++) {
                float diff = query[i] - centroid[i];
                d += diff * diff;
            }
            if (heap.size() < p) {
                heap.offer(new float[]{d, c});
            } else if (d < heap.peek()[0]) {
                heap.poll();
                heap.offer(new float[]{d, c});
            }
        }
        int[] cells = new int[heap.size()];
        int idx = 0;
        for (float[] h : heap) cells[idx++] = (int) h[1];
        return cells;
    }

    private float[][] buildLookupTable(float[] embedding) {
        float[][] lookup = new float[M][K];
        for (int m = 0; m < M; m++) {
            int subStart = m * dSub;
            float[][] centroids = codebook[m];
            for (int k = 0; k < K; k++) {
                float[] c = centroids[k];
                float d = 0f;
                for (int i = 0; i < dSub; i++) {
                    float diff = embedding[subStart + i] - c[i];
                    d += diff * diff;
                }
                lookup[m][k] = d;
            }
        }
        return lookup;
    }

    private float asymmetricDistance(float[][] lookup, byte[] pqCode) {
        float dist = 0f;
        for (int m = 0; m < M; m++) {
            int code = pqCode[m] & 0xFF;  // unsigned
            dist += lookup[m][code];
        }
        return dist;
    }

    static double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("dim mismatch: " + a.length + " vs " + b.length);
        }
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0 || nb == 0) return 0;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    // ════════════════════════════════════════
    // fp16 (IEEE 754 half) → float[] 디코더
    //
    // FastAPI 측에서 numpy.float16 으로 저장된 임베딩(4,096 byte = 2 byte × 2048)을
    // Java float[2048] 로 복원. JDK 21+ 의 Float.float16ToFloat 미사용 (Java 17 호환).
    // ════════════════════════════════════════

    public static float[] decodeFp16(byte[] bytes) {
        if (bytes == null) return new float[0];
        int n = bytes.length / 2;                                              // fp16 = 2 byte
        float[] out = new float[n];
        // numpy 의 fp16 저장은 little-endian (낮은 바이트가 먼저)
        ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < n; i++) {
            out[i] = halfToFloat(buf.getShort());
        }
        return out;
    }

    /**
     * IEEE 754 half-precision (16-bit) → single-precision (32-bit) 변환.
     * <p>
     * half  : [ sign 1 | exp 5 | mantissa 10 ]   = 16 bit, exponent bias = 15
     * float : [ sign 1 | exp 8 | mantissa 23 ]   = 32 bit, exponent bias = 127
     * <p>
     * 변환 규칙:
     * • exp 가 0      → 0 또는 비정규수(subnormal). 비정규는 정규화하면서 exp 보정.
     * • exp 가 0x1F   → 무한대 또는 NaN.
     * • 그 외 정상값   → exp 에 (127-15)=112 더해서 bias 차이 흡수, mantissa 는 <<13 으로 확장.
     */
    static float halfToFloat(short hbits) {
        int h = hbits & 0xFFFF;          // 부호 확장 방지
        int sign = (h >>> 15) & 0x1;        // 최상위 1 비트
        int exp = (h >>> 10) & 0x1F;       // 5 비트 지수
        int mant = h & 0x3FF;               // 10 비트 가수

        int bits;
        if (exp == 0) {
            if (mant == 0) {
                // ±0
                bits = sign << 31;
            } else {
                // 비정규수(subnormal half) → 정규화된 float 로 변환.
                // mantissa 를 왼쪽으로 시프트하면서 hidden bit(0x400) 을 만들고 exp 를 보정.
                exp = 1;
                while ((mant & 0x400) == 0) {
                    mant <<= 1;
                    exp--;
                }
                mant &= ~0x400;                                          // hidden bit 제거
                bits = (sign << 31) | ((exp + (127 - 15)) << 23) | (mant << 13);
            }
        } else if (exp == 31) {
            // exp 가 모두 1 → 무한대(mant=0) 또는 NaN(mant≠0). float 에서도 exp=0xFF.
            bits = (sign << 31) | 0x7F800000 | (mant << 13);
        } else {
            // 정상값: bias 차이만 보정.
            //   half exp 1..30  → float exp 113..142  (+112)
            //   mantissa 는 비트 폭이 10→23 으로 확장되므로 <<13.
            bits = (sign << 31) | ((exp + (127 - 15)) << 23) | (mant << 13);
        }
        return Float.intBitsToFloat(bits);
    }

    // ════════════════════════════════════════
    // NumPy .npy 파서 (v1.0 ~ v3.0, dtype='<f4', C-order)
    //
    // 파일 구조 (NEP-1 명세):
    //   ┌────────────────────────────────────────────────────────────┐
    //   │ offset 0: 매직 6 byte    "\x93NUMPY"                       │
    //   │ offset 6: 메이저 버전 1 byte                                │
    //   │ offset 7: 마이너 버전 1 byte                                │
    //   │ offset 8: 헤더 길이                                         │
    //   │   - v1.0       : uint16 LE  (2 byte)                       │
    //   │   - v2.0/v3.0  : uint32 LE  (4 byte)  (long shape 지원용)  │
    //   │ 다음 N byte    : ASCII 헤더 (Python dict 리터럴)            │
    //   │     예: "{'descr': '<f4', 'fortran_order': False,           │
    //   │           'shape': (64, 256, 32), }"                       │
    //   │ 이후 ~파일끝   : raw 데이터 (LE float32, C-order)          │
    //   └────────────────────────────────────────────────────────────┘
    //
    // 본 메서드는 PQ 코드북 (M, K, D_sub) = (64, 256, 32) float32 = 2 MB 를 가정.
    // train_pq_codebook.py 에서 np.save(path, codebook.astype(np.float32)) 로 저장됨.
    // ════════════════════════════════════════

    static float[][][] loadCodebook(String path) throws IOException {
        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(new FileInputStream(path)))) {

            // ── 1. 매직 6 byte 검증 ──────────────────────────────────
            // "\x93NUMPY" 가 아니면 정상 NPY 파일이 아님.
            byte[] magic = new byte[6];
            in.readFully(magic);
            if (magic[0] != (byte) 0x93 || magic[1] != 'N' || magic[2] != 'U'
                    || magic[3] != 'M' || magic[4] != 'P' || magic[5] != 'Y') {
                throw new IOException("Not a NPY file: " + path);
            }

            // ── 2. 버전 (메이저.마이너) ──────────────────────────────
            // 메이저에 따라 헤더 길이 인코딩이 달라짐.
            //   v1.0 은 uint16(2byte), v2.0~v3.0 은 uint32(4byte) 사용.
            int major = in.readUnsignedByte();
            in.readUnsignedByte();                                              // minor — 본 파서는 사용 안 함

            // ── 3. 헤더 길이 ────────────────────────────────────────
            // 모두 little-endian 으로 저장됨. DataInputStream 은 기본 big-endian 이라
            // readShort/readInt 사용 불가 → 바이트별로 읽어 LE 로 합산.
            int headerLen;
            if (major == 1) {
                int b0 = in.readUnsignedByte();
                int b1 = in.readUnsignedByte();
                headerLen = (b1 << 8) | b0;                                      // LE uint16
            } else if (major == 2 || major == 3) {
                int b0 = in.readUnsignedByte();
                int b1 = in.readUnsignedByte();
                int b2 = in.readUnsignedByte();
                int b3 = in.readUnsignedByte();
                headerLen = (b3 << 24) | (b2 << 16) | (b1 << 8) | b0;            // LE uint32
            } else {
                throw new IOException("Unsupported NPY version: " + major);
            }

            // ── 4. 헤더 ASCII 문자열 읽기 ────────────────────────────
            // Python dict 리터럴이 그대로 들어있음. 예:
            //   {'descr': '<f4', 'fortran_order': False, 'shape': (64, 256, 32), }
            // 64 byte 정렬을 위한 공백 패딩이 포함될 수 있지만 정규식 파싱에 무해.
            byte[] headerBytes = new byte[headerLen];
            in.readFully(headerBytes);
            String header = new String(headerBytes, StandardCharsets.US_ASCII);

            // ── 5. dtype 검증 ────────────────────────────────────────
            // '<f4' = little-endian float32. 본 파서는 이것만 지원.
            //   '<' little-endian, 'f' float, '4' 4-byte (=32bit).
            // '>f4' (big-endian) 이나 '<f8' (float64) 가 오면 거부.
            Matcher dmatch = Pattern.compile("'descr':\\s*'([^']+)'").matcher(header);
            if (!dmatch.find()) throw new IOException("Cannot parse descr: " + header);
            String descr = dmatch.group(1);
            if (!"<f4".equals(descr)) {
                throw new IOException("Expected '<f4' (little-endian float32), got '" + descr + "'");
            }

            // ── 6. 메모리 레이아웃 검증 ─────────────────────────────
            // numpy 는 기본 C-order (row-major) 로 저장. Fortran-order 면 인덱싱 순서가
            // 달라져서 동일 코드로 못 읽음 → 거부.
            if (header.contains("'fortran_order': True")) {
                throw new IOException("Fortran-order NPY not supported");
            }

            // ── 7. shape 파싱 (M, K, D_sub) ─────────────────────────
            // 정규식으로 (...) 안의 내용만 뽑고 콤마로 분리. 본 파서는 3D 만 허용.
            //   trailing comma "(64, 256, 32,)" 인 경우도 split 결과의 빈 문자열은 skip.
            Matcher smatch = Pattern.compile("'shape':\\s*\\(([^)]*)\\)").matcher(header);
            if (!smatch.find()) throw new IOException("Cannot parse shape: " + header);
            String[] dimStr = smatch.group(1).split(",");
            int[] dims = new int[3];
            int validDims = 0;
            for (String s : dimStr) {
                String trimmed = s.trim();
                if (trimmed.isEmpty()) continue;                                 // trailing comma 흡수
                if (validDims >= 3) {
                    throw new IOException("Only 3D codebook supported: " + header);
                }
                dims[validDims++] = Integer.parseInt(trimmed);
            }
            if (validDims != 3) {
                throw new IOException("Expected 3D codebook, got " + validDims + "D: " + header);
            }

            // ── 8. 데이터 영역 일괄 읽기 ────────────────────────────
            // C-order 라 [m][k][d] 순서로 d 가 가장 빠르게 증가. M*K*D 개의 float32 가
            // 연속 저장돼 있으니 전체를 byte[] 로 한 번에 읽고 FloatBuffer 로 묶어서
            // 행 단위(D 개)로 jvm 배열에 bulk copy → readFloat 반복보다 훨씬 빠름.
            int M = dims[0], K = dims[1], D = dims[2];
            int totalFloats = M * K * D;

            byte[] dataBytes = new byte[totalFloats * 4];                        // float32 = 4 byte
            in.readFully(dataBytes);
            FloatBuffer fb = ByteBuffer.wrap(dataBytes)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .asFloatBuffer();

            // ── 9. 3D 배열로 복사 ────────────────────────────────────
            // codebook[m][k] 의 float[D] 에 fb.get(...) 로 D 개 일괄 복사.
            // C-order 이므로 fb 의 진행 순서와 [m][k][d] 의 진행 순서가 일치.
            float[][][] codebook = new float[M][K][D];
            for (int m = 0; m < M; m++) {
                for (int k = 0; k < K; k++) {
                    fb.get(codebook[m][k]);                                      // D 개 float 한 번에
                }
            }
            return codebook;
        }
    }

    /**
     * IVF coarse centroids 로더 — 2D '<f4' C-order npy (nlist, dim).
     * loadCodebook 과 동일한 NPY 파싱을 쓰되 shape 만 2D 로 검증.
     * train_ivf_coarse.py 에서 np.save(path, centroids.astype(np.float32)) 로 저장.
     */
    static float[][] loadCoarseCentroids(String path) throws IOException {
        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(new FileInputStream(path)))) {

            byte[] magic = new byte[6];
            in.readFully(magic);
            if (magic[0] != (byte) 0x93 || magic[1] != 'N' || magic[2] != 'U'
                    || magic[3] != 'M' || magic[4] != 'P' || magic[5] != 'Y') {
                throw new IOException("Not a NPY file: " + path);
            }

            int major = in.readUnsignedByte();
            in.readUnsignedByte();                                              // minor

            int headerLen;
            if (major == 1) {
                int b0 = in.readUnsignedByte(), b1 = in.readUnsignedByte();
                headerLen = (b1 << 8) | b0;                                      // LE uint16
            } else if (major == 2 || major == 3) {
                int b0 = in.readUnsignedByte(), b1 = in.readUnsignedByte();
                int b2 = in.readUnsignedByte(), b3 = in.readUnsignedByte();
                headerLen = (b3 << 24) | (b2 << 16) | (b1 << 8) | b0;            // LE uint32
            } else {
                throw new IOException("Unsupported NPY version: " + major);
            }

            byte[] headerBytes = new byte[headerLen];
            in.readFully(headerBytes);
            String header = new String(headerBytes, StandardCharsets.US_ASCII);

            Matcher dmatch = Pattern.compile("'descr':\\s*'([^']+)'").matcher(header);
            if (!dmatch.find()) throw new IOException("Cannot parse descr: " + header);
            if (!"<f4".equals(dmatch.group(1))) {
                throw new IOException("Expected '<f4', got '" + dmatch.group(1) + "'");
            }
            if (header.contains("'fortran_order': True")) {
                throw new IOException("Fortran-order NPY not supported");
            }

            Matcher smatch = Pattern.compile("'shape':\\s*\\(([^)]*)\\)").matcher(header);
            if (!smatch.find()) throw new IOException("Cannot parse shape: " + header);
            String[] dimStr = smatch.group(1).split(",");
            int[] dims = new int[2];
            int validDims = 0;
            for (String s : dimStr) {
                String trimmed = s.trim();
                if (trimmed.isEmpty()) continue;
                if (validDims >= 2) throw new IOException("Only 2D coarse centroids supported: " + header);
                dims[validDims++] = Integer.parseInt(trimmed);
            }
            if (validDims != 2) throw new IOException("Expected 2D centroids, got " + validDims + "D: " + header);

            int nList = dims[0], D = dims[1];
            byte[] dataBytes = new byte[nList * D * 4];
            in.readFully(dataBytes);
            FloatBuffer fb = ByteBuffer.wrap(dataBytes).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer();

            float[][] centroids = new float[nList][D];
            for (int c = 0; c < nList; c++) {
                fb.get(centroids[c]);
            }
            return centroids;
        }
    }

}
