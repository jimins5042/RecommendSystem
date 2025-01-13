package shop.RecommendSystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import nu.pattern.OpenCV;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.ORB;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import shop.RecommendSystem.dto.ImageFeaturesResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Controller
public class ImageController {

    static {
        // OpenCV 라이브러리 로드
        //System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        OpenCV.loadLocally();
    }

    // 특징점과 설명자 반환하는 메서드
    @PostMapping("/testORB")
    @ResponseBody
    public ResponseEntity<Object> uploadImage(@RequestParam("image") MultipartFile file,
                                              @RequestParam("rgb") String palette) throws IOException {

        log.info("Uploading image");

        if (file.isEmpty()) {
            log.info("File is empty");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("파일이 비어 있습니다.");
        }
        long beforeTime = System.currentTimeMillis();
        // MultipartFile을 InputStream으로 변환
        InputStream inputStream = file.getInputStream();

        // InputStream을 Mat으로 변환
        Mat image = Imgcodecs.imdecode(new org.opencv.core.MatOfByte(inputStream.readAllBytes()), Imgcodecs.IMREAD_GRAYSCALE);

        if (image.empty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("이미지를 읽을 수 없습니다.");
        }

        // ORB 알고리즘으로 특징점 추출
        List<List<Float>> descriptors = extractDescriptors(image);
        System.out.println("Extracted descriptors: " + descriptors);  // 디버깅용
        // 특징점과 설명자 정보를 JSON 형식으로 반환
        ImageFeaturesResponse response = new ImageFeaturesResponse(descriptors);

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            // JSON 형식으로 출력
            String jsonResponse = objectMapper.writeValueAsString(response);
            log.info("jsonResponse \n{}", jsonResponse);
        } catch (Exception e) {
            e.printStackTrace();
        }
        log.info(" 특징점 추출 끝. {} ", System.currentTimeMillis() - beforeTime);

        return ResponseEntity.ok(response);
    }

    // 설명자 추출
    private List<List<Float>> extractDescriptors(Mat image) {
        ORB orb = ORB.create();
        MatOfKeyPoint keyPoints = new MatOfKeyPoint();

        // 특징점 검출
        orb.detect(image, keyPoints);

        if (keyPoints.empty()) {
            log.info("No keypoints detected");
            return new ArrayList<>();
        }

        // 설명자 추출
        Mat descriptors = new Mat();
        orb.compute(image, keyPoints, descriptors);

        List<List<Float>> descriptorList = new ArrayList<>();
        for (int i = 0; i < descriptors.rows(); i++) {
            List<Float> descriptor = new ArrayList<>();
            for (int j = 0; j < descriptors.cols(); j++) {
                descriptor.add((float) descriptors.get(i, j)[0]);
            }

            descriptorList.add(descriptor);
        }
        return descriptorList;
    }
}