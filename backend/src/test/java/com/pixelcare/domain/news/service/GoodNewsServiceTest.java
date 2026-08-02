package com.pixelcare.domain.news.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GoodNewsServiceTest {

    @Test
    void parsesRegionalGoodNewsWithoutHtmlOrSourceSuffix() {
        String xml = """
                <rss><channel><item>
                  <title>부산 시민봉사단 연탄 나눔 - 픽셀일보</title>
                  <link>https://example.com/news/1</link>
                  <description><![CDATA[<b>이웃을 위한</b> 따뜻한 나눔 활동]]></description>
                  <source>픽셀일보</source>
                  <pubDate>Sat, 01 Aug 2026 03:00:00 GMT</pubDate>
                </item></channel></rss>
                """;

        var items = new GoodNewsService().parse("부산", xml);

        assertThat(items).hasSize(1);
        assertThat(items.get(0).title()).isEqualTo("부산 시민봉사단 연탄 나눔");
        assertThat(items.get(0).summary()).isEqualTo("이웃을 위한 따뜻한 나눔 활동");
        assertThat(items.get(0).source()).isEqualTo("픽셀일보");
        assertThat(items.get(0).region()).isEqualTo("부산");
    }
}
