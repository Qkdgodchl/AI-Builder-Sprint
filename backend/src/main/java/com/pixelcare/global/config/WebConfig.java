package com.pixelcare.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 프론트가 붙는 출처를 배포 환경마다 환경변수로 지정한다.
 * 같은 "/api/**" 매핑을 두 곳에서 등록하면 나중에 등록된 쪽이 앞선 설정을 덮어써
 * 어느 쪽이 적용될지 빈 등록 순서에 좌우된다. 그래서 CORS는 이 클래스에서만 다룬다.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;

    public WebConfig(@Value("${app.cors.allowed-origins}") String[] allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                // 패턴이라 https://*.vercel.app 같은 미리보기 주소까지 함께 받는다.
                .allowedOriginPatterns(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
