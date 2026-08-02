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
        seedRecurringCommitments(donorId, organizationId);
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
