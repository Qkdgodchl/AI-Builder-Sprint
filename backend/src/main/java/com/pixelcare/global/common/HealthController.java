package com.pixelcare.global.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 배포 환경 헬스체크 전용.
 * 통계 API를 헬스체크로 쓰면 집계 쿼리가 밀리는 순간 타임아웃(5초)으로
 * 멀쩡한 인스턴스가 고장 판정을 받고 재시작된다. 이 응답은 DB도 외부 API도
 * 타지 않아 부하 중에도 즉시 답한다.
 */
@RestController
public class HealthController {

    @GetMapping("/healthz")
    public Map<String, String> healthz() {
        return Map.of("status", "ok");
    }
}
