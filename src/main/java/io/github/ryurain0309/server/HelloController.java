package io.github.ryurain0309.server;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
public class HelloController {

    @GetMapping("/hello")
    public Map<String, String> hello(@RequestParam(defaultValue = "World") String name) {
        return Map.of(
                "message", "Hello, " + name + "!",
                "container", "CustomServletContainer/1.0"
        );
    }

    @GetMapping(value = "/page", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> page(@RequestParam(defaultValue = "방문자") String name) {
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // CSS에 % 문자(100% 등)가 있어 String.formatted() 사용 불가 → replace()로 동적 값 삽입
        String html = """
                <!DOCTYPE html>
                <html lang="ko">
                <head>
                    <meta charset="UTF-8">
                    <title>동적 페이지</title>
                    <style>
                        body { font-family: 'Segoe UI', sans-serif; background: #f0f2f5;
                               display: flex; align-items: center; justify-content: center;
                               min-height: 100vh; margin: 0; }
                        .card { background: #fff; border-radius: 12px; padding: 40px 48px;
                                box-shadow: 0 4px 20px rgba(0,0,0,0.08); max-width: 480px; width: 100%; }
                        h1 { font-size: 1.5rem; margin-bottom: 16px; }
                        .tag { background: #818cf8; color: #fff; font-size: 0.72rem; font-weight: 700;
                               padding: 2px 10px; border-radius: 99px; vertical-align: middle; margin-left: 8px; }
                        .info { background: #f8fafc; border-radius: 8px; padding: 16px 20px;
                                margin-top: 20px; font-size: 0.9rem; line-height: 1.8; }
                        .label { color: #888; font-size: 0.78rem; }
                        a { color: #6366f1; text-decoration: none; }
                        a:hover { text-decoration: underline; }
                    </style>
                </head>
                <body>
                <div class="card">
                    <h1>안녕하세요, {{NAME}}님! <span class="tag">동적 HTML</span></h1>
                    <p>이 페이지는 <code>@RestController</code>에서 생성된 동적 HTML 응답입니다.</p>
                    <div class="info">
                        <div><span class="label">서버 시각</span><br>{{NOW}}</div>
                        <br>
                        <div><span class="label">컨테이너</span><br>CustomServletContainer/1.0</div>
                    </div>
                    <p style="margin-top: 20px; font-size: 0.85rem;">
                        <a href="/">홈으로 돌아가기</a> &nbsp;|&nbsp;
                        <a href="/hello">JSON API</a>
                    </p>
                </div>
                </body>
                </html>
                """
                .replace("{{NAME}}", name)
                .replace("{{NOW}}", now);

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }
}
