package handler;

import nginx.clojure.java.StringFacedJavaBodyFilter;
import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class GoogleLogoFilter extends StringFacedJavaBodyFilter {
    // <a id="logo">…</a> 블록만 잡는 패턴
    private static final Pattern ANCHOR_PATTERN = Pattern.compile(
        "(?is)" +
        "(<a\\b[^>]*\\bid=[\"']logo[\"'][^>]*>)" +
        "[\\s\\S]*?" +
        "(</a>)"
    );
    private static final String IMG_TAG =
        "<img src=\"https://jpassets.jobplanet.co.kr/production/uploads/"
      + "company/logo/276416/thumb_276416.JPG\" alt=\"Logo\" />";

    // 요청당 하나씩 생성되므로, 여기서 전체 바디를 모아둡니다.
    private final StringBuilder buffer = new StringBuilder();

    @Override
    protected Object[] doFilter(Map<String,Object> request,
                                String body,
                                boolean isLast) throws IOException {
        // 1) 받은 청크 전부 모아두기
        buffer.append(body);

        // 2) 중간 청크에선 아무 것도 출력하지 않습니다
        if (!isLast) {
            return new Object[]{ null, null, "" };
        }

        String full = buffer.toString();

        // ─── 디버그: id="logo" 위치와 주변 200자씩 찍어보기 ───
        int pos = full.indexOf("id=\"logo\"");
        if (pos >= 0) {
            int start = Math.max(0, pos - 100);
            int end   = Math.min(full.length(), pos + 200);
            System.err.println(">> found id=\"logo\" snippet:\n" +
                full.substring(start, end).replaceAll("\\s+", " ").trim()
            );
        } else {
            System.err.println(">> id=\"logo\" not found in full HTML");
        }
        // ───────────────────────────────────────────────────────

        // 3) 패턴 매칭 & 치환
        Matcher m = ANCHOR_PATTERN.matcher(full);
        String out;
        if (m.find()) {
            System.err.println("매칭 성공: replacement anchor block");
            String replacement = m.group(1) + IMG_TAG + m.group(2);
            out = m.replaceFirst(Matcher.quoteReplacement(replacement));
        } else {
            System.err.println("매칭 실패: <a id=\"logo\"> 블록을 못 찾음");
            out = full;
        }

        // 4) 최종 청크로 전체(수정된) HTML 내보내기
        return new Object[]{ null, null, out };
    }
}
