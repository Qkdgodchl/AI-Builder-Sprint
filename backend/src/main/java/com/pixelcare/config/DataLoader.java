package com.pixelcare.config;

import com.pixelcare.volunteer.Volunteer;
import com.pixelcare.volunteer.VolunteerRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataLoader implements CommandLineRunner {

    private final VolunteerRepository volunteerRepository;

    public DataLoader(VolunteerRepository volunteerRepository) {
        this.volunteerRepository = volunteerRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (volunteerRepository.count() == 0) {
            volunteerRepository.save(new Volunteer(
                    null,
                    "🐕 부산 북구 유기견 보육원 주말 봉사",
                    "VOLUNTEER",
                    "부산 북구 동물보호센터",
                    "부산 동네 온기 봉사단",
                    null,
                    0L,
                    List.of("1365 연동", "4시간 인정", "주말"),
                    "https://www.1365.go.kr/vols/1365/act/volsList.do?searchKeyword=유기견",
                    null
            ));

            volunteerRepository.save(new Volunteer(
                    null,
                    "🌊 해운대 해변 픽셀 플로깅 정화 활동",
                    "VOLUNTEER",
                    "부산 해운대 구남로 광장",
                    "그린 픽셀 에코 클럽",
                    null,
                    0L,
                    List.of("1365 연동", "3시간 인정", "환경정화"),
                    "https://www.1365.go.kr/vols/1365/act/volsList.do?searchKeyword=플로깅",
                    null
            ));

            volunteerRepository.save(new Volunteer(
                    null,
                    "🍲 금정구 독거어르신 온기 도시락 배달",
                    "VOLUNTEER",
                    "부산 금정구 종합복지관",
                    "사랑의 픽셀 이웃",
                    null,
                    0L,
                    List.of("1365 연동", "4시간 인정", "복지"),
                    "https://www.1365.go.kr/vols/1365/act/volsList.do?searchKeyword=도시락",
                    null
            ));
        }
    }
}
