package shop.RecommendSystem.service.logic;

import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.ORB;
import org.opencv.features2d.SIFT;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public class Surf {

    public String uploadImage(MultipartFile file) {
        // 이미지 파일 경로
        String imagePath = "path_to_your_image.jpg";

        // 이미지 읽기
        Mat image = Imgcodecs.imread(imagePath, Imgcodecs.IMREAD_GRAYSCALE);

        // ORB 객체 생성
        ORB orb = ORB.create();

        // 특징점과 설명자 리스트
        MatOfKeyPoint keypoints = new MatOfKeyPoint();
        Mat descriptors = new Mat();

        // ORB 특징점 검출 및 설명자 계산
        orb.detectAndCompute(image, new Mat(), keypoints, descriptors);

        // 특징점 그리기
        Mat outputImage = new Mat();
        Features2d.drawKeypoints(image, keypoints, outputImage);

        // 결과 이미지 저장
        Imgcodecs.imwrite("output_with_keypoints.jpg", outputImage);

        // 특징점 정보 출력
        List<KeyPoint> keypointList = keypoints.toList();
        System.out.println("검출된 특징점 개수: " + keypointList.size());
        return "";
    }
}


