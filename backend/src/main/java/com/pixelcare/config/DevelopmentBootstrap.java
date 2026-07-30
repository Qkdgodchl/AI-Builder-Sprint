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

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Component
@ConditionalOnProperty(name = "app.bootstrap.enabled", havingValue = "true")
public class DevelopmentBootstrap implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;
    private final String operatorPassword;
    private final String managerPassword;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public DevelopmentBootstrap(
            JdbcTemplate jdbcTemplate,
            @Value("${app.bootstrap.operator-password}") String operatorPassword,
            @Value("${app.bootstrap.manager-password}") String managerPassword
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.operatorPassword = operatorPassword;
        this.managerPassword = managerPassword;
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
        Long organizationId = ensureDemoOrganization(managerId);
        seedOpportunities(managerId, organizationId);
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

    private void seedOpportunities(Long managerId, Long organizationId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM opportunities",
                Integer.class
        );
        if (count != null && count > 0) {
            return;
        }
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
                       CASE WHEN v.location LIKE '부산%' THEN 'BUSAN' ELSE 'NATIONWIDE' END,
                       v.location,
                       'OFFLINE',
                       CASE WHEN v.category = 'VOLUNTEER' THEN 30 ELSE NULL END,
                       v.target_amount,
                       v.current_amount,
                       'PUBLISHED',
                       ?,
                       CURRENT_TIMESTAMP
                FROM volunteers v
                """, organizationId, managerId);
        jdbcTemplate.update("""
                INSERT INTO opportunity_required_documents (
                    opportunity_id, document_code, document_name, description,
                    is_required, display_order
                )
                SELECT id, 'PARTICIPATION_PLEDGE', '참여 약정서',
                       '프로그램 참여 조건과 준수사항을 확인합니다.', TRUE, 0
                FROM opportunities
                WHERE opportunity_type = 'VOLUNTEER'
                """);
    }
}
