package shop.RecommendSystem.recommend.ImageFeature;

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

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutdown Hook: Cleaning OpenCV resources");
            System.gc(); // 가비지 컬렉션 강제 실행
        }));
    }

    private final HashFunction rowHashFunction = Hashing.murmur3_32(); // 행 해싱용
    private final HashFunction finalHashFunction = Hashing.sha256(); // 최종 해싱용
    private final ORB orb = ORB.create(256, 1.2f, 8);


    public List<Double> getImageFeature(String imgUrl) throws IOException {
        List<Double> bitFlagList = new ArrayList<>();
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

    public List<Double> getImageFeature(MultipartFile file) throws IOException {
    //public String getImageFeature(MultipartFile file) throws IOException {

        log.info("Uploading image");

        long beforeTime = System.currentTimeMillis();
        InputStream inputStream = file.getInputStream();

        // InputStream을 Mat으로 변환
        //Mat image = Imgcodecs.imdecode(new org.opencv.core.MatOfByte(inputStream.readAllBytes()), Imgcodecs.IMREAD_GRAYSCALE);

        BufferedImage img = ImageIO.read(inputStream);
        Mat image = bufferedImageToMat(img);

        // ORB 알고리즘으로 특징점 추출
        //String bitFlagList = extractDescriptors(image);
        List<Double> bitFlagList= extractDescriptors(image);

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

    private String extractDescriptorsv2(Mat image) throws IOException {
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
                    if (value != 0) {
                        sum += value;
                        count++;
                    }
                }
            }
            double mean = sum / count;

            // 이진화된 디스크립터 저장
            byte[][] binaryDescriptors = new byte[descriptors.rows()][descriptors.cols()];

            for (int i = 0; i < descriptors.rows(); i++) {
                // 한 행의 디스크립터 가져오기
                double[] row = descriptors.get(i, 0);

                for (int j = 0; j < row.length; j++) {
                    // 이진화 처리
                    binaryDescriptors[i][j] = (byte) ((row[j] < mean) ? 0 : 1);
                }
            }
            // 각 행에 대해 해시 계산 후 결합
            StringBuilder intermediateHashes = new StringBuilder();

            for (byte[] row : binaryDescriptors) {
                // 바이트 배열을 문자열로 변환하여 해싱
                String rowHash = rowHashFunction.hashBytes(row).toString();
                intermediateHashes.append(rowHash); // 중간 해시 값 결합
            }

            // 결합된 해시를 최종적으로 SHA-256으로 해싱
            return finalHashFunction.hashString(intermediateHashes.toString(), java.nio.charset.StandardCharsets.UTF_8).toString();

        } finally {
            // 리소스 해제
            image.release();
            keyPoints.release();
            descriptors.release();
        }
    }

    private List<Double> extractDescriptors(Mat image) throws IOException {
        //ORB orb = ORB.create();
        MatOfKeyPoint keyPoints = new MatOfKeyPoint();
        HashMap<Double, Long> descriptorsMap = new HashMap<>();
        if (image.empty()) {
            throw new IOException("Image could not be loaded or is empty.");
        }

        try {
            // 특징점 검출
            orb.detect(image, keyPoints);

            if (keyPoints.empty()) {
                log.info("No keypoints detected");
            } else {
                // 설명자 추출
                Mat descriptors = new Mat();
                HashMap<Double, Long> descriptorsFrequencyMap = new HashMap<>();
                try {
                    orb.compute(image, keyPoints, descriptors);

                    for (int i = 0; i < descriptors.rows(); i++) {
                        for (int j = 0; j < descriptors.cols(); j++) {

                            for (Double d : descriptors.get(i, j)) {

                                descriptorsFrequencyMap.put(d, descriptorsMap.getOrDefault(d, 0L) + 1);
                            }

                            List<Double> keySet = new ArrayList<>(descriptorsFrequencyMap.keySet());
                            keySet.sort((o1, o2) -> descriptorsMap.get(o2).compareTo(descriptorsMap.get(o1)));

                            descriptorsMap.put(keySet.get(0), descriptorsMap.getOrDefault(keySet.get(0), 0L) + 1);
                            descriptorsFrequencyMap.clear();
                        }
                    }
                } finally {
                    descriptors.release(); // Mat 객체 해제
                }
            }
        } finally {
            image.release(); // Mat 객체 해제
            keyPoints.release(); // MatOfKeyPoint 해제
        }

        // 특징점의 갯수 순으로 정렬 및 상위 25개 추출
        List<Double> keySet = new ArrayList<>(descriptorsMap.keySet());
        keySet.sort((o1, o2) -> descriptorsMap.get(o2).compareTo(descriptorsMap.get(o1)));
        return keySet.subList(0, 25);

    }


    // 16진수 문자열을 2진수 문자열로 변환
    public static void printActiveBits(String hexString) {

        String binaryString = hexToBinary(hexString);

        System.out.println("활성화된 비트의 인덱스:");

        // 2진수 문자열을 순회하여 활성화된 비트(1인 비트)의 인덱스를 출력
        for (int i = 0; i < binaryString.length(); i++) {
            if (binaryString.charAt(i) == '1') {
                System.out.println("비트 " + i + "이(가) 활성화되었습니다.");
            }
        }
    }

    // 16진수 문자열을 2진수 문자열로 변환하는 메서드
    public static String hexToBinary(String hexString) {
        StringBuilder binaryString = new StringBuilder();

        // 16진수 문자열의 각 문자를 4비트 이진수로 변환
        for (int i = 0; i < hexString.length(); i++) {
            String binary = String.format("%4s", Integer.toBinaryString(Integer.parseInt(String.valueOf(hexString.charAt(i)), 16)))
                    .replace(' ', '0'); // 4자리 이진수로 맞추기
            binaryString.append(binary);
        }

        return binaryString.toString();

    }
}