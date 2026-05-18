# Custom Servlet Container

Spring Boot 위에 직접 구현한 서블릿 컨테이너 학습 프로젝트.

HTTP 파싱부터 필터 체인, 서블릿 라이프사이클까지 직접 구현하고,  
**싱글 스레드 → ThreadPoolExecutor → Tomcat NIO** 순으로 서버를 전환하며  
각 구현의 성능 차이를 측정하는 것이 최종 목표입니다.

---

## 요청 처리 흐름

```
TCP 연결
  └─ [acceptor 스레드]  ServerSocket.accept()
       └─ executor.execute(RequestTask)
            └─ [worker-N]  handleRequest(socket)
                 ├─ CustomHttpServletRequest  (HTTP 직접 파싱)
                 ├─ CustomHttpServletResponse (ByteArrayOutputStream 버퍼링)
                 ├─ CustomFilterChain
                 │    ├─ CharacterEncodingFilter
                 │    ├─ FormContentFilter
                 │    ├─ RequestContextFilter
                 │    └─ DispatcherServlet.service()
                 │         └─ HandlerMapping → @RestController
                 └─ response.flushBuffer() → HTTP 응답 전송
```

---

## 서버 구현 3종

| 프로필 | 서버 | I/O 방식 | 스레드 |
|---|---|---|---|
| `single` | CustomWebServer | Blocking | 1개 고정 |
| 기본 | CustomWebServer | Blocking | ThreadPoolExecutor (core=12, max=200) |
| `tomcat` | Tomcat NIO | Non-blocking | Poller + Worker Pool (max=200) |

---

## 실행 방법

```bash
# 싱글 스레드
./gradlew bootRun --args='--spring.profiles.active=single'

# ThreadPoolExecutor (기본)
./gradlew bootRun

# Tomcat NIO
./gradlew bootRun --args='--spring.profiles.active=tomcat'
```

### 엔드포인트

| 경로 | 설명 |
|---|---|
| `GET /` | 정적 메인 페이지 |
| `GET /index.html` | 정적 HTML |
| `GET /hello?name=` | JSON 응답 |
| `GET /page?name=` | 동적 HTML |

---

## 벤치마크

### 환경

| 항목 | 값 |
|---|---|
| 머신 | Apple M4 Pro (Performance 8 + Efficiency 4 cores) |
| RAM | 24 GB |
| JVM | OpenJDK 21 |
| 도구 | ApacheBench 2.3 |
| 조건 | `-n 5000 -c 50` (총 5,000 요청, 동시 50 연결) |
| 엔드포인트 | `GET /hello` (in-memory JSON 응답) |

### 결과

| 지표 | Single Thread | ThreadPoolExecutor | Tomcat NIO |
|---|---:|---:|---:|
| **Requests/sec** | 9,460 | 12,266 | 16,848 |
| **Mean latency** | 5.29 ms | 4.08 ms | 2.97 ms |
| **p50 latency** | 4 ms | 3 ms | 2 ms |
| **p95 latency** | 13 ms | 5 ms | 5 ms |
| **p99 latency** | 22 ms | 8 ms | 15 ms |
| **Failed requests** | 0 | 0 | 0 |
| **단일 스레드 대비** | — | +29.6% | +78.1% |

### 분석

**Single Thread vs ThreadPoolExecutor (+29.6%)**

싱글 스레드는 요청이 직렬 처리되므로, 동시 50 연결 중 1개만 실제로 처리된다.  
나머지 49개는 큐에서 대기한다.  
ThreadPoolExecutor는 12개 이상의 코어가 병렬 처리하므로 처리량이 높아진다.

**ThreadPoolExecutor vs Tomcat NIO (+37.3%)**

`/hello`는 순수 CPU 처리 작업(DB 없음, 네트워크 지연 없음)이라  
NIO의 비동기 I/O 대기 이점이 완전히 드러나지 않는 조건이다.  
실제 서비스처럼 DB 조회나 외부 API 호출이 있으면 차이가 더 커진다.

Tomcat NIO의 차별점은 `max-connections=8192` — 스레드 200개가  
동시에 최대 8,192개 연결을 처리할 수 있다.  
I/O 대기 중 스레드를 점유하지 않기 때문이다.

> 이 벤치마크는 JVM 워밍업 없이 측정된 결과이므로,  
> 실제 성능 비교 시에는 워밍업 이후 측정을 권장합니다.

---

## 프로젝트 구조

```
src/main/java/io/github/ryurain0309/
├── server/
│   ├── ServerApplication.java
│   ├── HelloController.java           — /hello (JSON), /page (동적 HTML), / (redirect)
│   ├── config/
│   │   └── WebServerConfig.java       — @Profile("!tomcat") CustomWebServer 등록
│   ├── container/
│   │   ├── CustomWebServer.java       — acceptor + ThreadPoolExecutor
│   │   ├── CustomServletConfig.java
│   │   ├── CustomFilterConfig.java
│   │   └── CustomFilterChain.java
│   └── http/
│       ├── CustomHttpServletRequest.java   — HTTP 직접 파싱
│       └── CustomHttpServletResponse.java  — 버퍼링 응답
└── servletcontainer/
    ├── CustomServletContext.java           — 서블릿/필터 레지스트리
    ├── CustomServletRegistration.java
    ├── CustomFilterRegistration.java
    └── CustomRequestDispatcher.java        — 정적 리소스 forward

src/main/resources/
├── application.yml
├── application-single.yml    — pool.max=1 (싱글 스레드)
├── application-tomcat.yml    — Tomcat NIO 설정
└── static/index.html
```

---

## 기술 스택

- Java 21
- Spring Boot 4.0.3
- Jakarta Servlet 6.1
- Tomcat 11 (tomcat 프로필)
- Lombok
