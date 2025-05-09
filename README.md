# DRMInside
디알엠인사이드 사전과제

## 웹 서버 구성 이해 문제

### 1단계: Nginx-clojure 웹 서버를 이용한 Google.com의 Reverse Proxy 구성

#### 구성 내용
- Nginx-clojure를 사용하여 Google.com의 reverse proxy를 구성했습니다.
- 설정은 `conf/nginx.conf` 파일에 있습니다.
- 주요 설정:
  ```nginx
  location / {
      proxy_pass https://www.google.com/;
      proxy_ssl_server_name on;
      proxy_ssl_name www.google.com;
      proxy_http_version 1.1;
      proxy_set_header Connection "";

      proxy_set_header Host www.google.com;
      proxy_set_header User-Agent "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36";
      proxy_set_header Referer "https://www.google.com/";
      proxy_set_header Accept "*/*";
      proxy_set_header Accept-Encoding "";

      sub_filter_once off;
      sub_filter 'action="https://www.google.com/search"' 'action="/search"';
      sub_filter 'action="/search"' 'action="/search"';
  }

  location /search {
      proxy_pass https://www.google.com/search;
      # 유사한 설정...
  }
  ```

#### 동작 설명
- 웹 브라우저에서 http://localhost 접속 시 Google 홈페이지로 이동합니다.
- 검색어 입력 시 Google 검색 결과가 표시됩니다.
- `proxy_pass`를 통해 요청을 Google.com으로 전달합니다.
- `proxy_set_header`를 통해 필요한 헤더를 설정합니다.
- `sub_filter`를 통해 Google 검색 폼의 action URL을 로컬 URL로 변경합니다.

### 2단계: Google 로고 이미지 변경 필터 구현

#### 구성 내용
- Java로 작성된 `GoogleLogoFilter` 클래스를 구현했습니다.
- 필터는 `src/handler/GoogleLogoFilter.java` 파일에 있습니다.
- nginx.conf에 필터 적용 설정을 추가했습니다:
  ```nginx
  location /search {
      # 기존 설정...
      body_filter_type 'java';
      body_filter_code 'handler.GoogleLogoFilter';
  }
  ```

#### 동작 방식
- `StringFacedJavaBodyFilter`를 상속받아 HTTP 응답 본문을 필터링합니다.
- 정규식을 사용하여 Google 로고의 SVG 태그를 찾습니다.
- 찾은 SVG 태그를 커스텀 이미지를 가리키는 `<img>` 태그로 대체합니다.
- HTML 콘텐츠에만 필터를 적용하고, 응답의 마지막 청크에서만 처리합니다.
- Content-Length 헤더를 제거하여 chunked 전송 방식을 사용합니다.

### 3단계: JVM 디버깅 설정

#### nginx.conf 수정 내용
```nginx
# JVM 디버깅 활성화
jvm_options "-Xdebug";
jvm_options "-Xrunjdwp:server=y,transport=dt_socket,address=8400,suspend=n";

# Java 패키지 접근 문제 해결을 위한 옵션
jvm_options "--add-opens=java.base/java.lang=ALL-UNNAMED";
jvm_options "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED";
jvm_options "--add-opens=java.base/java.util=ALL-UNNAMED";
jvm_options "--add-exports=java.base/sun.nio.cs=ALL-UNNAMED";
```

#### IDE에서 디버깅하는 방법

##### Eclipse에서 디버깅
1. Eclipse 실행 후 메뉴에서 Run > Debug Configurations 선택
2. Remote Java Application 선택 후 New configuration 생성
3. 설정:
   - Name: Nginx-Clojure Debug
   - Project: 프로젝트 선택
   - Connection Type: Standard (Socket Attach)
   - Host: localhost
   - Port: 8400
4. Apply 후 Debug 버튼 클릭
5. 디버깅하려는 코드에 브레이크포인트 설정
6. Nginx 서버 실행 (`./nginx` 명령어)
7. 브라우저에서 http://localhost 접속하여 디버깅 시작

##### Visual Studio Code에서 디버깅
1. VS Code에서 프로젝트 열기
2. launch.json 파일 생성 또는 편집:
   ```json
   {
     "version": "0.2.0",
     "configurations": [
       {
         "type": "java",
         "name": "Nginx-Clojure Debug",
         "request": "attach",
         "hostName": "localhost",
         "port": 8400
       }
     ]
   }
   ```
3. 디버깅하려는 코드에 브레이크포인트 설정
4. Nginx 서버 실행 (`./nginx` 명령어)
5. VS Code에서 F5 키를 눌러 디버깅 시작
6. 브라우저에서 http://localhost 접속하여 디버깅 시작

## 실행 방법
1. 프로젝트 루트 디렉토리에서 다음 명령어로 Nginx 서버 실행:
   ```
   ./nginx
   ```
2. 웹 브라우저에서 http://localhost 접속
3. 서버 중지:
   ```
   ./nginx -s stop
   ```
