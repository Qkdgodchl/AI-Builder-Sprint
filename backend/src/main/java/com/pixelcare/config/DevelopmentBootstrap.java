package com.pixelcare.config;

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
import java.time.LocalDate;
import java.util.List;
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
            @Value("${app.bootstrap.donor-password}") String donorPassword
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.operatorPassword = operatorPassword;
        this.managerPassword = managerPassword;
        this.donorPassword = donorPassword;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Long operatorId = ensureUser(
                "operator@pixelcare.local",
                "픽셀케어 운영진",
                "운영진",
                operatorPassword,
                "OPERATOR"
        );
        Long managerId = ensureUser(
                "manager@pixelcare.demo",
                "데모 센터 관리자",
                "데모 관리자",
                managerPassword,
                "CENTER_MANAGER"
        );
        Long donorId = ensureUser(
                "donor@pixelcare.demo",
                "정기후원 데모 후원자",
                "데모 후원자",
                donorPassword,
                "USER"
        );
        Long organizationId = ensureDemoOrganization(managerId);
        seedOpportunities(managerId, organizationId);
        seedExtraOpportunities(managerId, organizationId);
        ensureVolunteerRequiredDocuments();
        refreshPlaceholderDescriptions();
        seedRecurringCommitments(donorId, organizationId);
        seedCommunityActivity(organizationId);
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
            Long currentAmount
    ) {}

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
                    "부산 유산기부 상담센터", "BUSAN", "부산 연제구 시청 인근", null, null, null)
    );

    private void seedExtraOpportunities(Long managerId, Long organizationId) {
        int offset = 0;
        for (OpportunitySeed seed : EXTRA_OPPORTUNITIES) {
            offset++;
            boolean volunteer = "VOLUNTEER".equals(seed.type());
            // 봉사 시간 집계가 활동 시각에서 나오므로 공고마다 4시간 일정을 부여한다.
            jdbcTemplate.update("""
                    INSERT INTO opportunities (
                        organization_id, opportunity_type, category, title, summary, description,
                        region, location, participation_mode, capacity, target_amount, current_amount,
                        recruitment_end_at, activity_start_at, activity_end_at,
                        status, created_by, published_at
                    )
                    SELECT ?, ?, ?, ?, ?, ?, ?, ?, 'OFFLINE', ?, ?, ?,
                           %s, %s, %s,
                           'PUBLISHED', ?, CURRENT_TIMESTAMP
                    FROM DUAL
                    WHERE NOT EXISTS (SELECT 1 FROM opportunities o WHERE o.title = ?)
                    """.formatted(
                            volunteer ? "DATE_ADD(CURRENT_DATE, INTERVAL ? DAY)" : "NULL",
                            volunteer
                                    ? "DATE_ADD(DATE_ADD(CURRENT_DATE, INTERVAL ? DAY), INTERVAL 9 HOUR)"
                                    : "NULL",
                            volunteer
                                    ? "DATE_ADD(DATE_ADD(CURRENT_DATE, INTERVAL ? DAY), INTERVAL 13 HOUR)"
                                    : "NULL"
                    ),
                    buildExtraOpportunityArgs(seed, managerId, organizationId, offset, volunteer));
        }
    }

    private Object[] buildExtraOpportunityArgs(
            OpportunitySeed seed, Long managerId, Long organizationId, int offset, boolean volunteer
    ) {
        List<Object> args = new java.util.ArrayList<>(List.of(
                organizationId,
                seed.type(),
                seed.category(),
                seed.title(),
                seed.organizer(),
                seed.title() + " 프로그램의 상세 안내입니다."
        ));
        args.add(seed.region());
        args.add(seed.location());
        args.add(seed.capacity());
        args.add(seed.targetAmount());
        args.add(seed.currentAmount() == null ? 0L : seed.currentAmount());
        if (volunteer) {
            args.add(offset + 6);
            args.add(offset + 9);
            args.add(offset + 9);
        }
        args.add(managerId);
        args.add(seed.title());
        return args.toArray();
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
    private static final long[] PLEDGE_AMOUNTS = {30_000L, 50_000L, 100_000L, 150_000L};

    /**
     * 홈 누적 현황과 센터 대시보드가 실제 데이터로 채워지도록 데모 활동 이력을 만든다.
     * 회원 → 봉사 참여(완료) → 기부 약정 → 전자서명 문서 순으로 이어 붙인다.
     */
    private void seedCommunityActivity(Long organizationId) {
        if (userExists("member01@pixelcare.demo")) return;

        // 봉사 시간은 공고의 활동 시각에서 계산하므로 비어 있는 공고를 4시간 일정으로 채운다.
        jdbcTemplate.update("""
                UPDATE opportunities
                SET recruitment_end_at = DATE_ADD(CURRENT_DATE, INTERVAL (id % 20) + 5 DAY),
                    activity_start_at = DATE_ADD(DATE_ADD(CURRENT_DATE, INTERVAL (id % 20) + 8 DAY),
                                                 INTERVAL 9 HOUR),
                    activity_end_at = DATE_ADD(DATE_ADD(CURRENT_DATE, INTERVAL (id % 20) + 8 DAY),
                                               INTERVAL 13 HOUR)
                WHERE organization_id = ? AND opportunity_type = 'VOLUNTEER'
                  AND is_deleted = FALSE AND activity_start_at IS NULL
                """, organizationId);

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
                    "픽셀 이웃 " + suffix,
                    "이웃" + suffix,
                    donorPassword,
                    "USER"
            );
            // 온기를 고르게 흩어 평균이 한쪽으로 쏠리지 않게 한다.
            jdbcTemplate.update("UPDATE users SET temperature = ? WHERE id = ?",
                    36.5 + ((index * 7) % 45), memberId);

            for (int slot = 0; slot < VOLUNTEER_PER_MEMBER; slot++) {
                Long opportunityId = volunteerOpportunities.get(
                        (index + slot * 7) % volunteerOpportunities.size());
                insertApplication(opportunityId, memberId, "COMPLETED");
            }

            if (index % 2 != 0) continue;

            Long opportunityId = donationOpportunities.get(pledgeIndex % donationOpportunities.size());
            long amount = PLEDGE_AMOUNTS[pledgeIndex % PLEDGE_AMOUNTS.length];
            pledgeIndex++;

            Long applicationId = insertApplication(opportunityId, memberId, "APPROVED");
            if (applicationId == null) continue;
            Long commitmentId = insertDonationCommitment(
                    applicationId, opportunityId, memberId, organizationId, amount);

            // 일부는 서명 대기 상태로 남겨 대시보드가 한쪽으로만 보이지 않게 한다.
            boolean signed = signedCount < 11;
            insertClmDocument(commitmentId, opportunityId, memberId, signed);
            if (signed) signedCount++;
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
        if (existing != null && existing > 0) return null;

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO applications (
                        public_id, opportunity_id, applicant_user_id, status,
                        submitted_at, updated_at
                    ) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, UUID.randomUUID().toString());
            statement.setLong(2, opportunityId);
            statement.setLong(3, userId);
            statement.setString(4, status);
            return statement;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    private Long insertDonationCommitment(
            Long applicationId, Long opportunityId, Long userId, Long organizationId, long amount
    ) {
        String title = "픽셀케어 기부 약정 " + money(amount) + "원";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO commitments (
                        public_id, application_id, opportunity_id, user_id, organization_id,
                        current_version_no, commitment_status, title, commitment_type,
                        pledge_amount, pledge_frequency, effective_from, signed_at
                    ) VALUES (?, ?, ?, ?, ?, 1, 'ACTIVE', ?, 'DONATION', ?, 'ONE_TIME',
                              CURRENT_DATE, CURRENT_TIMESTAMP)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, UUID.randomUUID().toString());
            statement.setLong(2, applicationId);
            statement.setLong(3, opportunityId);
            statement.setLong(4, userId);
            statement.setLong(5, organizationId);
            statement.setString(6, title);
            statement.setLong(7, amount);
            return statement;
        }, keyHolder);
        Long commitmentId = keyHolder.getKey().longValue();

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
                userId
        );
        return commitmentId;
    }

    /** 데모용 전자서명 문서. 실제 모두싸인 문서가 아니므로 식별자에 demo 접두어를 둔다. */
    private void insertClmDocument(
            Long commitmentId, Long opportunityId, Long userId, boolean signed
    ) {
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
                opportunityId
        );
    }

    private static String money(long amount) {
        return java.text.NumberFormat.getNumberInstance(java.util.Locale.KOREA).format(amount);
    }

    private Long ensureUser(
            String email,
            String nickname,
            String name,
            String password,
            String role
    ) {
        List<Long> existing = jdbcTemplate.query(
                "SELECT id FROM users WHERE email = ?",
                (rs, rowNum) -> rs.getLong("id"),
                email
        );
        Long userId;
        if (existing.isEmpty()) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO users (
                            email, password_hash, nickname, name, role,
                            account_status, privacy_consent_at
                        ) VALUES (?, ?, ?, ?, ?, 'ACTIVE', CURRENT_TIMESTAMP)
                        """, Statement.RETURN_GENERATED_KEYS);
                statement.setString(1, email);
                statement.setString(2, passwordEncoder.encode(password));
                statement.setString(3, nickname);
                statement.setString(4, name);
                statement.setString(5, role);
                return statement;
            }, keyHolder);
            userId = keyHolder.getKey().longValue();
        } else {
            userId = existing.get(0);
        }
        jdbcTemplate.update("""
                INSERT INTO user_roles (user_id, role)
                SELECT ?, ?
                WHERE NOT EXISTS (
                    SELECT 1 FROM user_roles WHERE user_id = ? AND role = ?
                )
                """, userId, role, userId, role);
        jdbcTemplate.update(
                "UPDATE users SET role = ?, password_hash = COALESCE(password_hash, ?) WHERE id = ?",
                role,
                passwordEncoder.encode(password),
                userId
        );
        return userId;
    }

    private Long ensureDemoOrganization(Long managerId) {
        List<Long> existing = jdbcTemplate.query("""
                SELECT o.id
                FROM organizations o
                JOIN organization_managers om ON om.organization_id = o.id
                WHERE om.user_id = ? AND o.name = '픽셀케어 데모 센터'
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
                        '픽셀케어 데모 센터', 'SOCIAL_WELFARE',
                        '데모 관리자', '051-000-0000', 'manager@pixelcare.demo',
                        '부산광역시 금정구', '로컬 개발용 데모 센터입니다.', 'VERIFIED'
                    )
                    """, Statement.RETURN_GENERATED_KEYS);
            return statement;
        }, keyHolder);
        Long organizationId = keyHolder.getKey().longValue();
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
        if (opportunityIds.size() < 2) return;

        ensureRecurringCommitment(
                donorId, organizationId, opportunityIds.get(0),
                "매월 이어가는 아동 결식 예방 정기후원 약정",
                "MONTHLY", new BigDecimal("30000"), LocalDate.now().minusDays(3)
        );
        ensureRecurringCommitment(
                donorId, organizationId, opportunityIds.get(1),
                "매년 이어가는 지역 문화유산 보존 후원 약정",
                "ANNUAL", new BigDecimal("120000"), LocalDate.now().plusDays(12)
        );
    }

    private void ensureRecurringCommitment(
            Long donorId,
            Long organizationId,
            Long opportunityId,
            String title,
            String frequency,
            BigDecimal amount,
            LocalDate renewalDueAt
    ) {
        Integer existing = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM commitments
                WHERE user_id = ? AND opportunity_id = ?
                """, Integer.class, donorId, opportunityId);
        if (existing != null && existing > 0) return;

        KeyHolder applicationKey = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO applications (
                        public_id, opportunity_id, applicant_user_id, status, submitted_at
                    ) VALUES (?, ?, ?, 'APPROVED', CURRENT_TIMESTAMP)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, UUID.randomUUID().toString());
            statement.setLong(2, opportunityId);
            statement.setLong(3, donorId);
            return statement;
        }, applicationKey);
        Long applicationId = applicationKey.getKey().longValue();

        LocalDate effectiveFrom = "MONTHLY".equals(frequency)
                ? renewalDueAt.minusMonths(1)
                : renewalDueAt.minusYears(1);

        KeyHolder commitmentKey = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO commitments (
                        public_id, application_id, opportunity_id, user_id, organization_id,
                        current_version_no, commitment_status, title, commitment_type,
                        pledge_amount, pledge_frequency, renewal_due_at,
                        effective_from, signed_at
                    ) VALUES (?, ?, ?, ?, ?, 1, 'ACTIVE', ?, 'DONATION', ?, ?, ?, ?, CURRENT_TIMESTAMP)
                    """, Statement.RETURN_GENERATED_KEYS);
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
        Long commitmentId = commitmentKey.getKey().longValue();

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
                renewalDueAt
        );
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
                donorId
        );
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
}
