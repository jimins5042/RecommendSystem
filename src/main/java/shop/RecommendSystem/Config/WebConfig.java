package shop.RecommendSystem.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    //실제 서버 파일 경로
    @Value("${resourcePath}")
    private String resourcePath;

    //웹에서 접근할 URL 패턴
    @Value("${connectPath}")
    private String connectPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(connectPath + "/**").addResourceLocations("file:///" + resourcePath);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 모든 경로에 대해 CORS 설정
                .allowedOrigins("http://localhost:8080") // 허용할 도메인
                .allowedMethods("GET", "POST", "PUT", "DELETE", "HEAD") // 허용할 HTTP 메서드
                .allowedHeaders("*") // 허용할 헤더
                .allowCredentials(true); // 인증 정보(쿠키, 인증 헤더)를 포함할지 여부
    }


}
