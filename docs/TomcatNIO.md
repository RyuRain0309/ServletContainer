# Tomcat NIO 서버

## Blocking I/O vs Non-blocking I/O

### CustomWebServer (Blocking I/O)

```
클라이언트 연결
      │
  [acceptor]  ── Socket.accept() ──► executor.execute(RequestTask)
                                              │
                                         [worker-N]
                                              │
                                    소켓에서 데이터 읽기 ← 완료될 때까지 블로킹
                                              │
                                         요청 처리
                                              │
                                         응답 전송 ← 완료될 때까지 블로킹
                                              │
                                        스레드 반환
```

**한계:** I/O 대기 중에도 스레드를 점유한다.  
최대 동시 처리 요청 수 = `maxPoolSize` (200).  
I/O가 느린 클라이언트가 있으면 스레드가 낭비된다.

---

### Tomcat NIO (Non-blocking I/O)

```
클라이언트 연결
      │
  [Acceptor]  ── accept() ──► [Poller] (NIO Selector)
                                   │
                          연결을 Selector에 등록
                          (데이터 준비 이벤트 구독)
                                   │
                    데이터 준비 이벤트 발생 시에만
                                   │
                              [Worker 스레드]
                                   │
                               요청 처리
                                   │
                               응답 전송
                                   │
                            연결 Poller에 반환 (keep-alive)
```

**강점:** 스레드는 실제 처리 시간에만 사용된다.  
연결 자체는 `Selector` 하나가 수천 개를 동시에 감시할 수 있다.  
최대 동시 연결 수 = `maxConnections` (8192) — 스레드 수(200)와 분리.

---

## 핵심 구성 요소

### Acceptor 스레드

새 TCP 연결을 `accept()`하고 Poller에 등록.  
연결당 스레드를 할당하지 않고 바로 반환한다.

### Poller 스레드

Java NIO `Selector`를 사용해 등록된 수천 개의 연결을 감시.  
소켓에 읽을 데이터가 생기면(이벤트) Worker에게 전달.  
I/O 대기 시간 동안 CPU를 낭비하지 않는다.

### Worker 스레드 풀

실제 HTTP 파싱, 서블릿 처리, 응답 전송을 담당.  
스레드는 처리 중일 때만 점유되고, 완료 후 Poller로 연결 반환(keep-alive).

---

## 설정 파라미터 (`application-tomcat.yml`)

| 파라미터 | 값 | 대응 개념 (CustomWebServer) |
|---|---|---|
| `threads.max` | 200 | `MAX_POOL_SIZE` |
| `threads.min-spare` | 10 | `CORE_POOL_SIZE` 유사 |
| `accept-count` | 100 | `QUEUE_CAPACITY` |
| `max-connections` | 8192 | 스레드 수와 분리된 연결 허용량 |
| `connection-timeout` | 20000ms | — |

`max-connections`이 `threads.max`보다 훨씬 큰 것이 NIO의 핵심.  
연결은 최대 8192개를 동시에 열 수 있지만, 실제 CPU를 사용하는 스레드는 200개 이하.

---

## 프로필별 서버 전환

```bash
# Custom 서버 (기본)
./gradlew bootRun

# Tomcat NIO 서버
./gradlew bootRun --args='--spring.profiles.active=tomcat'
```

`WebServerConfig`가 `@Profile("!tomcat")`이므로,  
`tomcat` 프로필 활성화 시 `CustomWebServer` 빈이 등록되지 않고  
Spring Boot가 `TomcatServletWebServerFactory`를 자동 구성한다.

---

## 세 가지 서버 비교

| | 싱글 스레드 | ThreadPoolExecutor | Tomcat NIO |
|---|---|---|---|
| I/O 모델 | Blocking | Blocking | Non-blocking |
| 연결 처리 | 순차 | 병렬 (스레드 수 한계) | 병렬 (연결 수 분리) |
| 스레드 수 | 1 | core~max(200) | min-spare~max(200) |
| 최대 동시 연결 | 1 | 200 | 8192 |
| I/O 대기 시 | 스레드 블로킹 | 스레드 블로킹 | Poller가 감시 |
| 구현 복잡도 | 낮음 | 중간 | 높음 (내부 구현) |

---

## 다음 단계: 벤치마크

세 가지 서버 구현을 동일 엔드포인트(`/hello`)로 부하 테스트해 성능 비교.

```bash
# ApacheBench 예시
ab -n 10000 -c 100 http://localhost:8080/hello
```
