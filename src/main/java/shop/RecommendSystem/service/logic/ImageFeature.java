package shop.RecommendSystem.service.logic;

import lombok.extern.slf4j.Slf4j;
import nu.pattern.OpenCV;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.ORB;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
@Slf4j
public class ImageFeature {
    static {
        // OpenCV 라이브러리 로드
        //System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        OpenCV.loadLocally();
    }

    public String getImageFeature(MultipartFile file) throws IOException {

        log.info("Uploading image");

        long beforeTime = System.currentTimeMillis();

        // ORB 알고리즘으로 특징점 추출
        HashMap<Double, Long> descriptors = extractDescriptors(file);

        // 특징점의 갯수 순으로 정렬 및 상위 25개 추출
        ArrayList<Double> keySet = new ArrayList<>(descriptors.keySet());
        keySet.sort((o1, o2) -> descriptors.get(o2).compareTo(descriptors.get(o1)));


        // 상위 25개의 특징점을 16진수 문자열로 저장
        String hexBitFlag = encodeFeaturesAsHex(keySet, 25);

        log.info("Hexadecimal Bit Flag: {}", hexBitFlag);
        log.info("특징점 추출 끝. {}ms", System.currentTimeMillis() - beforeTime);

        printActiveBits(hexBitFlag);

        return hexBitFlag;
    }


    // 상위 N개의 특징점을 16진수로 인코딩
    private String encodeFeaturesAsHex(List<Double> keySet, int topN) {
        // 256 비트를 표현하기 위한 32바이트 배열 생성
        byte[] bitSet = new byte[32];

        int count = 0;
        for (Double key : keySet) {
            if (count >= topN) break;
            int index = key.intValue(); // 특징점 값을 인덱스로 사용
            if (index < 256) { // 유효한 범위 내에서만 처리
                int byteIndex = index / 8; // 바이트 배열에서의 위치
                int bitIndex = index % 8;  // 바이트 내의 비트 위치
                bitSet[byteIndex] |= (1 << (7 - bitIndex)); // 해당 비트를 1로 설정
            }
            count++;
        }

        // 바이트 배열을 16진수 문자열로 변환
        StringBuilder hexString = new StringBuilder();
        for (byte b : bitSet) {
            hexString.append(String.format("%02X", b));
        }

        return hexString.toString();
    }

    // 특징점 추출 메서드
    private HashMap<Double, Long> extractDescriptors(MultipartFile file) throws IOException {
        InputStream inputStream = file.getInputStream();

        // InputStream을 Mat으로 변환
        Mat image = Imgcodecs.imdecode(new org.opencv.core.MatOfByte(inputStream.readAllBytes()), Imgcodecs.IMREAD_GRAYSCALE);

        ORB orb = ORB.create();
        MatOfKeyPoint keyPoints = new MatOfKeyPoint();
        HashMap<Double, Long> descriptorsMap = new HashMap<>();

        try {
            // 특징점 검출
            orb.detect(image, keyPoints);

            if (keyPoints.empty()) {
                log.info("No keypoints detected");
            } else {
                // 설명자 추출
                Mat descriptors = new Mat();
                try {
                    orb.compute(image, keyPoints, descriptors);

                    for (int i = 0; i < descriptors.rows(); i++) {
                        for (int j = 0; j < descriptors.cols(); j++) {
                            double value = descriptors.get(i, j)[0];
                            descriptorsMap.put(value, descriptorsMap.getOrDefault(value, 0L) + 1);
                        }
                    }
                } finally {
                    descriptors.release(); // Mat 객체 해제
                }
            }
        } finally {
            keyPoints.release(); // MatOfKeyPoint 해제
        }

        return descriptorsMap;
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
