package com.pixelcare.domain.news.service;

import com.pixelcare.domain.news.dto.GoodNewsItem;
import com.pixelcare.domain.news.dto.GoodNewsResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GoodNewsService {

    private static final String PROVIDER = "Google News RSS";
    private static final Duration CACHE_TTL = Duration.ofMinutes(15);
    private static final Map<String, String> REGIONS = Map.ofEntries(
            Map.entry("전국", "전국"),
            Map.entry("서울", "서울"), Map.entry("부산", "부산"),
            Map.entry("대구", "대구"), Map.entry("광주", "광주"),
            Map.entry("인천", "인천"), Map.entry("대전", "대전"),
            Map.entry("울산", "울산"), Map.entry("세종", "세종"),
            Map.entry("경기", "경기"), Map.entry("강원", "강원"),
            Map.entry("충북", "충북"), Map.entry("충남", "충남"),
            Map.entry("전북", "전북"), Map.entry("전남", "전남"),
            Map.entry("경북", "경북"), Map.entry("경남", "경남"),
            Map.entry("제주", "제주")
    );

    private final RestClient restClient;
    private final Map<String, CachedFeed> cache = new ConcurrentHashMap<>();

    public GoodNewsService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3_000);
        factory.setReadTimeout(5_000);
        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .defaultHeader("User-Agent", "PixelCare/1.0 (+regional-good-news-reader)")
                .build();
    }

    public GoodNewsResponse getNews(String requestedRegion, int requestedLimit) {
        String region = normalizeRegion(requestedRegion);
        int limit = Math.max(1, Math.min(requestedLimit, 12));
        CachedFeed cached = cache.get(region);
        Instant now = Instant.now();
        if (cached != null && cached.fetchedAt().plus(CACHE_TTL).isAfter(now)) {
            return response(region, cached.items(), cached.fetchedAt(), false, null, limit);
        }

        try {
            List<GoodNewsItem> items = fetch(region);
            CachedFeed fresh = new CachedFeed(items, now);
            cache.put(region, fresh);
            return response(region, items, now, false,
                    items.isEmpty() ? "조건에 맞는 최근 선행 소식이 없습니다." : null, limit);
        } catch (RuntimeException error) {
            if (cached != null) {
                return response(region, cached.items(), cached.fetchedAt(), true,
                        "뉴스 제공처 연결이 지연되어 마지막으로 수집한 소식을 보여드립니다.", limit);
            }
            return response(region, List.of(), now, true,
                    "지역 선행 소식을 불러오지 못했습니다. 잠시 후 다시 시도해주세요.", limit);
        }
    }

    List<GoodNewsItem> parse(String region, String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            NodeList nodes = factory.newDocumentBuilder()
                    .parse(new InputSource(new StringReader(xml)))
                    .getElementsByTagName("item");
            java.util.ArrayList<GoodNewsItem> items = new java.util.ArrayList<>();
            for (int i = 0; i < nodes.getLength(); i++) {
                Element item = (Element) nodes.item(i);
                String title = text(item, "title");
                String link = text(item, "link");
                if (title.isBlank() || link.isBlank()) continue;
                String source = text(item, "source");
                if (source.isBlank()) source = sourceFromTitle(title);
                String cleanTitle = stripSourceSuffix(title, source);
                String summary = cleanHtml(text(item, "description"));
                String publishedAt = parsePublishedAt(text(item, "pubDate"));
                items.add(new GoodNewsItem(
                        hash(link), region, cleanTitle, summary, source, link, publishedAt
                ));
            }
            return items;
        } catch (Exception error) {
            throw new IllegalStateException("RSS 응답을 해석하지 못했습니다.", error);
        }
    }

    private List<GoodNewsItem> fetch(String region) {
        String location = "전국".equals(region) ? "대한민국" : region;
        String query = ("전국".equals(region) ? location : "intitle:" + location)
                + " (봉사 OR 기부 OR 나눔 OR 선행 OR 후원) when:30d";
        String url = "https://news.google.com/rss/search?q="
                + URLEncoder.encode(query, StandardCharsets.UTF_8)
                + "&hl=ko&gl=KR&ceid=KR:ko";
        String xml = restClient.get().uri(url).retrieve().body(String.class);
        if (xml == null || xml.isBlank()) throw new IllegalStateException("빈 RSS 응답입니다.");
        List<GoodNewsItem> parsed = parse(region, xml);
        if ("전국".equals(region)) return parsed;
        return parsed.stream()
                .filter(item -> item.title().contains(region) || item.summary().contains(region))
                .toList();
    }

    private GoodNewsResponse response(
            String region, List<GoodNewsItem> items, Instant updatedAt,
            boolean stale, String message, int limit
    ) {
        return new GoodNewsResponse(
                region, items.stream().limit(limit).toList(), updatedAt, PROVIDER, stale, message
        );
    }

    private String normalizeRegion(String requested) {
        if (requested == null || requested.isBlank()) return "전국";
        String compact = requested.trim()
                .replace("광역시", "")
                .replace("특별시", "")
                .replace("특별자치시", "")
                .replace("특별자치도", "")
                .replace("도", "");
        return REGIONS.getOrDefault(compact, "전국");
    }

    private String text(Element item, String tag) {
        NodeList nodes = item.getElementsByTagName(tag);
        return nodes.getLength() == 0 ? "" : nodes.item(0).getTextContent().trim();
    }

    private String cleanHtml(String value) {
        return value.replaceAll("<[^>]+>", " ")
                .replace("&nbsp;", " ").replace("&amp;", "&")
                .replace("&quot;", "\"").replace("&#39;", "'")
                .replaceAll("\\s+", " ").trim();
    }

    private String sourceFromTitle(String title) {
        int separator = title.lastIndexOf(" - ");
        return separator > 0 ? title.substring(separator + 3).trim() : "언론사";
    }

    private String stripSourceSuffix(String title, String source) {
        String suffix = " - " + source;
        return title.endsWith(suffix) ? title.substring(0, title.length() - suffix.length()) : title;
    }

    private String parsePublishedAt(String value) {
        try {
            return ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toString();
        } catch (Exception ignored) {
            return value;
        }
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 8);
        } catch (Exception error) {
            return Integer.toHexString(value.hashCode());
        }
    }

    private record CachedFeed(List<GoodNewsItem> items, Instant fetchedAt) {}
}
