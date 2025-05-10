package handler;

import nginx.clojure.java.StringFacedJavaBodyFilter;
import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class GoogleLogoFilter extends StringFacedJavaBodyFilter {
    // <a id="logo">…</a> 블록만 찾기 위한 정규식 (DOTALL 및 대소문자 무시)
    private static final Pattern ANCHOR_PATTERN = Pattern.compile(
        "(?is)" +                                   // DOTALL + IGNORE_CASE 플래그
        "(<a\\b[^>]*\\bid=[\"']logo[\"'][^>]*>)" +  // id="logo" 앵커 시작 태그
        "[\\s\\S]*?" +                              // 중간 콘텐츠(예: SVG)를 최소 매칭
        "(</a>)"                                    // 앵커 닫는 태그
    );

    // 대체할 <img> 태그 (커스텀 로고 이미지 URL)
    private static final String IMG_TAG =
        "<img src=\"https://jpassets.jobplanet.co.kr/production/uploads/"
        + "company/logo/276416/thumb_276416.JPG\" alt=\"Logo\""
        + " width=\"100\" height=\"50\" />";

    // 여러 바디 청크를 누적할 버퍼
    private final StringBuilder buffer = new StringBuilder();

    @Override
    protected Object[] doFilter(Map<String, Object> request, String chunk, boolean isLast) throws IOException {
        // 1) 들어오는 청크를 버퍼에 추가
        buffer.append(chunk);

        // 2) 마지막 청크가 아니면 빈 문자열을 반환하여 버퍼링 유지
        if (!isLast) {
            return new Object[]{ null, null, "" };
        }

        // 3) 모든 청크를 결합한 전체 HTML
        String fullBody = buffer.toString();

        // 4) 정규식으로 앵커 블록 매칭 및 치환
        Matcher matcher = ANCHOR_PATTERN.matcher(fullBody);
        String modifiedBody;
        if (matcher.find()) {
            // 그룹1(<a>), IMG_TAG, 그룹2(</a>) 형태로 교체
            String replacement = matcher.group(1) + IMG_TAG + matcher.group(2);
            modifiedBody = matcher.replaceFirst(Matcher.quoteReplacement(replacement));
        } else {
            // 매칭 실패 시 원본 그대로 사용
            modifiedBody = fullBody;
        }

        // 5) Content-Length 헤더 제거하여 청크 전송 유지
        @SuppressWarnings("unchecked")
        Map<String, String> headersOut = (Map<String, String>) request.get("headers_out");
        if (headersOut != null) {
            headersOut.remove("Content-Length");
        }

        // 6) 최종 수정된 바디를 반환
        return new Object[]{ null, null, modifiedBody };
    }
}