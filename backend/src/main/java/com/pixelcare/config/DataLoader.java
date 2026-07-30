package com.pixelcare.config;

import com.pixelcare.domain.community.entity.Post;
import com.pixelcare.domain.community.repository.PostRepository;
import com.pixelcare.volunteer.Volunteer;
import com.pixelcare.volunteer.VolunteerRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataLoader implements CommandLineRunner {

    private final VolunteerRepository volunteerRepository;
    private final PostRepository postRepository;

    public DataLoader(VolunteerRepository volunteerRepository, PostRepository postRepository) {
        this.volunteerRepository = volunteerRepository;
        this.postRepository = postRepository;
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
                    List.of("4시간 인정", "주말"),
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
                    List.of("3시간 인정", "환경정화"),
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
                    List.of("4시간 인정", "복지"),
                    null
            ));

            volunteerRepository.save(new Volunteer(
                    null,
                    "지역 아동 교육 정기 후원",
                    "GENERAL",
                    "전국",
                    "픽셀케어 파트너 재단",
                    null,
                    0L,
                    List.of("정기후원", "아동·청소년"),
                    null
            ));

            volunteerRepository.save(new Volunteer(
                    null,
                    "나눔을 이어가는 유산기부 상담",
                    "LEGACY",
                    "전국",
                    "유산기부 전문 상담센터",
                    null,
                    0L,
                    List.of("전문상담", "약정"),
                    null
            ));

            volunteerRepository.save(new Volunteer(
                    null,
                    "세계유산 보존 정기후원",
                    "UNESCO",
                    "전 세계",
                    "세계유산 보존 파트너",
                    null,
                    0L,
                    List.of("세계유산", "보존사업"),
                    null
            ));

            volunteerRepository.save(new Volunteer(
                    null,
                    "우리 문화유산 지킴이 후원",
                    "HERITAGE",
                    "전국",
                    "지역 문화유산 센터",
                    null,
                    0L,
                    List.of("문화유산", "복원"),
                    null
            ));

            volunteerRepository.save(new Volunteer(
                    null,
                    "부산 고향사랑기부제",
                    "HOMETOWN",
                    "부산광역시",
                    "지역 상생 기부 안내센터",
                    null,
                    0L,
                    List.of("고향사랑기부", "지역상생"),
                    null
            ));
        }

        if (postRepository.count() == 0) {
            postRepository.save(new Post(
                    "🌊 해운대 플로깅 봉사 함께 다녀왔어요!",
                    "오늘 주말에 해운대 해변 플로깅 봉사를 신청해서 다녀왔습니다! 쓰레기 3kg이나 줍고 바다가 깨끗해져서 뿌듯하네요. 같이 봉사하신 용사님들 모두 수고 많으셨습니다!",
                    "해운대 픽셀용사",
                    "LV2_WARMTH",
                    "REVIEW",
                    null
            ));

            postRepository.save(new Post(
                    "🍲 금정구 어르신 도시락 배달 후기 및 꿀팁 공유",
                    "어르신들께 도시락 전달해드리면서 따뜻한 말씀 나누고 오니 마음까지 따뜻해지네요. 엘리베이터 없는 건물 계단 오르내릴 때 편한 운동화 필수입니다!",
                    "금정 온기 요정",
                    "LV3_FIRE",
                    "REVIEW",
                    null
            ));

            postRepository.save(new Post(
                    "🐕 이번 주말 유기견 보육원 봉사 같이 가실 분 계신가요?",
                    "이번 주 토요일 오전 부산 북구 유기견 보육원 봉사 같이 카풀해서 가실 분 구합니다! 댓글 남겨주세요~",
                    "동네 픽셀 기사",
                    "LV1_SEED",
                    "RECRUIT",
                    null
            ));
        }
    }
}
