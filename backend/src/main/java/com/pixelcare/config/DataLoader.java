package com.pixelcare.config;

import com.pixelcare.domain.community.entity.Post;
import com.pixelcare.domain.community.entity.PostCategory;
import com.pixelcare.domain.community.repository.PostRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {

    private final PostRepository postRepository;

    public DataLoader(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (postRepository.count() == 0) {
            postRepository.save(new Post(
                    1L,
                    "해운대 픽셀용사",
                    "LV2_WARMTH",
                    PostCategory.REVIEW,
                    "🌊 해운대 플로깅 봉사 함께 다녀왔어요!",
                    "오늘 주말에 해운대 해변 플로깅 봉사를 신청해서 다녀왔습니다! 쓰레기 3kg이나 줍고 바다가 깨끗해져서 뿌듯하네요. 같이 봉사하신 용사님들 모두 수고 많으셨습니다!",
                    "https://images.unsplash.com/photo-1542601906990-b4d3fb778b09",
                    15,
                    3,
                    42
            ));

            postRepository.save(new Post(
                    2L,
                    "금정 온기 요정",
                    "LV3_FIRE",
                    PostCategory.REVIEW,
                    "🍲 금정구 어르신 도시락 배달 후기 및 꿀팁 공유",
                    "어르신들께 도시락 전달해드리면서 따뜻한 말씀 나누고 오니 마음까지 따뜻해지네요. 엘리베이터 없는 건물 계단 오르내릴 때 편한 운동화 필수입니다!",
                    "https://images.unsplash.com/photo-1488521787991-ed7bbaae773c",
                    23,
                    5,
                    68
            ));

            postRepository.save(new Post(
                    3L,
                    "동네 픽셀 기사",
                    "LV1_SEED",
                    PostCategory.RECRUIT,
                    "🐕 이번 주말 유기견 보육원 봉사 같이 가실 분 계신가요?",
                    "이번 주 토요일 오전 부산 북구 유기견 보육원 봉사 같이 카풀해서 가실 분 구합니다! 댓글 남겨주세요~",
                    null,
                    8,
                    2,
                    29
            ));
        }
    }
}
