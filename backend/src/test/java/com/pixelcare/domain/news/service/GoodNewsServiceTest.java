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

        var items = new GoodNewsService("build/test-news-cache").parse("부산", xml);

        assertThat(items).hasSize(1);
        assertThat(items.get(0).title()).isEqualTo("부산 시민봉사단 연탄 나눔");
        assertThat(items.get(0).summary()).isEqualTo("이웃을 위한 따뜻한 나눔 활동");
        assertThat(items.get(0).source()).isEqualTo("픽셀일보");
        assertThat(items.get(0).region()).isEqualTo("부산");
    }

    @Test
    void mapsFreeformProfileRegionToNewsRegion() {
        GoodNewsService service = new GoodNewsService("build/test-news-cache");

        assertThat(service.normalizeRegion("부산")).isEqualTo("부산");
        assertThat(service.normalizeRegion("부산광역시 해운대구")).isEqualTo("부산");
        assertThat(service.normalizeRegion("대구")).isEqualTo("대구");
        assertThat(service.normalizeRegion("충청북도 청주시")).isEqualTo("충북");
        assertThat(service.normalizeRegion("강원특별자치도 춘천시")).isEqualTo("강원");
        // 긴 이름을 먼저 봐야 '세종로'가 세종으로 새지 않는다.
        assertThat(service.normalizeRegion("서울특별시 종로구 세종로")).isEqualTo("서울");
    }

    @Test
    void fallsBackToNationwideWhenRegionIsUnknownOrEmpty() {
        GoodNewsService service = new GoodNewsService("build/test-news-cache");

        assertThat(service.normalizeRegion(null)).isEqualTo("전국");
        assertThat(service.normalizeRegion("  ")).isEqualTo("전국");
        assertThat(service.normalizeRegion("도쿄")).isEqualTo("전국");
    }
}
