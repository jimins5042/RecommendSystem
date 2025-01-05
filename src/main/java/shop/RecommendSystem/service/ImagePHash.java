package shop.RecommendSystem.service;

import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;

/*
    https://gist.github.com/kuFEAR/6e20342198d4040e0bb5 참고
 */
public class ImagePHash {

    private static final int SIZE = 64; // DCT를 위한 크기
    private static final int SMALL_SIZE = 16; // 해시값을 생성할 크기

    public String getPHash(String imgUrl) throws IOException {

        // 0. aws s3 링크를 BufferedImage로 변환
        URL url = new URL(imgUrl);
        BufferedImage img = ImageIO.read(url);

        return calPHash(img);
    }
    public String getPHash(MultipartFile imgFile) throws IOException {

        // 0. MultipartFile을 InputStream으로 받아 BufferedImage로 변환
        InputStream inputStream = imgFile.getInputStream();
        BufferedImage img = ImageIO.read(inputStream);

        return calPHash(img);
    }

    public String calPHash(BufferedImage img) throws IOException {

        // 0. aws s3 링크를 BufferedImage로 변환
        /*
        URL url = new URL(imgUrl);
        BufferedImage img = ImageIO.read(url);

         */

        // 1. 이미지를 흑백으로 변환하고 크기를 SIZE x SIZE로 조정
        BufferedImage resizedImage = resizeAndGrayscale(img, SIZE, SIZE);

        // 2. 이미지의 픽셀값을 DCT 변환
        double[][] dctValues = applyDCT(resizedImage);

        // 3. 상위 왼쪽 SMALL_SIZE x SMALL_SIZE 부분 추출
        double[] topLeftValues = getTopLeft(dctValues, SMALL_SIZE);

        // 4. 중간값 계산
        double median = calculateMedian(topLeftValues);

        // 5. 평균값을 기준으로 0 또는 1로 변환하여 해시 생성
        return generateHash(topLeftValues, median);
    }

    private static BufferedImage resizeAndGrayscale(BufferedImage img, int width, int height) {
        Image scaledImage = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        BufferedImage grayImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2d = grayImage.createGraphics();
        g2d.drawImage(scaledImage, 0, 0, null);
        g2d.dispose();
        return grayImage;
    }

    private static double[][] applyDCT(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();
        double[][] values = new double[width][height];

        // 픽셀값 추출
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                values[x][y] = img.getRaster().getSample(x, y, 0);
            }
        }
        // DCT 변환
        return performDCT(values);
    }

    private static double[][] performDCT(double[][] matrix) {
        int n = matrix.length;
        double[][] dct = new double[n][n];

        for (int u = 0; u < n; u++) {
            for (int v = 0; v < n; v++) {
                double sum = 0.0;
                for (int x = 0; x < n; x++) {
                    for (int y = 0; y < n; y++) {
                        sum += matrix[x][y] *
                                Math.cos(((2 * x + 1) * u * Math.PI) / (2.0 * n)) *
                                Math.cos(((2 * y + 1) * v * Math.PI) / (2.0 * n));
                    }
                }
                double cu = (u == 0) ? Math.sqrt(1.0 / n) : Math.sqrt(2.0 / n);
                double cv = (v == 0) ? Math.sqrt(1.0 / n) : Math.sqrt(2.0 / n);
                dct[u][v] = cu * cv * sum;
            }
        }

        return dct;
    }

    private static double[] getTopLeft(double[][] dctValues, int size) {
        double[] topLeft = new double[size * size];
        int l = 0;

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {

                topLeft[l++] = dctValues[j][i];
            }
        }
        return topLeft;
    }

    private static double calculateMedian(double[] values) {
        int totalElements = values.length;

        double[] flattened = new double[totalElements];

        // 배열 정렬
        Arrays.sort(flattened);

        // 중간값 계산
        if (totalElements % 2 == 1) {
            // 요소 개수가 홀수일 경우 중앙 값 반환
            return flattened[totalElements / 2];
        } else {
            // 요소 개수가 짝수일 경우 중앙 두 값의 평균 반환
            return (flattened[(totalElements / 2) - 1] + flattened[totalElements / 2]) / 2.0;
        }
    }

    private static String generateHash(double[] values, double average) {
        StringBuilder hexHash = new StringBuilder();
        int size = values.length;
        int bitCount = 0;
        int hexValue = 0;

        for (int i = 0; i < size; i++) {

            // 현재 비트를 추가
            hexValue = (hexValue << 1) | (values[i] > average ? 1 : 0);
            bitCount++;

            // 4비트가 쌓이면 16진수 문자로 변환
            if (bitCount == 4) {
                hexHash.append(Integer.toHexString(hexValue));
                hexValue = 0; // 초기화
                bitCount = 0; // 초기화
            }
        }
        // 남은 비트 처리 (4비트 미만인 경우)
        if (bitCount > 0) {
            hexValue = hexValue << (4 - bitCount); // 남은 비트를 왼쪽으로 밀기
            hexHash.append(Integer.toHexString(hexValue));
        }
        return hexHash.toString();
    }




}
