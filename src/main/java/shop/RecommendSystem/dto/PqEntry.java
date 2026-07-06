package shop.RecommendSystem.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 메모리 PQ 인덱스 1행. image_info 테이블의 (image_uuid, pq_code, detected_class) 대응.
 *
 * Redis 캐시 적재 대상이라 Serializable.
 * serialVersionUID 는 인덱스 캐시 호환성 관리용 — 필드 추가/변경 시 bump 하여
 * 구버전 캐시를 자동 무효화시킬 수 있음.
 */
@Getter
@Setter
public class PqEntry implements Serializable {
    // coarseId(IVF) 필드 추가로 구버전 캐시 무효화 → 2L 로 bump
    private static final long serialVersionUID = 2L;

    private String imageUuid;
    private byte[] pqCode;
    private String detectedClass;

    // IVF coarse cell 번호. coarse_id 컬럼 미적재(NULL) 시 -1.
    private int coarseId = -1;

    public PqEntry() {}
}
