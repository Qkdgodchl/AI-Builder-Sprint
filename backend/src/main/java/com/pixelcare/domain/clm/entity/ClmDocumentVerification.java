package com.pixelcare.domain.clm.entity;

import com.pixelcare.global.entity.BaseTimeEntity;
import jakarta.persistence.*;

/**
 * 체결된 약정서 파일을 되읽어 약정 원본과 맞춰 본 결과.
 *
 * 서명본에 적힌 내용과 우리가 보관한 약정 값이 같은지 확인해,
 * 나중에 다툼이 생겼을 때 "합의한 조건 그대로 서명됐다"를 보일 수 있게 남긴다.
 */
@Entity
@Table(name = "clm_document_verifications")
public class ClmDocumentVerification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "clm_document_id", nullable = false)
    private Long clmDocumentId;

    /** MATCHED: 모두 일치, MISMATCHED: 다른 항목 있음, UNREADABLE: 문서를 읽지 못함 */
    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "checked_count", nullable = false)
    private int checkedCount;

    @Column(name = "matched_count", nullable = false)
    private int matchedCount;

    /** 항목별 대조 결과 JSON. 화면이 그대로 펼쳐 보여준다. */
    @Column(name = "detail_json", columnDefinition = "TEXT")
    private String detailJson;

    /** 서명본에서 뽑아낸 원문 일부. 근거로 보여준다. */
    @Column(name = "parsed_excerpt", columnDefinition = "TEXT")
    private String parsedExcerpt;

    @Column(name = "source_file_type", length = 40)
    private String sourceFileType;

    @Column(length = 40)
    private String provider;

    protected ClmDocumentVerification() {}

    public ClmDocumentVerification(Long clmDocumentId, String status, int checkedCount, int matchedCount,
                                   String detailJson, String parsedExcerpt, String sourceFileType, String provider) {
        this.clmDocumentId = clmDocumentId;
        this.status = status;
        this.checkedCount = checkedCount;
        this.matchedCount = matchedCount;
        this.detailJson = detailJson;
        this.parsedExcerpt = parsedExcerpt;
        this.sourceFileType = sourceFileType;
        this.provider = provider;
    }

    public Long getId() { return id; }
    public Long getClmDocumentId() { return clmDocumentId; }
    public String getStatus() { return status; }
    public int getCheckedCount() { return checkedCount; }
    public int getMatchedCount() { return matchedCount; }
    public String getDetailJson() { return detailJson; }
    public String getParsedExcerpt() { return parsedExcerpt; }
    public String getSourceFileType() { return sourceFileType; }
    public String getProvider() { return provider; }
}
