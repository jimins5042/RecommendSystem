package shop.RecommendSystem.dto;


import lombok.Builder;
import lombok.Getter;

@Getter

public enum ColorTag {

    // Enum 상수 정의
    BLACK(new int[]{0, 0, 0}),
    WHITE(new int[]{255, 255, 255}),
    GRAY(new int[]{127, 127, 127}),
    RED(new int[]{255, 0, 0}),
    ORANGE(new int[]{255, 127, 0}),
    YELLOW(new int[]{255, 255, 0}),
    LIME(new int[]{127, 255, 0}),
    GREEN(new int[]{0, 255, 0}),
    TURQUOISE(new int[]{0, 255, 127}),
    CYAN(new int[]{0, 255, 255}),
    OCEAN(new int[]{0, 127, 255}),
    BLUE(new int[]{0, 0, 255}),
    VIOLET(new int[]{127, 0, 255}),
    MAGENTA(new int[]{255, 0, 255}),
    RASPBERRY(new int[]{255, 0, 127});

    // 필드 선언
    private final int[] rgb;

    // 생성자
    ColorTag(int[] rgb) {
        this.rgb = rgb;
    }

    // Getter 메서드
    public int[] getRgb() {
        return rgb;
    }
}

/*
사용 예제

        // Enum 상수 출력
        for (ColorTag color : ColorTag.values()) {
            System.out.println(color.name() + " -> " + arrayToString(color.getRgb()));
        }

        //출력
        BLACK -> [0, 0, 0]
        WHITE -> [255, 255, 255]

        // 특정 컬러의 RGB 값 가져오기
        int[] redRgb = ColorTag.RED.getRgb();

 */