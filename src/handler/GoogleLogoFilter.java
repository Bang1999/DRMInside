package handler;

import nginx.clojure.java.NginxJavaBodyFilter;

import java.io.*;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GoogleLogoFilter implements NginxJavaBodyFilter {

    private static final String CUSTOM_LOGO_URL =
            "https://jpassets.jobplanet.co.kr/production/uploads/company/logo/276416/thumb_276416.JPG";

    // 정규식: <img ... src="..." ...> 형태에서 src 값 바꾸기
    private static final Pattern LOGO_IMG_PATTERN =
            Pattern.compile("(<a[^>]*id=\"logo\"[^>]*>.*?<img[^>]*?src=\")(.*?)(\"[^>]*?>)", Pattern.DOTALL);

    @Override
    public Object[] doFilter(Map<String, Object> request,
                             InputStream inputStream,
                             boolean isLastPart) throws IOException {

        if (!isLastPart) {
            return new Object[]{inputStream};
        }

        // 1. 바디 읽기
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            baos.write(buffer, 0, len);
        }

        String html = baos.toString("UTF-8");

        // 2. 정규식으로 <img src="..."> 교체
        Matcher matcher = LOGO_IMG_PATTERN.matcher(html);
        String modifiedHtml;

        if (matcher.find()) {
            modifiedHtml = matcher.replaceFirst(matcher.group(1) + CUSTOM_LOGO_URL + matcher.group(3));
            System.err.println("===== REPLACED LOGO IMG WITH REGEX =====");
        } else {
            modifiedHtml = html;
            System.err.println("NO <a id=\"logo\"> with <img src=...> FOUND.");
        }

        InputStream modifiedStream = new ByteArrayInputStream(modifiedHtml.getBytes("UTF-8"));
        return new Object[]{modifiedStream};
    }
}
