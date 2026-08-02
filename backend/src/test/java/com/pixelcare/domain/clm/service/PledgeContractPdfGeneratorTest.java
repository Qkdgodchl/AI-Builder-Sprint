package com.pixelcare.domain.clm.service;

import com.pixelcare.domain.ai.dto.PledgeIntent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PledgeContractPdfGeneratorTest {

    @Test
    void rendersPledgeContractPdf() {
        PledgeIntent intent = new PledgeIntent(
                "HOMETOWN_DONATION", "잇다 데모 센터", new BigDecimal("30000"), "MONTHLY",
                null, "부산 사하구", null, null, null, null,
                null, null, null, null, null, null
        );

        byte[] pdf = new PledgeContractPdfGenerator().generate(
                intent, List.of(), "전동훈", "donor@pixelcare.demo",
                "부산 사하구 고향사랑기부 (감천마을 재생)", "잇다 데모 센터"
        );

        // 한글 폰트 로딩이나 서명란 레이아웃이 깨지면 렌더링 단계에서 바로 터진다.
        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    }
}
