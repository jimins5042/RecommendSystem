package shop.RecommendSystem.recommend.ImageFeature;

import info.debatty.java.lsh.MinHash;
import lombok.extern.slf4j.Slf4j;
import nu.pattern.OpenCV;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.ORB;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;


@Service
@Slf4j
public class ExtractByORB {
    static {
        // OpenCV 라이브러리 로드
        OpenCV.loadLocally();
        log.info("OpenCV loaded");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutdown Hook: Cleaning OpenCV resources");
            System.gc(); // 가비지 컬렉션 강제 실행
        }));
    }

    private final HashFunction rowHashFunction = Hashing.murmur3_32(); // 행 해싱용
    private final HashFunction finalHashFunction = Hashing.sha256(); // 최종 해싱용
    private final ORB orb = ORB.create(256, 1.2f, 8);


    public String getImageFeature(String imgUrl) throws IOException {
        String bitFlagList = new String();
        try {
            log.info("Downloading image from URL: {}", imgUrl);

            // S3 URL에서 이미지 다운로드 및 BufferedImage 생성
            URL url = new URL(imgUrl);
            BufferedImage img = ImageIO.read(url);
            if (img == null) {
                throw new IOException("Failed to read image from URL: " + imgUrl);
            }

            // BufferedImage를 Mat 객체로 변환
            Mat image = bufferedImageToMat(img);
            log.info("Image size: {}", image.size());

            bitFlagList = extractDescriptors(image);


        } catch (Exception e) {
            log.error("Error processing image from URL: {}", imgUrl, e);
            log.info("Error: " + e.getMessage());
        }
        return bitFlagList;
    }

    public String getImageFeature(MultipartFile file) throws IOException {

        log.info("Uploading image");

        long beforeTime = System.currentTimeMillis();
        InputStream inputStream = file.getInputStream();

        // InputStream을 Mat으로 변환
        //Mat image = Imgcodecs.imdecode(new org.opencv.core.MatOfByte(inputStream.readAllBytes()), Imgcodecs.IMREAD_GRAYSCALE);

        BufferedImage img = ImageIO.read(inputStream);
        Mat image = bufferedImageToMat(img);

        // ORB 알고리즘으로 특징점 추출
        String bitFlagList = extractDescriptors(image);

        log.info("특징점 추출 끝. {}ms", System.currentTimeMillis() - beforeTime);

        return bitFlagList;
    }

    public static Mat bufferedImageToMat(BufferedImage bi) {
        // BufferedImage의 타입을 확인
        int imageType = bi.getType();
        Mat mat;

        if (imageType == BufferedImage.TYPE_BYTE_GRAY) {
            mat = new Mat(bi.getHeight(), bi.getWidth(), CvType.CV_8UC1); // 그레이스케일 이미지
        } else {
            mat = new Mat(bi.getHeight(), bi.getWidth(), CvType.CV_8UC3); // 컬러 이미지
        }

        byte[] data = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();
        mat.put(0, 0, data);  // 데이터 설정
        return mat;
    }


    // 상위 25개의 특징점을 16진수로 인코딩
    public String encodeFeaturesAsHex(List<Double> keySet) {
        // 256 비트를 표현하기 위한 32바이트 배열 생성
        byte[] bitSet = new byte[32];

        for (Double key : keySet) {
            int index = key.intValue(); // 특징점 값을 인덱스로 사용
            if (index < 256) { // 유효한 범위 내에서만 처리
                int byteIndex = index / 8; // 바이트 배열에서의 위치
                int bitIndex = index % 8;  // 바이트 내의 비트 위치
                bitSet[byteIndex] |= (1 << (7 - bitIndex)); // 해당 비트를 1로 설정
            }
        }

        // 바이트 배열을 16진수 문자열로 변환
        StringBuilder hexString = new StringBuilder();
        for (byte b : bitSet) {
            hexString.append(String.format("%02X", b));
        }

        return hexString.toString();
    }


    private String extractDescriptors(Mat image) throws IOException {
        if (image.empty()) {
            throw new IOException("Image could not be loaded or is empty.");
        }

        MatOfKeyPoint keyPoints = new MatOfKeyPoint();
        Mat descriptors = new Mat();

        try {
            // 특징점 검출 및 디스크립터 추출
            orb.detectAndCompute(image, new Mat(), keyPoints, descriptors);

            if (keyPoints.empty()) {
                log.info("No keypoints detected");
                return "0";
            }

            // 디스크립터 평균 계산
            double sum = 0;
            int count = 0;
            for (int i = 0; i < descriptors.rows(); i++) {
                double[] row = descriptors.get(i, 0); // 한 행의 디스크립터 가져오기
                for (double value : row) {

                        sum += value;
                        count++;

                }
            }
            double mean = sum / count;

            // 이진화된 디스크립터 저장
            StringBuilder sb = new StringBuilder();
            byte[] binaryDescriptors = new byte[descriptors.rows() * descriptors.cols()];
            int l = 0;
            for (int i = 0; i < descriptors.rows(); i++) {
                // 한 행의 디스크립터 가져오기
                double[] row = descriptors.get(i, 0);

                for (int j = 0; j < row.length; j++) {
                    // 이진화 처리
                    sb.append((row[j] < mean) ? 0 : 1);
                    //binaryDescriptors[l++] = (byte) ((row[j] < mean) ? 0 : 1);
                }
            }
            TreeSet<Integer> set1 = binaryToSet("0100111000110110000101110111000000101101110101001100011010000000111000101001100110000011111001010001010001010100000011000000001010100001110000111100000110100000000110110010101111011100110000000101001100011011101011101");
            TreeSet<Integer> set2 = binaryToSet("0000001100000100110001001000110010010010010110100001000001001001011100010111001100100000110101011111110100000111011001000000000101111000001000000010100001001011001100000101110000000110101110101010110101100000");

            // 전체 유니버스 크기를 동일하게 설정
            int universeSize = Math.max(set1.last() + 1, set2.last() + 1); // 가장 큰 인덱스 + 1

            // MinHash 초기화 (동일한 유니버스 크기 사용)

            int[] arr = {64, 128, 192, 256};
            for (int i : arr) {
                MinHash minHash = new MinHash(i, set1.size(), 123456);
                MinHash minHash2 = new MinHash(i, set2.size(), 123456);

                // MinHash 서명 생성
                int[] signature1 = minHash.signature(set1);
                int[] signature2 = minHash2.signature(set2);

                // 유사도 계산
                double similarity = minHash.similarity(signature1, signature2);

                System.out.println("Signature similarity: " + similarity);
            }


            return sb.toString();
        } finally {
            // 리소스 해제
            image.release();
            keyPoints.release();
            descriptors.release();
        }
    }

    private static TreeSet<Integer> binaryToSet(String binaryFeature) {
        TreeSet<Integer> set = new TreeSet<>();
        for (int i = 0; i < binaryFeature.length(); i++) {
            if (binaryFeature.charAt(i) == '1') {
                set.add(i); // 1의 위치를 추가
            }
        }
        return set;
    }

}