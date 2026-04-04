package shop.RecommendSystem;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@MapperScan("shop.RecommendSystem.repository.mapper")
@EnableFeignClients
@SpringBootApplication
public class RecommendSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(RecommendSystemApplication.class, args);

    }

}
