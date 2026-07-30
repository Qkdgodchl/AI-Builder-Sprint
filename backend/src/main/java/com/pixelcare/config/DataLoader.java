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
            // 1. 봉사 (VOLUNTEER)
            volunteerRepository.save(new Volunteer(
                    null, "🐕 부산 북구 유기견 보육원 주말 봉사", "VOLUNTEER",
                    "부산 북구 동물보호센터", "부산 동네 온기 봉사단",
                    null, 0L, List.of("공식 인증", "4시간 인정", "주말봉사"), null
            ));
            volunteerRepository.save(new Volunteer(
                    null, "🌊 해운대 해변 픽셀 플로깅 정화 활동", "VOLUNTEER",
                    "부산 해운대 구남로 광장", "그린 픽셀 에코 클럽",
                    null, 0L, List.of("공식 인증", "3시간 인정", "환경정화"), null
            ));
            volunteerRepository.save(new Volunteer(
                    null, "🍲 금정구 독거어르신 온기 도시락 배달", "VOLUNTEER",
                    "부산 금정구 종합복지관", "사랑의 픽셀 이웃",
                    null, 0L, List.of("공식 인증", "4시간 인정", "어르신복지"), null
            ));
            volunteerRepository.save(new Volunteer(
                    null, "📚 사상구 꿈나무 차상위 아동 학습 지도", "VOLUNTEER",
                    "부산 사상구 지역아동센터", "픽셀 에듀 봉사단",
                    null, 0L, List.of("공식 인증", "2시간 인정", "학습지혜"), null
            ));
            volunteerRepository.save(new Volunteer(
                    null, "👵 남구 노인복지관 주말 말벗 및 장기 도우미", "VOLUNTEER",
                    "부산 남구 노인복지관", "부산 따뜻한 온기 동행",
                    null, 0L, List.of("공식 인증", "3시간 인정", "말벗정서"), null
            ));
            volunteerRepository.save(new Volunteer(
                    null, "🌱 동래구 수영강 환경 생태 보존 시민 봉사", "VOLUNTEER",
                    "부산 동래구 수영강 변 산책로", "수영강 생태 지킴이",
                    null, 0L, List.of("공식 인증", "4시간 인정", "생태보존"), null
            ));

            // 2. 일반기부 (GENERAL)
            volunteerRepository.save(new Volunteer(
                    null, "❤️ 저소득층 희귀질환 환아 치료비 정기후원", "GENERAL",
                    "전국", "픽셀케어 파트너 재단",
                    10000000L, 4250000L, List.of("정기후원", "의료지원", "환아돕기"), null
            ));
            volunteerRepository.save(new Volunteer(
                    null, "🍲 결식아동 주말 온기 밥상 지원 펀딩", "GENERAL",
                    "부산광역시 전역", "사랑의 나눔 기금",
                    5000000L, 3180000L, List.of("결식아동", "온기밥상", "급식지원"), null
            ));
            volunteerRepository.save(new Volunteer(
                    null, "🏠 주거 취약계층 온기 방한용품 나눔 후원", "GENERAL",
                    "부산 동구 초량동", "동구 희망나눔 센터",
                    3000000L, 2100000L, List.of("방한용품", "주거복지", "겨울나기"), null
            ));

            // 3. 유산기부 (LEGACY)
            volunteerRepository.save(new Volunteer(
                    null, "📜 나눔을 이어가는 미래세대 유산기부 전문상담", "LEGACY",
                    "전국", "유산기부 전문 상담센터",
                    null, 0L, List.of("전문상담", "약정", "미래유산"), null
            ));
            volunteerRepository.save(new Volunteer(
                    null, "🏛️ 세대를 넘어 전달되는 아동복지 유산 약정", "LEGACY",
                    "전국", "한국 아동복지 유산재단",
                    null, 0L, List.of("아동복지", "유산약정", "사회적가치"), null
            ));

            // 4. 유네스코 후원 (UNESCO)
            volunteerRepository.save(new Volunteer(
                    null, "🌏 기후위기 대응 긴급 구호 유네스코 펀딩", "UNESCO",
                    "전 세계", "세계유산 보존 파트너",
                    20000000L, 15400000L, List.of("세계유산", "기후위기", "긴급구호"), null
            ));
            volunteerRepository.save(new Volunteer(
                    null, "📚 분쟁지역 어린이 평화 교육 유네스코 사업", "UNESCO",
                    "전 세계", "유네스코 평화 교육위원회",
                    15000000L, 8900000L, List.of("평화교육", "유네스코", "어린이구호"), null
            ));

            // 5. 문화유산 후원 (HERITAGE)
            volunteerRepository.save(new Volunteer(
                    null, "🏛️ 부산 범어사 목조 아미타여래좌상 보존 후원", "HERITAGE",
                    "부산 금정구 범어사", "지역 문화유산 센터",
                    8000000L, 5200000L, List.of("문화유산", "문화재복원", "부산역사"), null
            ));
            volunteerRepository.save(new Volunteer(
                    null, "🏯 영도 동삼동 패총 유적지 보존 및 교육 후원", "HERITAGE",
                    "부산 영도구 동삼동", "부산 선사문화 보존회",
                    5000000L, 3400000L, List.of("동삼동패총", "선사유적", "유적지보존"), null
            ));

            // 6. 고향사랑기부제 (HOMETOWN)
            volunteerRepository.save(new Volunteer(
                    null, "🌾 부산광역시 고향사랑기부제 (지역 상생 답례품)", "HOMETOWN",
                    "부산광역시", "지역 상생 기부 안내센터",
                    null, 0L, List.of("고향사랑기부", "지역상생", "세액공제"), null
            ));
            volunteerRepository.save(new Volunteer(
                    null, "⚓ 부산 영도구 고향사랑기부 (해양 환경 상생 기금)", "HOMETOWN",
                    "부산 영도구", "영도구 고향사랑 기부팀",
                    null, 0L, List.of("영도사랑", "해양상생", "지역발전"), null
            ));
        }

        if (postRepository.count() == 0) {
            postRepository.save(new Post(
                    "🌊 해운대 플로깅 봉사 함께 다녀왔어요!",
                    "오늘 주말에 해운대 해변 플로깅 봉사를 신청해서 다녀왔습니다! 쓰레기 3kg이나 줍고 바다가 깨끗해져서 뿌듯하네요.",
                    "해운대 픽셀용사", "LV2_WARMTH", "REVIEW", null
            ));
            postRepository.save(new Post(
                    "🍲 금정구 어르신 도시락 배달 후기 및 꿀팁 공유",
                    "어르신들께 도시락 전달해드리면서 따뜻한 말씀 나누고 오니 마음까지 따뜻해지네요. 엘리베이터 없는 건물 계단 오르내릴 때 편한 운동화 필수입니다!",
                    "금정 온기 요정", "LV3_FIRE", "REVIEW", null
            ));
            postRepository.save(new Post(
                    "🐕 이번 주말 유기견 보육원 봉사 같이 가실 분 계신가요?",
                    "이번 주 토요일 오전 부산 북구 유기견 보육원 봉사 같이 카풀해서 가실 분 구합니다! 댓글 남겨주세요~",
                    "동네 픽셀 기사", "LV1_SEED", "RECRUIT", null
            ));
        }
    }
}
