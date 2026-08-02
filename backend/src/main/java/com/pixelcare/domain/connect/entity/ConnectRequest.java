package com.pixelcare.domain.connect.entity;

import com.pixelcare.global.entity.BaseTimeEntity;
import jakarta.persistence.*;

import java.util.UUID;

/**
 * 하고 싶은 선행이 있는데 등록된 프로그램이 없을 때 남기는 요청.
 *
 * AI 추천이 빈손으로 끝나면 그 말이 여기로 넘어와, 관련 센터가 보고
 * 프로그램을 새로 열 수 있게 한다. 시민의 수요와 센터의 공급을 잇는 자리다.
 */
@Entity
@Table(name = "connect_requests")
public class ConnectRequest extends BaseTimeEntity {

    /** 아직 센터가 보지 않은 상태. */
    public static final String OPEN = "OPEN";
    /** 센터가 맡아 검토 중. */
    public static final String REVIEWING = "REVIEWING";
    /** 실제 프로그램으로 열려 요청이 해소됨. */
    public static final String FULFILLED = "FULFILLED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    private String publicId = UUID.randomUUID().toString();

    @Column(name = "requester_user_id", nullable = false)
    private Long requesterUserId;

    @Column(name = "requester_nickname", nullable = false)
    private String requesterNickname;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** VOLUNTEER 또는 DONATION. */
    @Column(nullable = false, length = 30)
    private String category = "VOLUNTEER";

    @Column(length = 60)
    private String region;

    /** AI 추천이 비어 넘어온 요청인지, 직접 쓴 요청인지. */
    @Column(nullable = false, length = 20)
    private String origin = "DIRECT";

    @Column(nullable = false, length = 20)
    private String status = OPEN;

    @Column(name = "support_count", nullable = false)
    private int supportCount = 0;

    @Column(name = "handled_organization_id")
    private Long handledOrganizationId;

    @Column(name = "handled_organization_name")
    private String handledOrganizationName;

    @Column(name = "handled_opportunity_id")
    private Long handledOpportunityId;

    @Column(name = "handled_message", columnDefinition = "TEXT")
    private String handledMessage;

    /**
     * 두 센터가 같은 요청을 동시에 맡으려 하면 나중 것이 앞 것을 덮어쓴다.
     * 버전을 두어 뒤늦은 쪽이 실패하도록 한다.
     *
     * 값이 비어 있으면 Hibernate가 그 행을 갱신하지 못하므로 0으로 시작한다.
     * (이 열이 생기기 전에 만들어진 행은 0으로 채워 줘야 한다.)
     */
    @Version
    @Column(nullable = false)
    private Long version = 0L;

    protected ConnectRequest() {
    }

    public ConnectRequest(Long requesterUserId, String requesterNickname, String title,
                          String content, String category, String region, String origin) {
        this.requesterUserId = requesterUserId;
        this.requesterNickname = requesterNickname;
        this.title = title;
        this.content = content;
        this.category = category;
        this.region = region;
        this.origin = origin;
    }

    /** 센터가 요청을 맡는다. 아직 프로그램이 열린 것은 아니다. */
    public void markReviewing(Long organizationId, String organizationName, String message) {
        this.status = REVIEWING;
        this.handledOrganizationId = organizationId;
        this.handledOrganizationName = organizationName;
        this.handledMessage = message;
    }

    /** 실제 프로그램이 열려 요청이 해소된다. */
    public void markFulfilled(Long opportunityId, String message) {
        this.status = FULFILLED;
        this.handledOpportunityId = opportunityId;
        if (message != null && !message.isBlank()) this.handledMessage = message;
    }

    public void changeSupportCount(int delta) {
        this.supportCount = Math.max(0, this.supportCount + delta);
    }

    public Long getId() { return id; }
    public String getPublicId() { return publicId; }
    public Long getRequesterUserId() { return requesterUserId; }
    public String getRequesterNickname() { return requesterNickname; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getCategory() { return category; }
    public String getRegion() { return region; }
    public String getOrigin() { return origin; }
    public String getStatus() { return status; }
    public int getSupportCount() { return supportCount; }
    public Long getHandledOrganizationId() { return handledOrganizationId; }
    public String getHandledOrganizationName() { return handledOrganizationName; }
    public Long getHandledOpportunityId() { return handledOpportunityId; }
    public String getHandledMessage() { return handledMessage; }
}
