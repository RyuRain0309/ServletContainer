# Thread Pool

## 싱글 스레드의 문제

기존 싱글 스레드 서버는 `ServerSocket.accept()` → `handleRequest()` → `accept()` 순서로 동작한다.  
즉, 요청 하나가 완전히 처리될 때까지 다음 연결을 받지 못한다.

```
accept → [요청 A 처리] → accept → [요청 B 처리] → ...
           (블로킹)                  (블로킹)
```

동시 사용자가 많아지면 앞 요청이 끝날 때까지 대기가 누적된다.

---

## ThreadPoolExecutor 구조

```
accept() ──► executor.execute(RequestTask) ──► worker-1 ─► handleRequest()
          ├──────────────────────────────────► worker-2 ─► handleRequest()
          └──────────────────────────────────► worker-3 ─► handleRequest()
                                                ...
```

`acceptor` 스레드는 연결 수락만 담당하고, 실제 처리는 스레드 풀의 워커에게 위임한다.  
덕분에 `acceptor`는 즉시 다음 연결로 넘어갈 수 있다.

---

## ThreadPoolExecutor 구성 요소

| 파라미터 | 값 | 설명 |
|---|---|---|
| `corePoolSize` | CPU 코어 수 | 항상 살아있는 스레드 수 |
| `maximumPoolSize` | 200 | 최대 동시 스레드 수 (Tomcat 기본값과 동일) |
| `keepAliveTime` | 60초 | 코어 초과 스레드가 유휴 상태로 대기하는 시간 |
| `workQueue` | `LinkedBlockingQueue(100)` | 스레드가 모두 바쁠 때 요청을 대기시키는 큐 |

### 스레드 생성 규칙

1. 활성 스레드 수 < `corePoolSize` → 새 스레드 생성
2. 활성 스레드 수 >= `corePoolSize` → 큐에 추가
3. 큐가 가득 참 + 활성 스레드 수 < `maxPoolSize` → 새 스레드 생성
4. 큐가 가득 참 + 활성 스레드 수 == `maxPoolSize` → **거부 정책** 실행

---

## 거부 정책 (RejectedExecutionHandler)

큐와 스레드 풀이 모두 포화 상태일 때 새 요청을 어떻게 처리할지 결정한다.

| 정책 | 설명 |
|---|---|
| `AbortPolicy` (기본) | `RejectedExecutionException` 발생 |
| `CallerRunsPolicy` | accept 스레드가 직접 처리 (백프레셔) |
| `DiscardPolicy` | 조용히 폐기 |
| **커스텀** | 클라이언트에 503 반환 ← 이 프로젝트에서 사용 |

```
큐 포화 → rejectRequest() → 소켓에 직접 "HTTP/1.1 503 Service Unavailable" 응답
```

---

## acceptor vs worker 스레드

| | acceptor | worker |
|---|---|---|
| 역할 | `accept()` 루프만 실행 | `handleRequest()` 실행 |
| 개수 | 1개 고정 | corePoolSize ~ maxPoolSize |
| daemon | `false` (JVM 종료 방지) | `true` (acceptor 종료 시 자동 종료) |

---

## stop() — Graceful Shutdown

```
stop() 호출
  │
  ├─ serverSocket.close()       ← 새 연결 차단
  │
  └─ executor.shutdown()        ← 진행 중인 요청은 마저 처리
       │
       └─ awaitTermination(30s) ← 최대 30초 대기 후 강제 종료
```

---

## 로그 예시

```
[worker-1] [GET] /hello (active=3, queued=0)
[worker-2] [GET] /hello (active=3, queued=0)
[worker-3] [GET] /hello (active=3, queued=1)
```

- `active`: 현재 요청을 처리 중인 스레드 수
- `queued`: 큐에서 대기 중인 요청 수

---

## 다음 단계: Tomcat NIO Connector와 비교

Tomcat은 Java NIO를 사용하는 `NioEndpoint`를 통해 스레드 수 대비 훨씬 많은 연결을 처리한다.

| | 이 구현 | Tomcat NIO |
|---|---|---|
| I/O 모델 | 블로킹 (1 요청 = 1 스레드) | 비동기 논블로킹 (Poller → Worker) |
| 연결당 스레드 | 요청 처리 동안 점유 | Poller가 이벤트 감지, Worker는 처리 시에만 점유 |
| 최대 동시 연결 | maxPoolSize(200) | `maxConnections`(기본 8192) |
