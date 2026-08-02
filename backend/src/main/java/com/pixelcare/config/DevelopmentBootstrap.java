package com.pixelcare.config;

import com.pixelcare.global.common.KeyExtractUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.bootstrap.enabled", havingValue = "true")
public class DevelopmentBootstrap implements CommandLineRunner {

        private final JdbcTemplate jdbcTemplate;
        private final String operatorPassword;
        private final String managerPassword;
        private final String donorPassword;
        private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        public DevelopmentBootstrap(
                        JdbcTemplate jdbcTemplate,
                        @Value("${app.bootstrap.operator-password}") String operatorPassword,
                        @Value("${app.bootstrap.manager-password}") String managerPassword,
                        @Value("${app.bootstrap.donor-password}") String donorPassword) {
                this.jdbcTemplate = jdbcTemplate;
                this.operatorPassword = operatorPassword;
                this.managerPassword = managerPassword;
                this.donorPassword = donorPassword;
        }

        @Override
        @Transactional
        public void run(String... args) {
                ensureSchemaTablesExist();
                Long operatorId = ensureUser(
                                "operator@pixelcare.local",
                                "잇다 운영진",
                                "운영진",
                                operatorPassword,
                                "OPERATOR");
                Long managerId = ensureUser(
                                "manager@pixelcare.demo",
                                "데모 센터 관리자",
                                "데모 관리자",
                                managerPassword,
                                "CENTER_MANAGER");
                Long donorId = ensureUser(
                                "donor@pixelcare.demo",
                                "정기후원 데모 후원자",
                                "데모 후원자",
                                donorPassword,
                                "USER");
                rebrandLegacyNames();
                Long organizationId = ensureDemoOrganization(managerId);
                seedOpportunities(managerId, organizationId);
                seedExtraOpportunities(managerId, organizationId);
                ensureVolunteerRequiredDocuments();
                refreshPlaceholderDescriptions();
                alignDemoFundingTargets();
                seedRecurringCommitments(donorId, organizationId);
                seedCommunityActivity(organizationId);
                backfillParticipationDates();
        }

        private record OpportunitySeed(
                        String type,
                        String category,
                        String title,
                        String organizer,
                        String region,
                        String location,
                        Integer capacity,
                        Long targetAmount,
                        Long currentAmount) {
        }

        /** 카탈로그가 비어 보이지 않도록 지역·유형을 넓힌 추가 공고. 부산 비중을 크게 둔다. */
        private static final List<OpportunitySeed> EXTRA_OPPORTUNITIES = List.of(
                        new OpportunitySeed("VOLUNTEER", "VOLUNTEER", "🏖️ 송정해수욕장 새벽 해변 정화 봉사",
                                        "해운대 블루오션 봉사단", "BUSAN", "부산 해운대구 송정해수욕장", 40, null, null),
                        new OpportunitySeed("VOLUNTEER", "VOLUNTEER", "📖 부산진구 다문화가정 한글 교실 보조",
                                        "부산진구 다문화지원센터", "BUSAN", "부산 부산진구 가족센터", 20, null, null),
                        new OpportunitySeed("VOLUNTEER", "VOLUNTEER", "🧺 사하구 독거어르신 빨래방 나눔 봉사",
                                        "사하 온기나눔 협의회", "BUSAN", "부산 사하구 감천문화마을", 25, null, null),
                        new OpportunitySeed("VOLUNTEER", "VOLUNTEER", "🚲 수영구 자전거 안전 캠페인 도우미",
                                        "수영구 교통안전 시민모임", "BUSAN", "부산 수영구 광안리 해변로", 30, null, null),
                        new OpportunitySeed("VOLUNTEER", "VOLUNTEER", "🐟 기장군 어촌마을 그물 손질 일손 돕기",
                                        "기장 바다살림 봉사회", "BUSAN", "부산 기장군 대변항", 20, null, null),
                        new OpportunitySeed("VOLUNTEER", "VOLUNTEER", "🎨 중구 원도심 벽화 보수 재능기부",
                                        "부산 원도심 문화지킴이", "BUSAN", "부산 중구 40계단 일원", 25, null, null),
                        new OpportunitySeed("VOLUNTEER", "VOLUNTEER", "🍞 연제구 푸드뱅크 식품 분류 봉사",
                                        "연제구 푸드뱅크", "BUSAN", "부산 연제구 푸드뱅크 물류센터", 30, null, null),
                        new OpportunitySeed("VOLUNTEER", "VOLUNTEER", "🐕 인천 계양구 유기견 산책 도우미",
                                        "인천 반려동물 보호연대", "NATIONWIDE", "인천 계양구 동물보호센터", 20, null, null),
                        new OpportunitySeed("VOLUNTEER", "VOLUNTEER", "🌾 전주 한옥마을 관광 안내 통역 봉사",
                                        "전주 문화관광 서포터즈", "NATIONWIDE", "전북 전주시 한옥마을", 25, null, null),
                        new OpportunitySeed("VOLUNTEER", "VOLUNTEER", "♻️ 대전 유성구 제로웨이스트 캠페인",
                                        "대전 초록실천단", "NATIONWIDE", "대전 유성구 봉명동", 30, null, null),
                        new OpportunitySeed("DONATION", "GENERAL", "🎒 취약계층 아동 신학기 학용품 지원",
                                        "부산 아동복지 연대", "BUSAN", "부산광역시 전역", null, 4_000_000L, 1_850_000L),
                        new OpportunitySeed("DONATION", "GENERAL", "❄️ 쪽방촌 이웃 겨울 난방유 지원 모금",
                                        "부산 겨울나기 지원본부", "BUSAN", "부산 동구 쪽방상담소", null, 6_000_000L, 2_640_000L),
                        new OpportunitySeed("DONATION", "GENERAL", "🦮 시각장애인 안내견 양성 후원",
                                        "한국 안내견 학교", "NATIONWIDE", "전국", null, 12_000_000L, 7_320_000L),
                        new OpportunitySeed("DONATION", "GENERAL", "🩺 의료취약지 이동검진 버스 운영 후원",
                                        "찾아가는 건강나눔 재단", "NATIONWIDE", "전국", null, 15_000_000L, 6_100_000L),
                        new OpportunitySeed("HOMETOWN_DONATION", "HOMETOWN", "🐋 부산 기장군 고향사랑기부 (해녀 문화 보전)",
                                        "기장군 고향사랑 기부팀", "BUSAN", "부산 기장군", null, null, null),
                        new OpportunitySeed("HOMETOWN_DONATION", "HOMETOWN", "🌉 부산 사하구 고향사랑기부 (감천마을 재생)",
                                        "사하구 고향사랑 기부팀", "BUSAN", "부산 사하구", null, null, null),
                        new OpportunitySeed("CULTURAL_HERITAGE_DONATION", "HERITAGE", "⛩️ 부산 충렬사 단청 보수 후원",
                                        "부산 문화재 지킴이회", "BUSAN", "부산 동래구 충렬대로", null, 7_000_000L, 2_950_000L),
                        new OpportunitySeed("LEGACY_DONATION", "LEGACY", "🕊️ 사회복지 유산기부 사전 상담 프로그램",
                                        "부산 유산기부 상담센터", "BUSAN", "부산 연제구 시청 인근", null, null, null));

        private void seedExtraOpportunities(Long managerId, Long organizationId) {
                int offset = 0;
                for (OpportunitySeed seed : EXTRA_OPPORTUNITIES) {
                        offset++;
                        Integer count = jdbcTemplate.queryForObject(
                                        "SELECT COUNT(*) FROM opportunities WHERE title = ?",
                                        Integer.class, seed.title());
                        if (count != null && count > 0) {
                                continue;
                        }

                        boolean volunteer = "VOLUNTEER".equals(seed.type());
                        LocalDate today = LocalDate.now();
                        Timestamp recruitmentEndAt = volunteer ? Timestamp.valueOf(today.plusDays(offset + 6).atStartOfDay()) : null;
                        Timestamp activityStartAt = volunteer ? Timestamp.valueOf(today.plusDays(offset + 9).atTime(9, 0)) : null;
                        Timestamp activityEndAt = volunteer ? Timestamp.valueOf(today.plusDays(offset + 9).atTime(13, 0)) : null;

                        jdbcTemplate.update("""
                                        INSERT INTO opportunities (
                                            organization_id, opportunity_type, category, title, summary, description,
                                            region, location, participation_mode, capacity, target_amount, current_amount,
                                            recruitment_end_at, activity_start_at, activity_end_at,
                                            status, created_by, published_at
                                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'OFFLINE', ?, ?, ?, ?, ?, ?, 'PUBLISHED', ?, CURRENT_TIMESTAMP)
                                        """,
                                        organizationId,
                                        seed.type(),
                                        seed.category(),
                                        seed.title(),
                                        seed.organizer(),
                                        seed.title() + " 프로그램의 상세 안내입니다.",
                                        seed.region(),
                                        seed.location(),
                                        seed.capacity(),
                                        seed.targetAmount(),
                                        seed.currentAmount() == null ? 0L : seed.currentAmount(),
                                        recruitmentEndAt,
                                        activityStartAt,
                                        activityEndAt,
                                        managerId);
                }
        }

        /**
         * 초기 시드가 넣어 둔 "~프로그램의 상세 안내입니다" 자리표시 문구를
         * 구분에 맞는 실제 설명으로 바꾼다. 상세 화면에서 읽을 내용이 생긴다.
         */
        private void refreshPlaceholderDescriptions() {
                jdbcTemplate.update("""
                                UPDATE opportunities
                                SET description = CASE opportunity_type
                                    WHEN 'VOLUNTEER' THEN CONCAT(
                                        '현장에서 이웃과 직접 만나는 봉사 프로그램입니다. 신청이 승인되면 담당자가 활동 시간과 집결 장소, 준비물을 안내합니다. ',
                                        '처음 참여하는 분도 현장 안내를 받은 뒤 함께할 수 있으며, 활동을 마치면 참여 확인이 기록으로 남습니다.')
                                    WHEN 'DONATION' THEN CONCAT(
                                        '모금 목표를 향해 이웃의 마음이 모이는 기부 프로그램입니다. 후원금은 주관기관의 사업 계획에 따라 집행되며, ',
                                        '약정 이후에도 진행 상황과 사용 내역을 확인할 수 있습니다.')
                                    WHEN 'HOMETOWN_DONATION' THEN CONCAT(
                                        '고향사랑기부제로 지역 공동체 사업을 지원하는 프로그램입니다. 현재 주민등록상 거주 지역을 제외한 곳에 기부할 수 있고, ',
                                        '기부액에 따라 세액공제와 지역 답례품 혜택이 함께 제공됩니다.')
                                    WHEN 'LEGACY_DONATION' THEN CONCAT(
                                        '삶의 가치를 다음 세대로 잇는 유산기부 상담 프로그램입니다. 상담에서 기부 시점과 방식, 가족과 합의할 사항을 ',
                                        '충분히 검토한 뒤 약정을 진행하므로 신청 단계에서 바로 체결되지 않습니다.')
                                    ELSE CONCAT(
                                        '문화유산을 지키고 알리는 일에 함께하는 후원 프로그램입니다. 후원금은 복원과 기록, 시민 교육 사업에 쓰이며 ',
                                        '주요 진행 경과는 후원자에게 공개됩니다.')
                                    END
                                WHERE description LIKE '%프로그램의 상세 안내입니다.'
                                """);
        }

        /**
         * 데모 모금 목표를 30~50만원 규모로 맞춘다.
         * 모금액은 실제 체결된 약정 합계로 계산하므로, 목표가 수천만원이면
         * 진행률이 늘 0%대로 보여 화면에서 의미를 읽기 어렵다.
         * id 기반이라 여러 번 실행해도 같은 값이 된다.
         */
        private void alignDemoFundingTargets() {
                jdbcTemplate.update("""
                                UPDATE opportunities
                                SET target_amount = 300000 + MOD(id, 3) * 100000
                                WHERE target_amount IS NOT NULL AND target_amount > 0
                                """);
        }

        /**
         * 서비스명을 잇다(ITDA)로 바꾸기 전에 만들어진 이름을 옮긴다.
         * 데모 센터를 이름으로 찾기 때문에, 옮기지 않으면 센터가 중복 생성된다.
         */
        private void rebrandLegacyNames() {
                jdbcTemplate.update(
                                "UPDATE organizations SET name = '잇다 데모 센터' WHERE name = '픽셀케어 데모 센터'");
                jdbcTemplate.update(
                                "UPDATE commitments SET title = REPLACE(title, '픽셀케어', '잇다') WHERE title LIKE '%픽셀케어%'");
                jdbcTemplate.update(
                                "UPDATE opportunities SET summary = REPLACE(summary, '픽셀케어', '잇다') WHERE summary LIKE '%픽셀케어%'");
                jdbcTemplate.update(
                                "UPDATE users SET nickname = REPLACE(nickname, '픽셀 이웃', '잇다 이웃') WHERE nickname LIKE '%픽셀 이웃%'");
                jdbcTemplate.update(
                                "UPDATE users SET nickname = REPLACE(nickname, '픽셀케어', '잇다') WHERE nickname LIKE '%픽셀케어%'");
        }

        /** 봉사 공고에는 참여 약정서를 필수 제출 서류로 붙인다. */
        private void ensureVolunteerRequiredDocuments() {
                jdbcTemplate.update("""
                                INSERT INTO opportunity_required_documents (
                                    opportunity_id, document_code, document_name, description,
                                    is_required, display_order
                                )
                                SELECT id, 'PARTICIPATION_PLEDGE', '참여 약정서',
                                       '프로그램 참여 조건과 준수사항을 확인합니다.', TRUE, 0
                                FROM opportunities o
                                WHERE o.opportunity_type = 'VOLUNTEER'
                                  AND NOT EXISTS (
                                      SELECT 1 FROM opportunity_required_documents d
                                      WHERE d.opportunity_id = o.id AND d.document_code = 'PARTICIPATION_PLEDGE'
                                  )
                                """);
        }

        /** 데모 회원 수. 홈 누적 현황이 비어 보이지 않도록 활동 이력까지 함께 만든다. */
        private static final int DEMO_MEMBER_COUNT = 28;

        /** 회원 한 명이 참여하는 봉사 건수. 공고 한 건은 4시간 활동으로 맞춘다. */
        private static final int VOLUNTEER_PER_MEMBER = 3;

        /** 정기·일시 기부 약정 금액 순환값. */
        private static final long[] PLEDGE_AMOUNTS = { 30_000L, 50_000L, 100_000L, 150_000L };

        /**
         * 홈 누적 현황과 센터 대시보드가 실제 데이터로 채워지도록 데모 활동 이력을 만든다.
         * 회원 → 봉사 참여(완료) → 기부 약정 → 전자서명 문서 순으로 이어 붙인다.
         */
        private void seedCommunityActivity(Long organizationId) {
                if (userExists("member01@pixelcare.demo"))
                        return;

                // 봉사 시간은 공고의 활동 시각에서 계산하므로 비어 있는 공고를 4시간 일정으로 채운다.
                List<Long> volIds = jdbcTemplate.query("""
                                SELECT id FROM opportunities
                                WHERE organization_id = ? AND opportunity_type = 'VOLUNTEER'
                                  AND is_deleted = FALSE AND activity_start_at IS NULL
                                """, (rs, rowNum) -> rs.getLong("id"), organizationId);
                LocalDate today = LocalDate.now();
                for (Long id : volIds) {
                        long daysOffset = id % 20;
                        Timestamp recruitmentEndAt = Timestamp.valueOf(today.plusDays(daysOffset + 5).atStartOfDay());
                        Timestamp activityStartAt = Timestamp.valueOf(today.plusDays(daysOffset + 8).atTime(9, 0));
                        Timestamp activityEndAt = Timestamp.valueOf(today.plusDays(daysOffset + 8).atTime(13, 0));
                        jdbcTemplate.update("""
                                        UPDATE opportunities
                                        SET recruitment_end_at = ?, activity_start_at = ?, activity_end_at = ?
                                        WHERE id = ?
                                        """, recruitmentEndAt, activityStartAt, activityEndAt, id);
                }

                List<Long> volunteerOpportunities = opportunityIds(organizationId, true);
                List<Long> donationOpportunities = opportunityIds(organizationId, false);
                if (volunteerOpportunities.size() < VOLUNTEER_PER_MEMBER || donationOpportunities.isEmpty()) {
                        return;
                }

                int pledgeIndex = 0;
                int signedCount = 0;
                for (int index = 0; index < DEMO_MEMBER_COUNT; index++) {
                        String suffix = String.format("%02d", index + 1);
                        Long memberId = ensureUser(
                                        "member" + suffix + "@pixelcare.demo",
                                        "잇다 이웃 " + suffix,
                                        "이웃" + suffix,
                                        donorPassword,
                                        "USER");
                        // 온기를 고르게 흩어 평균이 한쪽으로 쏠리지 않게 한다.
                        jdbcTemplate.update("UPDATE users SET temperature = ? WHERE id = ?",
                                        36.5 + ((index * 7) % 45), memberId);

                        for (int slot = 0; slot < VOLUNTEER_PER_MEMBER; slot++) {
                                Long opportunityId = volunteerOpportunities.get(
                                                (index + slot * 7) % volunteerOpportunities.size());
                                insertApplication(opportunityId, memberId, "COMPLETED");
                        }

                        if (index % 2 != 0)
                                continue;

                        Long opportunityId = donationOpportunities.get(pledgeIndex % donationOpportunities.size());
                        long amount = PLEDGE_AMOUNTS[pledgeIndex % PLEDGE_AMOUNTS.length];
                        pledgeIndex++;

                        Long applicationId = insertApplication(opportunityId, memberId, "APPROVED");
                        if (applicationId == null)
                                continue;
                        Long commitmentId = insertDonationCommitment(
                                        applicationId, opportunityId, memberId, organizationId, amount);

                        // 일부는 서명 대기 상태로 남겨 대시보드가 한쪽으로만 보이지 않게 한다.
                        boolean signed = signedCount < 11;
                        insertClmDocument(commitmentId, opportunityId, memberId, signed);
                        if (signed)
                                signedCount++;
                }
        }

        /**
         * 다이어리는 참여 희망일을 기준으로 날짜를 잡는다.
         * 초기 시드에는 이 값이 없어 캘린더가 비어 보이므로 활동 일정에서 채운다.
         */
        private void backfillParticipationDates() {
                List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                                SELECT a.id AS app_id, o.activity_start_at AS act_start
                                FROM applications a
                                JOIN opportunities o ON o.id = a.opportunity_id
                                WHERE a.participation_date IS NULL
                                """);
                for (Map<String, Object> row : rows) {
                        Long appId = ((Number) row.get("app_id")).longValue();
                        Object actStartObj = row.get("act_start");
                        LocalDate participationDate;
                        if (actStartObj instanceof Timestamp ts) {
                                participationDate = ts.toLocalDateTime().toLocalDate();
                        } else if (actStartObj instanceof LocalDateTime ldt) {
                                participationDate = ldt.toLocalDate();
                        } else if (actStartObj instanceof Date d) {
                                participationDate = d.toLocalDate();
                        } else {
                                participationDate = LocalDate.now().minusDays(appId % 28);
                        }
                        jdbcTemplate.update("UPDATE applications SET participation_date = ? WHERE id = ?", Date.valueOf(participationDate), appId);
                }
        }

        private boolean userExists(String email) {
                Integer count = jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM users WHERE email = ?", Integer.class, email);
                return count != null && count > 0;
        }

        private List<Long> opportunityIds(Long organizationId, boolean volunteer) {
                return jdbcTemplate.query("""
                                SELECT id FROM opportunities
                                WHERE organization_id = ? AND is_deleted = FALSE
                                  AND opportunity_type %s 'VOLUNTEER'
                                ORDER BY id
                                """.formatted(volunteer ? "=" : "<>"),
                                (rs, rowNum) -> rs.getLong("id"), organizationId);
        }

        /** 같은 공고에 같은 사람이 두 번 신청할 수 없으므로 중복이면 건너뛴다. */
        private Long insertApplication(Long opportunityId, Long userId, String status) {
                Integer existing = jdbcTemplate.queryForObject("""
                                SELECT COUNT(*) FROM applications
                                WHERE opportunity_id = ? AND applicant_user_id = ?
                                """, Integer.class, opportunityId, userId);
                if (existing != null && existing > 0)
                        return null;

                KeyHolder keyHolder = new GeneratedKeyHolder();
                jdbcTemplate.update(connection -> {
                        PreparedStatement statement = connection.prepareStatement("""
                                        INSERT INTO applications (
                                            public_id, opportunity_id, applicant_user_id, status,
                                            submitted_at, updated_at
                                        ) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                                        """, new String[] { "id" });
                        statement.setString(1, UUID.randomUUID().toString());
                        statement.setLong(2, opportunityId);
                        statement.setLong(3, userId);
                        statement.setString(4, status);
                        return statement;
                }, keyHolder);
                return KeyExtractUtils.extractId(keyHolder);
        }

        private Long insertDonationCommitment(
                        Long applicationId, Long opportunityId, Long userId, Long organizationId, long amount) {
                String title = "잇다 기부 약정 " + money(amount) + "원";
                KeyHolder keyHolder = new GeneratedKeyHolder();
                jdbcTemplate.update(connection -> {
                        PreparedStatement statement = connection.prepareStatement("""
                                        INSERT INTO commitments (
                                            public_id, application_id, opportunity_id, user_id, organization_id,
                                            current_version_no, commitment_status, title, commitment_type,
                                            pledge_amount, pledge_frequency, effective_from, signed_at
                                        ) VALUES (?, ?, ?, ?, ?, 1, 'ACTIVE', ?, 'DONATION', ?, 'ONE_TIME',
                                                  CURRENT_DATE, CURRENT_TIMESTAMP)
                                        """, new String[] { "id" });
                        statement.setString(1, UUID.randomUUID().toString());
                        statement.setLong(2, applicationId);
                        statement.setLong(3, opportunityId);
                        statement.setLong(4, userId);
                        statement.setLong(5, organizationId);
                        statement.setString(6, title);
                        statement.setLong(7, amount);
                        return statement;
                }, keyHolder);
                Long commitmentId = KeyExtractUtils.extractId(keyHolder);

                jdbcTemplate.update("DELETE FROM commitment_versions WHERE commitment_id = ? AND version_no = 1", commitmentId);
                jdbcTemplate.update("""
                                INSERT INTO commitment_versions (
                                    commitment_id, version_no, terms_json, rendered_content, change_summary, created_by
                                ) VALUES (?, 1, ?, ?, '기부 약정 체결', ?)
                                """,
                                commitmentId,
                                "{\"specialConditions\":\"\",\"privacyConsent\":true,"
                                                + "\"thirdPartyConsent\":true,\"portraitConsent\":false}",
                                "[기부 약정서]\n약정 금액: " + money(amount) + "원\n약정 주기: 일시\n"
                                                + "개인정보 수집 동의: 동의\n제3자 제공 동의: 동의\n",
                                userId);
                return commitmentId;
        }

        /** 데모용 전자서명 문서. 실제 모두싸인 문서가 아니므로 식별자에 demo 접두어를 둔다. */
        private void insertClmDocument(
                        Long commitmentId, Long opportunityId, Long userId, boolean signed) {
                jdbcTemplate.update("""
                                INSERT INTO clm_documents (
                                    commitment_id, volunteer_id, volunteer_title, applicant_user_id,
                                    applicant_name, applicant_email, modusign_document_id,
                                    signing_method, status, signed_at, last_event_type, last_event_rank
                                )
                                SELECT ?, o.id, o.title, u.id, u.name, u.email, ?,
                                       'SECURE_LINK', ?, %s, ?, ?
                                FROM opportunities o JOIN users u ON u.id = ?
                                WHERE o.id = ?
                                """.formatted(signed ? "CURRENT_TIMESTAMP" : "NULL"),
                                commitmentId,
                                "demo-" + UUID.randomUUID(),
                                signed ? "SIGNED" : "PENDING_SIGNATURE",
                                signed ? "document_all_signed" : null,
                                signed ? 100 : 0,
                                userId,
                                opportunityId);
        }

        private static String money(long amount) {
                return java.text.NumberFormat.getNumberInstance(java.util.Locale.KOREA).format(amount);
        }

        private Long ensureUser(
                        String email,
                        String nickname,
                        String name,
                        String password,
                        String role) {
                List<Long> existing = jdbcTemplate.query(
                                "SELECT id FROM users WHERE email = ?",
                                (rs, rowNum) -> rs.getLong("id"),
                                email);
                Long userId;
                if (existing.isEmpty()) {
                        KeyHolder keyHolder = new GeneratedKeyHolder();
                        jdbcTemplate.update(connection -> {
                                PreparedStatement statement = connection.prepareStatement(
                                                """
                                                                INSERT INTO users (
                                                                    email, password_hash, nickname, name, role,
                                                                    account_status, privacy_consent_at, created_at, updated_at, is_deleted, temperature
                                                                ) VALUES (?, ?, ?, ?, ?, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE, 36.5)
                                                                """,
                                                new String[]{"id"});
                                statement.setString(1, email);
                                statement.setString(2, passwordEncoder.encode(password));
                                statement.setString(3, nickname);
                                statement.setString(4, name);
                                statement.setString(5, role);
                                return statement;
                        }, keyHolder);
                        userId = KeyExtractUtils.extractId(keyHolder);
                } else {
                        userId = existing.get(0);
                }
                Integer roleCount = jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM user_roles WHERE user_id = ? AND role = ?",
                                Integer.class, userId, role);
                if (roleCount == null || roleCount == 0) {
                        jdbcTemplate.update("INSERT INTO user_roles (user_id, role) VALUES (?, ?)", userId, role);
                }
                jdbcTemplate.update(
                                "UPDATE users SET role = ?, password_hash = COALESCE(password_hash, ?) WHERE id = ?",
                                role,
                                passwordEncoder.encode(password),
                                userId);
                return userId;
        }

        private Long ensureDemoOrganization(Long managerId) {
                List<Long> existing = jdbcTemplate.query("""
                                SELECT o.id
                                FROM organizations o
                                JOIN organization_managers om ON om.organization_id = o.id
                                WHERE om.user_id = ? AND o.name = '잇다 데모 센터'
                                """, (rs, rowNum) -> rs.getLong("id"), managerId);
                if (!existing.isEmpty()) {
                        return existing.get(0);
                }
                KeyHolder keyHolder = new GeneratedKeyHolder();
                jdbcTemplate.update(connection -> {
                        PreparedStatement statement = connection.prepareStatement("""
                                        INSERT INTO organizations (
                                            name, organization_type, representative_name, phone, email,
                                            address, description, verification_status
                                        ) VALUES (
                                            '잇다 데모 센터', 'SOCIAL_WELFARE',
                                            '데모 관리자', '051-000-0000', 'manager@pixelcare.demo',
                                            '부산광역시 금정구', '로컬 개발용 데모 센터입니다.', 'VERIFIED'
                                        )
                                        """, new String[] { "id" });
                        return statement;
                }, keyHolder);
                Long organizationId = KeyExtractUtils.extractId(keyHolder);
                jdbcTemplate.update("""
                                INSERT INTO organization_managers (organization_id, user_id, manager_role)
                                VALUES (?, ?, 'OWNER')
                                """, organizationId, managerId);
                return organizationId;
        }

        /**
         * 정기 약정 갱신 흐름을 시연할 수 있도록 월간·연간 약정을 하나씩 만든다.
         * 하나는 갱신일이 지난 상태(갱신 필요), 다른 하나는 갱신 예정 상태로 둔다.
         */
        private void seedRecurringCommitments(Long donorId, Long organizationId) {
                List<Long> opportunityIds = jdbcTemplate.query("""
                                SELECT id FROM opportunities
                                WHERE organization_id = ?
                                  AND opportunity_type IN ('DONATION', 'HOMETOWN_DONATION')
                                  AND is_deleted = FALSE
                                ORDER BY id
                                LIMIT 2
                                """, (rs, rowNum) -> rs.getLong("id"), organizationId);
                if (opportunityIds.size() < 2)
                        return;

                ensureRecurringCommitment(
                                donorId, organizationId, opportunityIds.get(0),
                                "매월 이어가는 아동 결식 예방 정기후원 약정",
                                "MONTHLY", new BigDecimal("30000"), LocalDate.now().minusDays(3));
                ensureRecurringCommitment(
                                donorId, organizationId, opportunityIds.get(1),
                                "매년 이어가는 지역 문화유산 보존 후원 약정",
                                "ANNUAL", new BigDecimal("120000"), LocalDate.now().plusDays(12));
        }

        private void ensureRecurringCommitment(
                        Long donorId,
                        Long organizationId,
                        Long opportunityId,
                        String title,
                        String frequency,
                        BigDecimal amount,
                        LocalDate renewalDueAt) {
                Integer existing = jdbcTemplate.queryForObject("""
                                SELECT COUNT(*) FROM commitments
                                WHERE user_id = ? AND opportunity_id = ?
                                """, Integer.class, donorId, opportunityId);
                if (existing != null && existing > 0)
                        return;

                KeyHolder applicationKey = new GeneratedKeyHolder();
                jdbcTemplate.update(connection -> {
                        PreparedStatement statement = connection.prepareStatement("""
                                        INSERT INTO applications (
                                            public_id, opportunity_id, applicant_user_id, status, submitted_at
                                        ) VALUES (?, ?, ?, 'APPROVED', CURRENT_TIMESTAMP)
                                        """, new String[] { "id" });
                        statement.setString(1, UUID.randomUUID().toString());
                        statement.setLong(2, opportunityId);
                        statement.setLong(3, donorId);
                        return statement;
                }, applicationKey);
                Long applicationId = KeyExtractUtils.extractId(applicationKey);

                LocalDate effectiveFrom = "MONTHLY".equals(frequency)
                                ? renewalDueAt.minusMonths(1)
                                : renewalDueAt.minusYears(1);

                KeyHolder commitmentKey = new GeneratedKeyHolder();
                jdbcTemplate.update(connection -> {
                        PreparedStatement statement = connection.prepareStatement(
                                        """
                                                        INSERT INTO commitments (
                                                            public_id, application_id, opportunity_id, user_id, organization_id,
                                                            current_version_no, commitment_status, title, commitment_type,
                                                            pledge_amount, pledge_frequency, renewal_due_at,
                                                            effective_from, signed_at
                                                        ) VALUES (?, ?, ?, ?, ?, 1, 'ACTIVE', ?, 'DONATION', ?, ?, ?, ?, CURRENT_TIMESTAMP)
                                                        """,
                                        new String[] { "id" });
                        statement.setString(1, UUID.randomUUID().toString());
                        statement.setLong(2, applicationId);
                        statement.setLong(3, opportunityId);
                        statement.setLong(4, donorId);
                        statement.setLong(5, organizationId);
                        statement.setString(6, title);
                        statement.setBigDecimal(7, amount);
                        statement.setString(8, frequency);
                        statement.setDate(9, Date.valueOf(renewalDueAt));
                        statement.setDate(10, Date.valueOf(effectiveFrom));
                        return statement;
                }, commitmentKey);
                Long commitmentId = KeyExtractUtils.extractId(commitmentKey);

                String rendered = """
                                [정기 후원 약정서]
                                프로그램: %s
                                약정 금액: %s원
                                약정 주기: %s
                                약정 시작일: %s
                                다음 갱신일: %s
                                개인정보 수집 동의: 동의
                                제3자 제공 동의: 동의
                                """.formatted(
                                title,
                                amount.toBigInteger(),
                                "MONTHLY".equals(frequency) ? "매월 정기" : "매년 정기",
                                effectiveFrom,
                                renewalDueAt);
                jdbcTemplate.update("DELETE FROM commitment_versions WHERE commitment_id = ? AND version_no = 1", commitmentId);
                jdbcTemplate.update("""
                                INSERT INTO commitment_versions (
                                    commitment_id, version_no, terms_json, rendered_content,
                                    change_summary, created_by
                                ) VALUES (?, 1, ?, ?, ?, ?)
                                """,
                                commitmentId,
                                "{\"specialConditions\":\"\",\"privacyConsent\":true,"
                                                + "\"thirdPartyConsent\":true,\"portraitConsent\":false}",
                                rendered,
                                "정기 약정 체결",
                                donorId);
        }

        private void seedOpportunities(Long managerId, Long organizationId) {
                jdbcTemplate.update("""
                                INSERT INTO opportunities (
                                    organization_id, opportunity_type, category, title, summary,
                                    description, region, location, participation_mode, capacity,
                                    target_amount, current_amount, status, created_by, published_at
                                )
                                SELECT ?,
                                       CASE v.category
                                           WHEN 'VOLUNTEER' THEN 'VOLUNTEER'
                                           WHEN 'HOMETOWN' THEN 'HOMETOWN_DONATION'
                                           WHEN 'LEGACY' THEN 'LEGACY_DONATION'
                                           WHEN 'UNESCO' THEN 'CULTURAL_HERITAGE_DONATION'
                                           WHEN 'HERITAGE' THEN 'CULTURAL_HERITAGE_DONATION'
                                           ELSE 'DONATION'
                                       END,
                                       v.category,
                                       v.title,
                                       v.organizer,
                                       CONCAT(v.title, ' 프로그램의 상세 안내입니다.'),
                                       CASE
                                           WHEN v.location LIKE '서울%' THEN 'SEOUL'
                                           WHEN v.location LIKE '부산%' THEN 'BUSAN'
                                           WHEN v.location LIKE '대구%' THEN 'DAEGU'
                                           WHEN v.location LIKE '광주%' THEN 'GWANGJU'
                                           ELSE 'NATIONWIDE'
                                       END,
                                       v.location,
                                       'OFFLINE',
                                       CASE WHEN v.category = 'VOLUNTEER' THEN 30 ELSE NULL END,
                                       v.target_amount,
                                       v.current_amount,
                                       'PUBLISHED',
                                       ?,
                                       CURRENT_TIMESTAMP
                                FROM volunteers v
                                WHERE NOT EXISTS (
                                    SELECT 1
                                    FROM opportunities o
                                    WHERE o.title = v.title
                                )
                                """, organizationId, managerId);
                jdbcTemplate.update("""
                                INSERT INTO opportunity_required_documents (
                                    opportunity_id, document_code, document_name, description,
                                    is_required, display_order
                                )
                                SELECT id, 'PARTICIPATION_PLEDGE', '참여 약정서',
                                       '프로그램 참여 조건과 준수사항을 확인합니다.', TRUE, 0
                                FROM opportunities o
                                WHERE o.opportunity_type = 'VOLUNTEER'
                                  AND NOT EXISTS (
                                      SELECT 1
                                      FROM opportunity_required_documents d
                                      WHERE d.opportunity_id = o.id
                                        AND d.document_code = 'PARTICIPATION_PLEDGE'
                                  )
                                """);
        }

        private void safeAddColumn(String table, String column, String type) {
                try {
                        jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
                } catch (Exception ignored) {
                }
        }

        private void ensureSchemaTablesExist() {
                boolean isPostgres = false;
                try (var conn = jdbcTemplate.getDataSource().getConnection()) {
                        String dbName = conn.getMetaData().getDatabaseProductName();
                        if (dbName != null && dbName.toLowerCase().contains("postgres")) {
                                isPostgres = true;
                        }
                } catch (Exception ignored) {
                }

                String autoInc = isPostgres ? "BIGSERIAL PRIMARY KEY" : "BIGINT AUTO_INCREMENT PRIMARY KEY";
                String dateTimeType = isPostgres ? "TIMESTAMP" : "DATETIME";

                jdbcTemplate.execute("""
                                CREATE TABLE IF NOT EXISTS user_roles (
                                    id BIGINT NULL,
                                    user_id BIGINT NOT NULL,
                                    role VARCHAR(50) NOT NULL,
                                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                                )
                                """);
                safeAddColumn("user_roles", "id", "BIGINT");
                safeAddColumn("user_roles", "created_at", dateTimeType + " DEFAULT CURRENT_TIMESTAMP");

                jdbcTemplate.execute("""
                                CREATE TABLE IF NOT EXISTS user_interests (
                                    id BIGINT NULL,
                                    user_id BIGINT NOT NULL,
                                    interest VARCHAR(100) NULL,
                                    interest_code VARCHAR(100) NULL,
                                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                                )
                                """);
                safeAddColumn("user_interests", "interest_code", "VARCHAR(100)");

                jdbcTemplate.execute(String.format("""
                                CREATE TABLE IF NOT EXISTS refresh_tokens (
                                    id %s,
                                    user_id BIGINT NOT NULL,
                                    token_hash VARCHAR(255) NOT NULL,
                                    expires_at %s NOT NULL,
                                    revoked_at %s NULL,
                                    created_at %s DEFAULT CURRENT_TIMESTAMP
                                )
                                """, autoInc, dateTimeType, dateTimeType, dateTimeType));

                jdbcTemplate.execute(String.format("""
                                CREATE TABLE IF NOT EXISTS access_tokens (
                                    id %s,
                                    user_id BIGINT NOT NULL,
                                    token_hash VARCHAR(255) NOT NULL,
                                    expires_at %s NOT NULL,
                                    created_at %s DEFAULT CURRENT_TIMESTAMP,
                                    revoked_at %s NULL
                                )
                                """, autoInc, dateTimeType, dateTimeType, dateTimeType));

                jdbcTemplate.execute(String.format("""
                                CREATE TABLE IF NOT EXISTS stored_files (
                                    id %s,
                                    owner_user_id BIGINT NULL,
                                    storage_key VARCHAR(500) NULL,
                                    original_name VARCHAR(255) NULL,
                                    original_filename VARCHAR(255) NULL,
                                    stored_filename VARCHAR(255) NULL,
                                    file_path VARCHAR(500) NULL,
                                    file_size BIGINT NULL,
                                    size_bytes BIGINT NULL,
                                    content_type VARCHAR(100) NULL,
                                    mime_type VARCHAR(100) NULL,
                                    checksum VARCHAR(128) NULL,
                                    checksum_sha256 VARCHAR(128) NULL,
                                    file_purpose VARCHAR(50) NULL,
                                    category VARCHAR(50) NULL,
                                    created_at %s DEFAULT CURRENT_TIMESTAMP,
                                    deleted_at %s NULL
                                )
                                """, autoInc, dateTimeType, dateTimeType));
                safeAddColumn("stored_files", "storage_key", "VARCHAR(500)");
                safeAddColumn("stored_files", "original_name", "VARCHAR(255)");
                safeAddColumn("stored_files", "original_filename", "VARCHAR(255)");
                safeAddColumn("stored_files", "stored_filename", "VARCHAR(255)");
                safeAddColumn("stored_files", "file_path", "VARCHAR(500)");
                safeAddColumn("stored_files", "size_bytes", "BIGINT");
                safeAddColumn("stored_files", "file_size", "BIGINT");
                safeAddColumn("stored_files", "content_type", "VARCHAR(100)");
                safeAddColumn("stored_files", "mime_type", "VARCHAR(100)");
                safeAddColumn("stored_files", "checksum", "VARCHAR(128)");
                safeAddColumn("stored_files", "file_purpose", "VARCHAR(50)");

                jdbcTemplate.execute(String.format("""
                                CREATE TABLE IF NOT EXISTS organizations (
                                    id %s,
                                    source_application_id BIGINT NULL,
                                    name VARCHAR(255) NOT NULL,
                                    organization_type VARCHAR(50) NULL,
                                    registration_number VARCHAR(50) NULL,
                                    representative_name VARCHAR(100) NULL,
                                    phone VARCHAR(50) NULL,
                                    phone_number VARCHAR(50) NULL,
                                    email VARCHAR(255) NULL,
                                    address VARCHAR(500) NULL,
                                    description TEXT NULL,
                                    logo_file_id BIGINT NULL,
                                    verification_status VARCHAR(30) DEFAULT 'VERIFIED',
                                    organization_status VARCHAR(50) DEFAULT 'APPROVED',
                                    created_at %s DEFAULT CURRENT_TIMESTAMP,
                                    updated_at %s NULL,
                                    is_deleted BOOLEAN DEFAULT FALSE,
                                    deleted_at %s NULL,
                                    deleted_by VARCHAR(255) NULL
                                )
                                """, autoInc, dateTimeType, dateTimeType, dateTimeType));
                safeAddColumn("organizations", "source_application_id", "BIGINT");
                safeAddColumn("organizations", "phone", "VARCHAR(50)");
                safeAddColumn("organizations", "phone_number", "VARCHAR(50)");
                safeAddColumn("organizations", "email", "VARCHAR(255)");
                safeAddColumn("organizations", "description", "TEXT");
                safeAddColumn("organizations", "logo_file_id", "BIGINT");
                safeAddColumn("organizations", "verification_status", "VARCHAR(30) DEFAULT 'VERIFIED'");
                safeAddColumn("organizations", "updated_at", dateTimeType);
                safeAddColumn("organizations", "is_deleted", "BOOLEAN DEFAULT FALSE");

                jdbcTemplate.execute(String.format("""
                                CREATE TABLE IF NOT EXISTS organization_managers (
                                    id %s,
                                    organization_id BIGINT NOT NULL,
                                    user_id BIGINT NOT NULL,
                                    manager_role VARCHAR(50) DEFAULT 'PRIMARY',
                                    joined_at %s DEFAULT CURRENT_TIMESTAMP,
                                    left_at %s NULL
                                )
                                """, autoInc, dateTimeType, dateTimeType));

                jdbcTemplate.execute(String.format("""
                                CREATE TABLE IF NOT EXISTS manager_applications (
                                    id %s,
                                    applicant_user_id BIGINT NULL,
                                    user_id BIGINT NULL,
                                    organization_id BIGINT NULL,
                                    organization_name VARCHAR(200) NULL,
                                    business_registration_number VARCHAR(50) NULL,
                                    contact_number VARCHAR(50) NULL,
                                    phone VARCHAR(50) NULL,
                                    email VARCHAR(255) NULL,
                                    proof_file_id BIGINT NULL,
                                    reason TEXT NULL,
                                    status VARCHAR(50) DEFAULT 'PENDING',
                                    application_status VARCHAR(50) DEFAULT 'PENDING',
                                    submitted_at %s DEFAULT CURRENT_TIMESTAMP,
                                    reviewed_at %s NULL,
                                    reviewed_by BIGINT NULL,
                                    rejection_reason TEXT NULL,
                                    created_at %s DEFAULT CURRENT_TIMESTAMP,
                                    updated_at %s NULL
                                )
                                """, autoInc, dateTimeType, dateTimeType, dateTimeType, dateTimeType));
                safeAddColumn("manager_applications", "applicant_user_id", "BIGINT");
                safeAddColumn("manager_applications", "organization_id", "BIGINT");
                safeAddColumn("manager_applications", "proof_file_id", "BIGINT");
                safeAddColumn("manager_applications", "reason", "TEXT");

                jdbcTemplate.execute(String.format("""
                                CREATE TABLE IF NOT EXISTS manager_application_files (
                                    manager_application_id BIGINT NULL,
                                    application_id BIGINT NULL,
                                    file_id BIGINT NULL,
                                    stored_file_id BIGINT NULL,
                                    file_purpose VARCHAR(50) NULL
                                )
                                """));

                jdbcTemplate.execute(String.format("""
                                CREATE TABLE IF NOT EXISTS organization_applications (
                                    id %s,
                                    applicant_user_id BIGINT NULL,
                                    name VARCHAR(255) NULL,
                                    organization_type VARCHAR(50) NULL,
                                    registration_number VARCHAR(50) NULL,
                                    representative_name VARCHAR(100) NULL,
                                    phone VARCHAR(50) NULL,
                                    phone_number VARCHAR(50) NULL,
                                    email VARCHAR(255) NULL,
                                    address VARCHAR(500) NULL,
                                    description TEXT NULL,
                                    proof_file_id BIGINT NULL,
                                    status VARCHAR(50) DEFAULT 'PENDING',
                                    submitted_at %s DEFAULT CURRENT_TIMESTAMP,
                                    reviewed_at %s NULL,
                                    reviewed_by BIGINT NULL,
                                    rejection_reason TEXT NULL,
                                    created_at %s DEFAULT CURRENT_TIMESTAMP,
                                    updated_at %s NULL
                                )
                                """, autoInc, dateTimeType, dateTimeType, dateTimeType, dateTimeType));
                safeAddColumn("organization_applications", "applicant_user_id", "BIGINT");
                safeAddColumn("organization_applications", "phone", "VARCHAR(50)");
                safeAddColumn("organization_applications", "email", "VARCHAR(255)");
                safeAddColumn("organization_applications", "description", "TEXT");
                safeAddColumn("organization_applications", "proof_file_id", "BIGINT");

                jdbcTemplate.execute(String.format("""
                                CREATE TABLE IF NOT EXISTS contract_templates (
                                    id %s,
                                    organization_id BIGINT NULL,
                                    opportunity_type VARCHAR(50) NULL,
                                    name VARCHAR(255) NULL,
                                    title VARCHAR(255) NULL,
                                    template_code VARCHAR(100) NULL,
                                    description TEXT NULL,
                                    status VARCHAR(30) DEFAULT 'ACTIVE',
                                    created_by BIGINT NULL,
                                    created_at %s DEFAULT CURRENT_TIMESTAMP,
                                    updated_at %s NULL
                                )
                                """, autoInc, dateTimeType, dateTimeType));

                jdbcTemplate.execute(String.format("""
                                CREATE TABLE IF NOT EXISTS contract_template_versions (
                                    id %s,
                                    template_id BIGINT NULL,
                                    version_no INT DEFAULT 1,
                                    schema_json TEXT NULL,
                                    body_template TEXT NULL,
                                    content TEXT NULL,
                                    change_note VARCHAR(500) NULL,
                                    created_by BIGINT NULL,
                                    created_at %s DEFAULT CURRENT_TIMESTAMP
                                )
                                """, autoInc, dateTimeType));

                jdbcTemplate.execute(String.format("""
                                CREATE TABLE IF NOT EXISTS opportunities (
                                    id %s,
                                    public_id VARCHAR(100) NULL,
                                    organization_id BIGINT NULL,
                                    template_id BIGINT NULL,
                                    opportunity_type VARCHAR(50) NULL,
                                    type VARCHAR(50) NULL,
                                    category VARCHAR(50) NULL,
                                    organizer VARCHAR(100) NULL,
                                    title VARCHAR(255) NULL,
                                    summary VARCHAR(1000) NULL,
                                    description TEXT NULL,
                                    region VARCHAR(50) NULL,
                                    location VARCHAR(500) NULL,
                                    participation_mode VARCHAR(50) DEFAULT 'OFFLINE',
                                    recruitment_start_at %s NULL,
                                    recruitment_end_at %s NULL,
                                    activity_start_at %s NULL,
                                    activity_end_at %s NULL,
                                    capacity INT NULL,
                                    target_amount BIGINT NULL,
                                    current_amount BIGINT DEFAULT 0,
                                    thumbnail_file_id BIGINT NULL,
                                    external_url VARCHAR(1000) NULL,
                                    status VARCHAR(30) DEFAULT 'PUBLISHED',
                                    created_by BIGINT NULL,
                                    published_at %s NULL,
                                    created_at %s DEFAULT CURRENT_TIMESTAMP,
                                    updated_at %s DEFAULT CURRENT_TIMESTAMP,
                                    is_deleted BOOLEAN DEFAULT FALSE,
                                    deleted_at %s NULL,
                                    deleted_by VARCHAR(255) NULL
                                )
                                """, autoInc, dateTimeType, dateTimeType, dateTimeType, dateTimeType, dateTimeType, dateTimeType, dateTimeType, dateTimeType));
                safeAddColumn("opportunities", "public_id", "VARCHAR(100)");
                safeAddColumn("opportunities", "template_id", "BIGINT");
                safeAddColumn("opportunities", "opportunity_type", "VARCHAR(50)");
                safeAddColumn("opportunities", "summary", "VARCHAR(1000)");
                safeAddColumn("opportunities", "participation_mode", "VARCHAR(50) DEFAULT 'OFFLINE'");
                safeAddColumn("opportunities", "recruitment_start_at", dateTimeType);
                safeAddColumn("opportunities", "recruitment_end_at", dateTimeType);
                safeAddColumn("opportunities", "activity_start_at", dateTimeType);
                safeAddColumn("opportunities", "activity_end_at", dateTimeType);
                safeAddColumn("opportunities", "thumbnail_file_id", "BIGINT");
                safeAddColumn("opportunities", "external_url", "VARCHAR(1000)");
                safeAddColumn("opportunities", "created_by", "BIGINT");
                safeAddColumn("opportunities", "published_at", dateTimeType);
                safeAddColumn("opportunities", "is_deleted", "BOOLEAN DEFAULT FALSE");

                jdbcTemplate.execute(String.format("""
                                CREATE TABLE IF NOT EXISTS opportunity_required_documents (
                                    id %s,
                                    opportunity_id BIGINT NULL,
                                    document_code VARCHAR(50) NULL,
                                    document_name VARCHAR(255) NULL,
                                    description VARCHAR(1000) NULL,
                                    is_required BOOLEAN DEFAULT TRUE,
                                    display_order INT DEFAULT 0
                                )
                                """, autoInc));

                jdbcTemplate.execute(String.format("""
                                CREATE TABLE IF NOT EXISTS applications (
                                    id %s,
                                    public_id VARCHAR(100) NULL,
                                    opportunity_id BIGINT NULL,
                                    applicant_user_id BIGINT NULL,
                                    applicant_id BIGINT NULL,
                                    consultation_id BIGINT NULL,
                                    answers_json TEXT NULL,
                                    status VARCHAR(30) DEFAULT 'APPROVED',
                                    application_status VARCHAR(30) DEFAULT 'APPROVED',
                                    participation_date DATE NULL,
                                    notes TEXT NULL,
                                    submitted_at %s NULL,
                                    reviewed_by BIGINT NULL,
                                    reviewed_at %s NULL,
                                    rejection_reason TEXT NULL,
                                    created_at %s DEFAULT CURRENT_TIMESTAMP,
                                    updated_at %s DEFAULT CURRENT_TIMESTAMP
                                )
                                """, autoInc, dateTimeType, dateTimeType, dateTimeType, dateTimeType));
                safeAddColumn("applications", "public_id", "VARCHAR(100)");
                safeAddColumn("applications", "applicant_user_id", "BIGINT");
                safeAddColumn("applications", "applicant_id", "BIGINT");
                safeAddColumn("applications", "consultation_id", "BIGINT");
                safeAddColumn("applications", "answers_json", "TEXT");
                safeAddColumn("applications", "participation_date", "DATE");
                safeAddColumn("applications", "submitted_at", dateTimeType);
                safeAddColumn("applications", "reviewed_by", "BIGINT");
                safeAddColumn("applications", "reviewed_at", dateTimeType);
                safeAddColumn("applications", "rejection_reason", "TEXT");

                jdbcTemplate.execute(String.format("""
                                CREATE TABLE IF NOT EXISTS commitments (
                                    id %s,
                                    public_id VARCHAR(100) NULL,
                                    application_id BIGINT NULL,
                                    opportunity_id BIGINT NULL,
                                    user_id BIGINT NULL,
                                    organization_id BIGINT NULL,
                                    current_version_no INT DEFAULT 1,
                                    commitment_status VARCHAR(30) DEFAULT 'ACTIVE',
                                    status VARCHAR(50) DEFAULT 'ACTIVE',
                                    commitment_type VARCHAR(50) NULL,
                                    recurring_amount BIGINT NULL,
                                    title VARCHAR(255) NULL,
                                    effective_from DATE NULL,
                                    effective_to DATE NULL,
                                    signed_at %s NULL,
                                    completed_at %s NULL,
                                    cancelled_at %s NULL,
                                    created_at %s DEFAULT CURRENT_TIMESTAMP,
                                    updated_at %s DEFAULT CURRENT_TIMESTAMP
                                )
                                """, autoInc, dateTimeType, dateTimeType, dateTimeType, dateTimeType, dateTimeType));
                safeAddColumn("commitments", "public_id", "VARCHAR(100)");
                safeAddColumn("commitments", "opportunity_id", "BIGINT");
                safeAddColumn("commitments", "user_id", "BIGINT");
                safeAddColumn("commitments", "organization_id", "BIGINT");
                safeAddColumn("commitments", "current_version_no", "INT DEFAULT 1");
                safeAddColumn("commitments", "commitment_status", "VARCHAR(30) DEFAULT 'ACTIVE'");
                safeAddColumn("commitments", "effective_from", "DATE");
                safeAddColumn("commitments", "effective_to", "DATE");
                safeAddColumn("commitments", "signed_at", dateTimeType);

                jdbcTemplate.execute(String.format("""
                                CREATE TABLE IF NOT EXISTS commitment_versions (
                                    id %s,
                                    commitment_id BIGINT NULL,
                                    version_no INT DEFAULT 1,
                                    version INT DEFAULT 1,
                                    template_version_id BIGINT NULL,
                                    terms_json TEXT NULL,
                                    rendered_content TEXT NULL,
                                    content TEXT NULL,
                                    change_summary VARCHAR(1000) NULL,
                                    created_by BIGINT NULL,
                                    created_at %s DEFAULT CURRENT_TIMESTAMP
                                )
                                """, autoInc, dateTimeType));
                safeAddColumn("commitment_versions", "version_no", "INT DEFAULT 1");
                safeAddColumn("commitment_versions", "terms_json", "TEXT");
                safeAddColumn("commitment_versions", "rendered_content", "TEXT");
                safeAddColumn("commitment_versions", "created_by", "BIGINT");

                jdbcTemplate.execute(String.format("""
                                CREATE TABLE IF NOT EXISTS consents (
                                    id %s,
                                    user_id BIGINT NULL,
                                    commitment_id BIGINT NULL,
                                    commitment_version_id BIGINT NULL,
                                    consent_type VARCHAR(50) NULL,
                                    policy_version VARCHAR(50) NULL,
                                    is_consented BOOLEAN DEFAULT TRUE,
                                    is_agreed BOOLEAN DEFAULT TRUE,
                                    consented_at %s DEFAULT CURRENT_TIMESTAMP,
                                    agreed_at %s NULL,
                                    withdrawn_at %s NULL,
                                    ip_address VARCHAR(45) NULL,
                                    user_agent VARCHAR(1000) NULL,
                                    created_at %s DEFAULT CURRENT_TIMESTAMP
                                )
                                """, autoInc, dateTimeType, dateTimeType, dateTimeType, dateTimeType));

                jdbcTemplate.execute(String.format("""
                                CREATE TABLE IF NOT EXISTS contract_documents (
                                    id %s,
                                    commitment_id BIGINT NULL,
                                    commitment_version_id BIGINT NULL,
                                    file_id BIGINT NULL,
                                    stored_file_id BIGINT NULL,
                                    file_path VARCHAR(500) NULL,
                                    document_type VARCHAR(50) NULL,
                                    document_status VARCHAR(30) DEFAULT 'GENERATED',
                                    checksum VARCHAR(128) NULL,
                                    generated_at %s DEFAULT CURRENT_TIMESTAMP,
                                    created_at %s DEFAULT CURRENT_TIMESTAMP
                                )
                                """, autoInc, dateTimeType, dateTimeType));

                jdbcTemplate.execute(String.format("""
                                CREATE TABLE IF NOT EXISTS signature_requests (
                                    id %s,
                                    commitment_id BIGINT NULL,
                                    commitment_version_id BIGINT NULL,
                                    requester_id BIGINT NULL,
                                    signer_user_id BIGINT NULL,
                                    provider VARCHAR(50) NULL,
                                    provider_request_id VARCHAR(255) NULL,
                                    signer_email VARCHAR(255) NULL,
                                    external_tx_id VARCHAR(255) NULL,
                                    status VARCHAR(50) DEFAULT 'PENDING',
                                    signature_status VARCHAR(30) DEFAULT 'PENDING',
                                    requested_at %s DEFAULT CURRENT_TIMESTAMP,
                                    expires_at %s NULL,
                                    signed_at %s NULL,
                                    failed_reason TEXT NULL,
                                    created_at %s DEFAULT CURRENT_TIMESTAMP,
                                    updated_at %s NULL
                                )
                                """, autoInc, dateTimeType, dateTimeType, dateTimeType, dateTimeType, dateTimeType));

                jdbcTemplate.execute("""
                                CREATE TABLE IF NOT EXISTS signature_request_documents (
                                    signature_request_id BIGINT NULL,
                                    contract_document_id BIGINT NULL,
                                    document_id BIGINT NULL,
                                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                                )
                                """);

                jdbcTemplate.execute(String.format("""
                                CREATE TABLE IF NOT EXISTS processed_webhook_events (
                                    id %s,
                                    provider VARCHAR(50) NULL,
                                    external_event_id VARCHAR(255) NULL,
                                    event_id VARCHAR(255) NULL,
                                    event_type VARCHAR(100) NULL,
                                    payload_json TEXT NULL,
                                    processing_status VARCHAR(30) DEFAULT 'RECEIVED',
                                    processed_at %s DEFAULT CURRENT_TIMESTAMP,
                                    error_message TEXT NULL,
                                    created_at %s DEFAULT CURRENT_TIMESTAMP
                                )
                                """, autoInc, dateTimeType, dateTimeType));

                jdbcTemplate.execute(String.format("""
                                CREATE TABLE IF NOT EXISTS commitment_change_requests (
                                    id %s,
                                    commitment_id BIGINT NULL,
                                    requested_by BIGINT NULL,
                                    requester_user_id BIGINT NULL,
                                    request_type VARCHAR(50) NULL,
                                    requested_changes_json TEXT NULL,
                                    reason TEXT NULL,
                                    status VARCHAR(50) DEFAULT 'PENDING',
                                    reviewed_by BIGINT NULL,
                                    reviewed_at %s NULL,
                                    decision_reason TEXT NULL,
                                    resulting_version_id BIGINT NULL,
                                    created_at %s DEFAULT CURRENT_TIMESTAMP,
                                    updated_at %s NULL
                                )
                                """, autoInc, dateTimeType, dateTimeType, dateTimeType));

                jdbcTemplate.execute(String.format("""
                                CREATE TABLE IF NOT EXISTS activity_records (
                                    id %s,
                                    application_id BIGINT NULL,
                                    commitment_id BIGINT NULL,
                                    recorded_by BIGINT NULL,
                                    user_id BIGINT NULL,
                                    organization_id BIGINT NULL,
                                    opportunity_id BIGINT NULL,
                                    activity_type VARCHAR(50) NULL,
                                    activity_date DATE NULL,
                                    quantity DECIMAL(15, 2) NULL,
                                    unit VARCHAR(30) NULL,
                                    amount BIGINT NULL,
                                    hours INT DEFAULT 0,
                                    status VARCHAR(50) DEFAULT 'COMPLETED',
                                    description TEXT NULL,
                                    notes TEXT NULL,
                                    evidence_file_id BIGINT NULL,
                                    verification_status VARCHAR(30) DEFAULT 'PENDING',
                                    verified_by BIGINT NULL,
                                    verified_at %s NULL,
                                    rejection_reason TEXT NULL,
                                    created_at %s DEFAULT CURRENT_TIMESTAMP,
                                    updated_at %s NULL
                                )
                                """, autoInc, dateTimeType, dateTimeType, dateTimeType));

                jdbcTemplate.execute(String.format("""
                                CREATE TABLE IF NOT EXISTS community_posts (
                                    id %s,
                                    author_user_id BIGINT NULL,
                                    author_id BIGINT NULL,
                                    organization_id BIGINT NULL,
                                    category VARCHAR(50) NULL,
                                    title VARCHAR(255) NULL,
                                    content TEXT NULL,
                                    visibility VARCHAR(30) DEFAULT 'PUBLIC',
                                    like_count INT DEFAULT 0,
                                    comment_count INT DEFAULT 0,
                                    view_count INT DEFAULT 0,
                                    created_at %s DEFAULT CURRENT_TIMESTAMP,
                                    updated_at %s DEFAULT CURRENT_TIMESTAMP,
                                    is_deleted BOOLEAN DEFAULT FALSE,
                                    deleted_at %s NULL,
                                    deleted_by VARCHAR(255) NULL
                                )
                                """, autoInc, dateTimeType, dateTimeType, dateTimeType));

                jdbcTemplate.execute(String.format("""
                                CREATE TABLE IF NOT EXISTS community_post_images (
                                    id %s,
                                    post_id BIGINT NULL,
                                    file_id BIGINT NULL,
                                    display_order INT DEFAULT 0,
                                    created_at %s DEFAULT CURRENT_TIMESTAMP
                                )
                                """, autoInc, dateTimeType));

                jdbcTemplate.execute(String.format("""
                                CREATE TABLE IF NOT EXISTS community_comments (
                                    id %s,
                                    post_id BIGINT NULL,
                                    author_user_id BIGINT NULL,
                                    author_id BIGINT NULL,
                                    parent_comment_id BIGINT NULL,
                                    content TEXT NULL,
                                    created_at %s DEFAULT CURRENT_TIMESTAMP,
                                    updated_at %s NULL,
                                    is_deleted BOOLEAN DEFAULT FALSE,
                                    deleted_at %s NULL,
                                    deleted_by VARCHAR(255) NULL
                                )
                                """, autoInc, dateTimeType, dateTimeType, dateTimeType));

                jdbcTemplate.execute(String.format("""
                                CREATE TABLE IF NOT EXISTS post_reactions (
                                    id %s,
                                    post_id BIGINT NULL,
                                    user_id BIGINT NULL,
                                    reaction_type VARCHAR(50) DEFAULT 'LIKE',
                                    created_at %s DEFAULT CURRENT_TIMESTAMP
                                )
                                """, autoInc, dateTimeType));

                jdbcTemplate.execute(String.format("""
                                CREATE TABLE IF NOT EXISTS reports (
                                    id %s,
                                    reporter_user_id BIGINT NULL,
                                    target_type VARCHAR(50) NULL,
                                    target_id BIGINT NULL,
                                    reason_code VARCHAR(50) NULL,
                                    reason TEXT NULL,
                                    description TEXT NULL,
                                    status VARCHAR(30) DEFAULT 'PENDING',
                                    handled_by BIGINT NULL,
                                    handled_at %s NULL,
                                    resolution_note TEXT NULL,
                                    created_at %s DEFAULT CURRENT_TIMESTAMP
                                )
                                """, autoInc, dateTimeType, dateTimeType));

                jdbcTemplate.execute(String.format("""
                                CREATE TABLE IF NOT EXISTS admin_audit_logs (
                                    id %s,
                                    actor_user_id BIGINT NULL,
                                    admin_user_id BIGINT NULL,
                                    action_type VARCHAR(100) NULL,
                                    target_type VARCHAR(100) NULL,
                                    target_id VARCHAR(100) NULL,
                                    request_id VARCHAR(100) NULL,
                                    before_json TEXT NULL,
                                    after_json TEXT NULL,
                                    ip_address VARCHAR(45) NULL,
                                    details TEXT NULL,
                                    created_at %s DEFAULT CURRENT_TIMESTAMP
                                )
                                """, autoInc, dateTimeType));

                jdbcTemplate.execute(String.format("""
                                CREATE TABLE IF NOT EXISTS notifications (
                                    id %s,
                                    user_id BIGINT NULL,
                                    notification_type VARCHAR(50) NULL,
                                    title VARCHAR(255) NULL,
                                    content TEXT NULL,
                                    message TEXT NULL,
                                    reference_type VARCHAR(50) NULL,
                                    reference_id BIGINT NULL,
                                    is_read BOOLEAN DEFAULT FALSE,
                                    read_at %s NULL,
                                    created_at %s DEFAULT CURRENT_TIMESTAMP
                                )
                                """, autoInc, dateTimeType, dateTimeType));

                jdbcTemplate.execute(String.format("""
                                CREATE TABLE IF NOT EXISTS ai_consultations (
                                    id %s,
                                    user_id BIGINT NULL,
                                    title VARCHAR(255) NULL,
                                    consultation_status VARCHAR(50) DEFAULT 'IN_PROGRESS',
                                    intent_summary TEXT NULL,
                                    extracted_preferences_json TEXT NULL,
                                    started_at %s DEFAULT CURRENT_TIMESTAMP,
                                    completed_at %s NULL,
                                    ended_at %s NULL,
                                    created_at %s DEFAULT CURRENT_TIMESTAMP,
                                    updated_at %s NULL
                                )
                                """, autoInc, dateTimeType, dateTimeType, dateTimeType, dateTimeType, dateTimeType));

                jdbcTemplate.execute(String.format("""
                                CREATE TABLE IF NOT EXISTS ai_messages (
                                    id %s,
                                    consultation_id BIGINT NULL,
                                    sender_type VARCHAR(50) NULL,
                                    content TEXT NULL,
                                    metadata_json TEXT NULL,
                                    sequence_no INT DEFAULT 1,
                                    created_at %s DEFAULT CURRENT_TIMESTAMP
                                )
                                """, autoInc, dateTimeType));

                jdbcTemplate.execute(String.format("""
                                CREATE TABLE IF NOT EXISTS activity_notes (
                                    id %s,
                                    application_id BIGINT NULL,
                                    user_id BIGINT NULL,
                                    note_text TEXT NULL,
                                    visibility VARCHAR(50) DEFAULT 'PRIVATE',
                                    is_private BOOLEAN DEFAULT FALSE,
                                    created_at %s DEFAULT CURRENT_TIMESTAMP
                                )
                                """, autoInc, dateTimeType));

                jdbcTemplate.execute("""
                                CREATE TABLE IF NOT EXISTS activity_note_files (
                                    activity_note_id BIGINT NULL,
                                    stored_file_id BIGINT NULL,
                                    display_order INT DEFAULT 0
                                )
                                """);

                jdbcTemplate.execute(String.format("""
                                CREATE TABLE IF NOT EXISTS heritage_projects (
                                    id %s,
                                    title VARCHAR(255) NULL,
                                    description TEXT NULL,
                                    target_amount BIGINT NULL,
                                    current_amount BIGINT DEFAULT 0,
                                    created_at %s DEFAULT CURRENT_TIMESTAMP
                                )
                                """, autoInc, dateTimeType));
        }
}
