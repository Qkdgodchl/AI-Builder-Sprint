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
    // 구글 뉴스 RSS는 짧은 시간에 반복 호출하면 결과를 거의 돌려주지 않는다.
    // 캐시를 길게 잡아 외부 호출 자체를 줄인다.
    private static final Duration CACHE_TTL = Duration.ofHours(6);
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

    /** 프로필 지역은 자유 입력이라 정식 명칭이 그대로 들어온다. 짧은 표기로 되돌린다. */
    private static final Map<String, String> REGION_ALIASES = Map.ofEntries(
            Map.entry("서울특별시", "서울"), Map.entry("부산광역시", "부산"),
            Map.entry("대구광역시", "대구"), Map.entry("광주광역시", "광주"),
            Map.entry("인천광역시", "인천"), Map.entry("대전광역시", "대전"),
            Map.entry("울산광역시", "울산"), Map.entry("세종특별자치시", "세종"),
            Map.entry("경기도", "경기"),
            Map.entry("강원특별자치도", "강원"), Map.entry("강원도", "강원"),
            Map.entry("충청북도", "충북"), Map.entry("충청남도", "충남"),
            Map.entry("전북특별자치도", "전북"), Map.entry("전라북도", "전북"),
            Map.entry("전라남도", "전남"),
            Map.entry("경상북도", "경북"), Map.entry("경상남도", "경남"),
            Map.entry("제주특별자치도", "제주"), Map.entry("제주도", "제주")
    );

    /**
     * "부산광역시 해운대구"처럼 시·군·구가 붙어 와도 지역을 찾아야 한다.
     * 긴 이름을 먼저 보아야 "서울특별시 종로구 세종로"가 세종으로 빠지지 않는다.
     */
    private static final List<String> REGION_LOOKUP_ORDER =
            java.util.stream.Stream.concat(REGION_ALIASES.keySet().stream(), REGIONS.keySet().stream())
                    .sorted(java.util.Comparator.comparingInt(String::length).reversed())
                    .toList();

    private final RestClient restClient;
    private final Map<String, CachedFeed> cache = new ConcurrentHashMap<>();
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper =
            new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules();
    private final java.nio.file.Path snapshotDir;

    public GoodNewsService(
            @org.springframework.beans.factory.annotation.Value("${app.storage.path:storage}")
            String storagePath
    ) {
        this.snapshotDir = java.nio.file.Path.of(storagePath)
                .toAbsolutePath().normalize().resolve("news-cache");
        restoreSnapshots();

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        // 배포 서버에서 구글 뉴스 응답이 100KB를 넘고 국내에서 받을 때보다 느리다.
        // 3초·5초로는 정상 응답도 중간에 끊겨 지역 소식이 매번 비어 보인다.
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(15_000);
        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .defaultHeader("User-Agent",
                        "Mozilla/5.0 (compatible; ItdaBot/1.0; +https://itdafront.vercel.app)")
                .defaultHeader("Accept", "application/rss+xml, application/xml;q=0.9, */*;q=0.8")
                .defaultHeader("Accept-Language", "ko-KR,ko;q=0.9")
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

            if (items.isEmpty()) {
                if (cached != null && !cached.items().isEmpty()) {
                    // 제공처가 일시적으로 빈 결과를 주면 마지막 정상 수집분을 유지한다.
                    return response(region, cached.items(), cached.fetchedAt(), true,
                            "뉴스 제공처가 잠시 응답하지 않아 마지막으로 수집한 소식을 보여드립니다.", limit);
                }
                // 빈 결과는 캐시에 넣지 않는다. 넣으면 TTL이 끝날 때까지 재시도조차 하지 못한다.
                return response(region, List.of(), now, false,
                        "조건에 맞는 최근 선행 소식이 없습니다.", limit);
            }

            CachedFeed fresh = new CachedFeed(items, now);
            cache.put(region, fresh);
            saveSnapshot(region, fresh);
            return response(region, items, now, false, null, limit);
        } catch (RuntimeException error) {
            // 어떤 단계에서 막혔는지 남긴다. 원인 없이 "불러오지 못했습니다"만 보면
            // 차단인지, 지연인지, 응답 형식 문제인지 가릴 수가 없다.
            Throwable cause = error.getCause() != null ? error.getCause() : error;
            System.err.println("지역 선행 소식 수집 실패 [" + region + "] "
                    + error.getClass().getSimpleName() + ": " + error.getMessage()
                    + " / 원인 " + cause.getClass().getSimpleName() + ": " + cause.getMessage());
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

    /** 제목에 하나라도 있어야 선행 소식으로 본다. */
    private static final List<String> GOOD_KEYWORDS = List.of(
            "기부", "성금", "기탁", "후원", "나눔", "봉사", "자원봉사", "선행", "온정",
            "장학금", "무료급식", "연탄", "헌혈", "모금", "선한", "미담", "돕", "전달");

    /** 하나라도 있으면 제외한다. 같은 검색어에 사건·사고 기사가 섞여 들어온다. */
    private static final List<String> BAD_KEYWORDS = List.of(
            "사고", "사망", "숨져", "숨진", "부상", "화재", "참사", "실종", "피해",
            "횡령", "비리", "논란", "의혹", "구속", "체포", "기소", "징역", "실형",
            "고발", "사기", "갈등", "반발", "폐지", "삭감", "적발", "수사", "재판",
            "소송", "파산", "분쟁", "학대", "폭행", "성추행", "마약", "음주운전");

    private List<GoodNewsItem> fetch(String region) {
        return fetchFromGoogle(region).stream()
                .filter(GoodNewsService::isGoodNews)
                .filter(item -> matchesRegion(region, item))
                .toList();
    }

    private List<GoodNewsItem> fetchFromGoogle(String region) {
        // URI 객체로 넘긴다. 문자열로 주면 RestClient가 이미 인코딩된 %20을 %2520으로
        // 다시 인코딩해, 검색어가 깨진 채 빈 결과만 돌아온다.
        String xml = restClient.get()
                .uri(java.net.URI.create(buildSearchUrl(region)))
                .retrieve()
                .body(String.class);
        if (xml == null || xml.isBlank()) throw new IllegalStateException("빈 RSS 응답입니다.");
        return parse(region, xml);
    }

    /**
     * intitle: 제약은 지역 기사를 거의 걸러내 결과가 비어버린다.
     * 지역명을 일반 검색어로 넣고, 사건·사고 단어는 검색 단계에서 먼저 제외한다.
     */
    private String buildSearchUrl(String region) {
        String subject = "(기부 OR 성금 OR 기탁 OR 후원 OR 나눔 OR 봉사 OR 선행 OR 온정 OR 모금)";
        String exclude = " -사고 -사망 -숨져 -화재 -횡령 -비리 -구속 -기소 -징역 -사기 -학대 -폭행";
        String query = ("전국".equals(region) ? "" : region + " ") + subject + exclude + " when:30d";
        // URLEncoder는 공백을 '+'로 바꾸는데, 구글 뉴스는 이를 검색어의 일부로 읽어
        // 질의가 통째로 어긋난다. 공백은 %20으로 넣어야 지역·주제 조건이 살아난다.
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8).replace("+", "%20");
        return "https://news.google.com/rss/search?q=" + encoded + "&hl=ko&gl=KR&ceid=KR:ko";
    }

    /** 검색어만으로는 부정 기사가 남아 제목을 한 번 더 본다. */
    private static boolean isGoodNews(GoodNewsItem item) {
        String title = item.title();
        if (BAD_KEYWORDS.stream().anyMatch(title::contains)) return false;
        return GOOD_KEYWORDS.stream().anyMatch(title::contains);
    }

    private static boolean matchesRegion(String region, GoodNewsItem item) {
        if ("전국".equals(region)) return true;
        return item.title().contains(region) || item.summary().contains(region);
    }

    /** 재시작 후에도 마지막 수집분을 보여줄 수 있도록 디스크에 남긴다. */
    private void saveSnapshot(String region, CachedFeed feed) {
        if (feed.items().isEmpty()) return;
        try {
            java.nio.file.Files.createDirectories(snapshotDir);
            objectMapper.writeValue(snapshotDir.resolve(snapshotName(region)).toFile(), feed.items());
        } catch (Exception ignored) {
            // 스냅샷 저장 실패는 화면에 영향을 주지 않는다.
        }
    }

    private void restoreSnapshots() {
        for (String region : REGIONS.keySet()) {
            java.nio.file.Path file = snapshotDir.resolve(snapshotName(region));
            if (!java.nio.file.Files.isRegularFile(file)) continue;
            try {
                List<GoodNewsItem> items = objectMapper.readValue(
                        file.toFile(),
                        objectMapper.getTypeFactory()
                                .constructCollectionType(List.class, GoodNewsItem.class));
                if (!items.isEmpty()) {
                    // 오래된 스냅샷이므로 즉시 갱신을 시도하도록 만료된 시각으로 넣는다.
                    cache.put(region, new CachedFeed(items, Instant.EPOCH));
                }
            } catch (Exception ignored) {
                // 손상된 스냅샷은 무시하고 새로 수집한다.
            }
        }
    }

    private String snapshotName(String region) {
        return java.net.URLEncoder.encode(region, StandardCharsets.UTF_8) + ".json";
    }

    private GoodNewsResponse response(
            String region, List<GoodNewsItem> items, Instant updatedAt,
            boolean stale, String message, int limit
    ) {
        return new GoodNewsResponse(
                region, items.stream().limit(limit).toList(), updatedAt,
                PROVIDER, stale, message
        );
    }

    String normalizeRegion(String requested) {
        if (requested == null || requested.isBlank()) return "전국";
        String value = requested.trim();
        for (String name : REGION_LOOKUP_ORDER) {
            if (value.contains(name)) {
                return REGION_ALIASES.getOrDefault(name, REGIONS.getOrDefault(name, "전국"));
            }
        }
        // 아는 지역이 없으면 전국 소식으로 대신한다.
        return "전국";
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
