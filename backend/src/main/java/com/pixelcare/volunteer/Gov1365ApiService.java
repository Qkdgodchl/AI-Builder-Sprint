package com.pixelcare.volunteer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Service
public class Gov1365ApiService {

    private static final Logger log = Logger.getLogger(Gov1365ApiService.class.getName());

    @Value("${gov1365.api.end-point:https://apis.data.go.kr/1741000/VolunteerPartcptnService}")
    private String apiEndPoint;

    @Value("${gov1365.api.service-key-encoding:2vU2n5a8v48w9dstMuQB5IruvSqfyFOc5s4oR3wBpSBARe6OpcKWPkT2BaftUfCd1ASYG0BXvpznf8KeNUaidw%3D%3D}")
    private String serviceKeyEncoding;

    private final RestTemplate restTemplate = new RestTemplate();

    public List<VolunteerResponseDto> fetch1365Volunteers() {
        List<VolunteerResponseDto> resultList = new ArrayList<>();
        String reqUrl = apiEndPoint + "/getVolsPrtcpList?serviceKey=" + serviceKeyEncoding + "&numOfRows=20&pageNo=1&schSido=6260000";

        try {
            log.info("Requesting 1365 Public Open API: " + reqUrl);
            String xmlResponse = restTemplate.getForObject(reqUrl, String.class);

            if (xmlResponse != null && xmlResponse.contains("<item>")) {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(new ByteArrayInputStream(xmlResponse.getBytes(StandardCharsets.UTF_8)));

                NodeList items = doc.getElementsByTagName("item");
                for (int i = 0; i < items.getLength(); i++) {
                    Element itemEl = (Element) items.item(i);

                    String progrmRegistNo = getXmlTagValue(itemEl, "progrmRegistNo");
                    String progrmSj = getXmlTagValue(itemEl, "progrmSj");
                    String nanmmbyNm = getXmlTagValue(itemEl, "nanmmbyNm");
                    String actPlace = getXmlTagValue(itemEl, "actPlace");
                    String srvcClCode = getXmlTagValue(itemEl, "srvcClCode");

                    String detailLink = (progrmRegistNo != null && !progrmRegistNo.isBlank())
                            ? "https://www.1365.go.kr/vols/1365/act/volsDetail.do?progrmRegistNo=" + progrmRegistNo
                            : "https://www.1365.go.kr/vols/1365/act/volsList.do";

                    resultList.add(VolunteerResponseDto.builder()
                            .id((long) (1000 + i))
                            .title(progrmSj != null ? "🤝 " + progrmSj : "🤝 1365 봉사 미션")
                            .category("VOLUNTEER")
                            .location(actPlace != null && !actPlace.isBlank() ? actPlace : "부산광역시 봉사 장소")
                            .organizer(nanmmbyNm != null && !nanmmbyNm.isBlank() ? nanmmbyNm : "1365 자원봉사센터")
                            .tags(List.of("1365 공공데이터", srvcClCode != null ? srvcClCode : "봉사참여"))
                            .link1365(detailLink)
                            .build());
                }
            }
        } catch (Exception e) {
            log.warning("1365 Open API Gateway connection info: " + e.getMessage());
        }

        return resultList;
    }

    private String getXmlTagValue(Element element, String tagName) {
        NodeList nodeList = element.getElementsByTagName(tagName);
        if (nodeList != null && nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent();
        }
        return "";
    }
}
