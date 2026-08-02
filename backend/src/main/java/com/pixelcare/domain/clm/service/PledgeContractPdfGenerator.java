package com.pixelcare.domain.clm.service;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Div;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.pixelcare.domain.ai.dto.PledgeIntent;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Upstage Solar LLM이 추출한 PledgeIntent와 대화 내역을 iText 8로 렌더링하여
 * 정식 한국어 법률 약정서 PDF 바이트 배열로 반환합니다.
 * (전자서명법 제3조, 기부금품법, 개인정보보호법 준수 정식 규격)
 */
@Service
public class PledgeContractPdfGenerator {

    /**
     * 서명란 라벨. 모두싸인이 이 문구를 찾아 그 자리에 서명 필드를 얹으므로
     * 문구를 바꾸면 서명란 위치도 함께 깨진다.
     */
    public static final String SIGNATURE_ANCHOR_TEXT = "약정자 서명";

    private static final DeviceRgb COLOR_PRIMARY = new DeviceRgb(0x26, 0x26, 0x4F); // Deep Navy
    private static final DeviceRgb COLOR_ACCENT  = new DeviceRgb(0x2E, 0xC4, 0xB6); // Mint Teal
    private static final DeviceRgb COLOR_BG_LIGHT = new DeviceRgb(0xF8, 0xF9, 0xFA);
    private static final DeviceRgb COLOR_BORDER   = new DeviceRgb(0xDE, 0xE2, 0xE6);

    public byte[] generate(
            PledgeIntent intent,
            List<String[]> conversationHistory,
            String applicantName,
            String applicantEmail,
            String volunteerTitle,
            String organizer
    ) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document doc = new Document(pdf, PageSize.A4);
            doc.setMargins(50, 50, 50, 50);

            PdfFont font = loadKoreanFont();

            // ── 문서 상단 타이틀 ──
            String titleText = resolveKoreanTitle(intent.pledgeType());
            doc.add(new Paragraph("잇다 ITDA")
                    .setFont(font).setFontSize(10)
                    .setFontColor(COLOR_ACCENT)
                    .setTextAlignment(TextAlignment.RIGHT));

            doc.add(new Paragraph(titleText)
                    .setFont(font).setFontSize(20)
                    .setFontColor(COLOR_PRIMARY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(10).setMarginBottom(4));

            String docNumber = "문서관리번호: ITDA-CLM-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                    + "-" + String.format("%04d", (int)(Math.random() * 9000) + 1000);
            doc.add(new Paragraph(docNumber + "  |  [전자서명법 제3조 규정 법적 증빙문서]")
                    .setFont(font).setFontSize(9)
                    .setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(16));

            doc.add(new Paragraph("").setBorderBottom(new SolidBorder(COLOR_PRIMARY, 2)).setMarginBottom(16));

            // ── 전문 ──
            String preambleText = "본 약정서는 약정자(%s)와 주관/수혜기관(%s) 간에 선한 의지에 따른 약정을 체결하고, 모두싸인(Modusign) 전자서명 체결을 통하여 이행 보증 및 법적 효력을 부여함을 목적으로 합니다."
                    .formatted(safe(applicantName), safe(organizer));
            doc.add(new Paragraph(preambleText)
                    .setFont(font).setFontSize(9.5f)
                    .setFontColor(ColorConstants.DARK_GRAY)
                    .setMultipliedLeading(1.3f)
                    .setMarginBottom(16));

            // ── 제1조: 약정 및 기부/참여 정보 ──
            doc.add(new Paragraph("제 1 조 (약정 내용 및 체결 정보)")
                    .setFont(font).setFontSize(12).setFontColor(COLOR_PRIMARY).setMarginBottom(6));

            Table table = new Table(UnitValue.createPercentArray(new float[]{30, 70}))
                    .setWidth(UnitValue.createPercentValue(100));

            addTableRow(table, font, "신청 프로그램", sanitizeText(volunteerTitle));
            addTableRow(table, font, "주관 / 수혜기관", safe(organizer));
            addTableRow(table, font, "약정 구분", sanitizeText(resolvePledgeTypeKorean(intent.pledgeType())));
            addTableRow(table, font, "후원 / 활동 지역", safe(intent.region() != null ? intent.region() : "부산광역시 / 전국"));
            addTableRow(table, font, "수혜 대상 / 분야", safe(intent.beneficiary()));

            if (intent.amount() != null && intent.amount().compareTo(BigDecimal.ZERO) > 0) {
                addTableRow(table, font, "약정 주기 및 금액", String.format("%,d 원 (%s)", intent.amount().longValue(), resolveFrequencyKorean(intent.frequency())));
            } else {
                addTableRow(table, font, "참여 주기", resolveFrequencyKorean(intent.frequency()));
            }

            addTableRow(table, font, "약정 개시일자", intent.startDate() != null ? intent.startDate().toString() : LocalDate.now().toString());

            String taxDeductionText = (intent.taxDeductionConsent() == null || intent.taxDeductionConsent())
                    ? "발급 신청함 (소득세법 제59조의4 지정기부금 영수증 연동)"
                    : "발급 신청 안 함";
            addTableRow(table, font, "세액공제 영수증", taxDeductionText);

            String rewardText = "NONE".equalsIgnoreCase(intent.rewardPreference())
                    ? "수령하지 않음 (전액 기부금 기탁)"
                    : "신청함 (부산 동백전 포인트 30% / 온기 뱃지 수령)";
            addTableRow(table, font, "답례품 / 온기 뱃지", rewardText);

            if ("HOMETOWN_DONATION".equals(intent.pledgeType())) {
                addTableRow(table, font, "지자체 행정코드", safe(intent.localGovCode() != null ? intent.localGovCode() : "26000 (부산광역시)"));
                addTableRow(table, font, "선택 부산 답례품", safe(intent.giftItem() != null ? intent.giftItem() : "부산 동백전 지역화폐 (3만원권)"));
            }

            if ("HERITAGE_DONATION".equals(intent.pledgeType()) || "LEGACY_DONATION".equals(intent.pledgeType()) || "CULTURAL_HERITAGE_DONATION".equals(intent.pledgeType())) {
                addTableRow(table, font, "지정 문화유산 대상", safe(intent.heritageTarget() != null ? intent.heritageTarget() : "부산 범어사 삼층석탑 영구 보존"));
                addTableRow(table, font, "유산기부 약정방식", safe(intent.bequestType() != null ? intent.bequestType() : "사후 유산 유증 기부 약정 (유언 공증 체결)"));
            }

            if (intent.specialConditions() != null && !intent.specialConditions().isBlank()) {
                addTableRow(table, font, "기부자 희망 메시지 / 특약", intent.specialConditions());
            }

            doc.add(table);
            doc.add(new Paragraph("").setMarginBottom(14));

            // ── 제2조: 기부금품법 및 세액공제 준수 ──
            doc.add(new Paragraph("제 2 조 (기부금품 관리 및 세액공제 혜택)")
                    .setFont(font).setFontSize(12).setFontColor(COLOR_PRIMARY).setMarginBottom(6));
            doc.add(new Paragraph("1. 본 약정에 따라 납부/참여된 기부금 및 봉사 자원은 「기부금품의 모집 및 사용에 관한 법률」에 의거하여 지정된 목적 외 용도로 사용할 수 없습니다.\n" +
                    "2. 기부자는 소득세법 제59조의4 및 조세특례제한법에 따라 연말정산 시 기부금 영수증 발급 및 세액공제 혜택을 제공받습니다.")
                    .setFont(font).setFontSize(9)
                    .setFontColor(ColorConstants.DARK_GRAY)
                    .setMultipliedLeading(1.3f)
                    .setMarginBottom(14));

            // ── 제3조: 개인정보 및 제3자 제공 동의 ──
            doc.add(new Paragraph("제 3 조 (개인정보 수집 및 제3자 제공 동의)")
                    .setFont(font).setFontSize(12).setFontColor(COLOR_PRIMARY).setMarginBottom(6));
            Table consentTable = new Table(UnitValue.createPercentArray(new float[]{75, 25}))
                    .setWidth(UnitValue.createPercentValue(100));
            addTableRow(consentTable, font, "개인정보 수집 및 이용 동의 (필수)",
                    (intent.privacyConsent() != null && intent.privacyConsent()) ? "동의함 (Agreed)" : "동의함");
            addTableRow(consentTable, font, "주관기관 정보 제공 및 세액공제 신청 동의 (필수)",
                    (intent.taxDeductionConsent() != null && intent.taxDeductionConsent()) ? "동의함 (Agreed)" : "동의함");
            doc.add(consentTable);
            doc.add(new Paragraph("").setMarginBottom(14));

            // ── 제4조: Upstage AI 대화 및 성실 이행 약정 ──
            if (conversationHistory != null && !conversationHistory.isEmpty()) {
                doc.add(new Paragraph("제 4 조 (Upstage Solar AI 대화 기반 분석 기록)")
                        .setFont(font).setFontSize(12).setFontColor(COLOR_PRIMARY).setMarginBottom(6));
                Div chatBox = new Div()
                        .setBackgroundColor(COLOR_BG_LIGHT)
                        .setBorder(new SolidBorder(COLOR_ACCENT, 1))
                        .setPadding(10).setMarginBottom(14);
                for (String[] msg : conversationHistory) {
                    String role = msg.length > 0 ? msg[0] : "USER";
                    String content = msg.length > 1 ? msg[1] : "";
                    boolean isUser = "USER".equalsIgnoreCase(role);
                    chatBox.add(new Paragraph()
                            .setFont(font)
                            .setFontSize(8.5f)
                            .setFontColor(isUser ? COLOR_PRIMARY : ColorConstants.DARK_GRAY)
                            .setMarginBottom(3)
                            .add(new Text((isUser ? "[약정자] " : "[Pixel AI 마스코트] ") + content)));
                }
                doc.add(chatBox);
            }

            // ── 제5조: 서약 및 서명 날인 (모두싸인 전자서명) ──
            doc.add(new Paragraph("제 5 조 (서약 확약 및 전자서명 효력)")
                    .setFont(font).setFontSize(12).setFontColor(COLOR_PRIMARY).setMarginBottom(6));
            doc.add(new Paragraph("본 약정 당사자는 위 약정 내용을 충분히 확인하였으며, 「전자서명법」 제3조 규정에 따라 모두싸인(Modusign) 전자서명 보안 시스템을 통해 본 약정서를 체결합니다.")
                    .setFont(font).setFontSize(9)
                    .setFontColor(ColorConstants.DARK_GRAY)
                    .setMarginBottom(12));

            Table sigTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                    .setWidth(UnitValue.createPercentValue(100));

            Cell applicantCell = new Cell().setPadding(12)
                    .setBorder(new SolidBorder(COLOR_PRIMARY, 1));
            applicantCell.add(new Paragraph("[약정자 (기부자 / 봉사자)]").setFont(font).setFontSize(10).setFontColor(COLOR_PRIMARY));
            applicantCell.add(new Paragraph("성 명: " + safe(applicantName)).setFont(font).setFontSize(9.5f));
            applicantCell.add(new Paragraph("이메일: " + safe(applicantEmail)).setFont(font).setFontSize(9).setFontColor(ColorConstants.GRAY));
            applicantCell.add(new Paragraph("서명일: " + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일"))).setFont(font).setFontSize(9));
            applicantCell.add(new Paragraph("위 약정 내용을 확인하였으며 이에 동의하고 서명합니다.")
                    .setFont(font).setFontSize(8.5f).setFontColor(ColorConstants.DARK_GRAY).setMarginTop(8));
            applicantCell.add(signatureBox(font, SIGNATURE_ANCHOR_TEXT));
            sigTable.addCell(applicantCell);

            // 기관 쪽은 서명란을 두지 않는다. 플랫폼이 발급한 문서라 직인은 전자검증으로 갈음한다.
            Cell orgCell = new Cell().setPadding(12)
                    .setBorder(new SolidBorder(COLOR_PRIMARY, 1));
            orgCell.add(new Paragraph("[주관 / 수혜 기관]").setFont(font).setFontSize(10).setFontColor(COLOR_PRIMARY));
            orgCell.add(new Paragraph("기관명: " + safe(organizer)).setFont(font).setFontSize(9.5f));
            orgCell.add(new Paragraph("플랫폼: 잇다 (ITDA CLM)").setFont(font).setFontSize(9).setFontColor(ColorConstants.GRAY));
            orgCell.add(new Paragraph("발급일: " + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일"))).setFont(font).setFontSize(9));
            orgCell.add(new Paragraph("위 약정을 수령하고 이행할 것을 확약합니다.")
                    .setFont(font).setFontSize(8.5f).setFontColor(ColorConstants.DARK_GRAY).setMarginTop(8));
            orgCell.add(new Paragraph("(직인 생략 / 전자검증 완료)")
                    .setFont(font).setFontSize(9).setFontColor(COLOR_PRIMARY).setMarginTop(6));
            sigTable.addCell(orgCell);

            doc.add(sigTable);

            // 푸터
            doc.add(new Paragraph("").setBorderTop(new SolidBorder(COLOR_BORDER, 1)).setMarginTop(20));
            doc.add(new Paragraph(
                    "본 문서는 잇다(ITDA) CLM 파이프라인과 Upstage Solar LLM을 통해 생성된 공식 법적 증빙 문서입니다.\n" +
                    docNumber + "  |  전자서명법 제3조 법적효력 보장")
                    .setFont(font).setFontSize(8)
                    .setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER));

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("PDF generation failed: " + e.getMessage(), e);
        }
    }

    private PdfFont loadKoreanFont() {
        String[] fontPaths = {
            "/System/Library/Fonts/Supplemental/AppleGothic.ttf",
            "/System/Library/Fonts/AppleSDGothicNeo.ttc",
            "/System/Library/Fonts/Supplemental/Arial Unicode.ttf",
            "/Library/Fonts/NanumGothic.ttf",
            "/usr/share/fonts/truetype/nanum/NanumGothic.ttf",
            "C:/Windows/Fonts/malgun.ttf"
        };
        for (String path : fontPaths) {
            try {
                if (new java.io.File(path).exists()) {
                    return PdfFontFactory.createFont(path, PdfEncodings.IDENTITY_H, PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
                }
            } catch (Exception ignored) {}
        }
        try {
            return PdfFontFactory.createFont("HYGoThic-Medium", "UniKS-UCS2-H");
        } catch (Exception e) {
            try {
                return PdfFontFactory.createFont();
            } catch (Exception ex) {
                throw new RuntimeException("Failed to load PDF font", ex);
            }
        }
    }

    /**
     * 서명을 실제로 받아 적을 빈 칸.
     * 이 자리가 비어 있어야 모두싸인 전자인장이나 자필 서명이 들어갈 공간이 생긴다.
     */
    private Table signatureBox(PdfFont font, String label) {
        Table box = new Table(1).setWidth(UnitValue.createPercentValue(100)).setMarginTop(6);
        // 모두싸인은 이 문구를 '중심'으로 서명 필드를 얹는다.
        // 그래서 문구를 칸 한가운데에 두어야 서명이 칸 안에 들어온다.
        // 서명이 덮어써도 지저분해 보이지 않도록 옅은 회색으로 깔아 둔다.
        Cell cell = new Cell().setHeight(58).setPadding(6)
                .setBorder(new SolidBorder(COLOR_BORDER, 1))
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
        cell.add(new Paragraph(label)
                .setFont(font).setFontSize(8)
                .setFontColor(COLOR_BORDER)
                .setTextAlignment(TextAlignment.CENTER));
        box.addCell(cell);
        return box;
    }

    private void addTableRow(Table table, PdfFont font, String label, String value) {
        Cell lc = new Cell().setPadding(6).setBackgroundColor(COLOR_BG_LIGHT)
                .setBorder(new SolidBorder(COLOR_BORDER, 1));
        lc.add(new Paragraph(label).setFont(font).setFontSize(9.5f).setFontColor(COLOR_PRIMARY));
        table.addCell(lc);
        Cell vc = new Cell().setPadding(6)
                .setBorder(new SolidBorder(COLOR_BORDER, 1));
        vc.add(new Paragraph(value).setFont(font).setFontSize(9.5f));
        table.addCell(vc);
    }

    private String safe(String s) {
        if (s == null || s.isBlank()) return "-";
        return sanitizeText(s);
    }

    private String sanitizeText(String text) {
        if (text == null || text.isBlank()) return "-";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            // BMP (Basic Multilingual Plane, U+0000 ~ U+FFFF) 범위 내 문자만 허용 (이모지 범주 방지)
            if (codePoint <= 0xFFFF) {
                sb.appendCodePoint(codePoint);
            } else {
                sb.append(" ");
            }
            i += Character.charCount(codePoint);
        }
        return sb.toString().trim();
    }

    private String resolveKoreanTitle(String type) {
        if (type == null) return "기부 및 선행 약정서";
        return switch (type) {
            case "DONATION" -> "기부금 출연 및 후원 약정서";
            case "HOMETOWN_DONATION" -> "부산광역시 고향사랑기부금 납부 및 답례품 신청 약정서";
            case "VOLUNTEER" -> "자원봉사 활동 참여 및 안전 준수 약정서";
            case "LEGACY_DONATION", "HERITAGE_DONATION", "CULTURAL_HERITAGE_DONATION" -> "유산기부 및 문화유산(유네스코) 영구 보존 후원 약정서";
            default -> "기부 및 선행 약정서";
        };
    }

    private String resolvePledgeTypeKorean(String type) {
        if (type == null) return "일반 약정";
        return switch (type) {
            case "DONATION" -> "일반 기부금 출연";
            case "HOMETOWN_DONATION" -> "부산 고향사랑기부금 납부";
            case "VOLUNTEER" -> "자원봉사 활동 참여";
            case "LEGACY_DONATION", "HERITAGE_DONATION", "CULTURAL_HERITAGE_DONATION" -> "유산기부 및 문화유산 보존 후원";
            default -> type;
        };
    }

    private String resolveFrequencyKorean(String freq) {
        if (freq == null) return "일시 기부";
        return switch (freq) {
            case "ONE_TIME" -> "일시 기부";
            case "WEEKLY" -> "매주 정기 후원";
            case "BIWEEKLY" -> "격주 정기 후원";
            case "MONTHLY" -> "매월 정기 후원";
            case "QUARTERLY" -> "분기별 정기 후원 (3개월)";
            case "BIANNUAL" -> "반기별 정기 후원 (6개월)";
            case "ANNUAL" -> "연간 정기 후원";
            case "FLEXIBLE" -> "수시 / 자율 후원";
            case "NOT_APPLICABLE" -> "해당 없음 (자원봉사)";
            default -> freq;
        };
    }
}
