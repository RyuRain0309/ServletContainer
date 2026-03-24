# Servlet Context

## Servlet Context

> `ServletContext`는 웹 애플리케이션 전역에서 공유되는 실행 환경 객체.
>
> 애플리케이션 초기화 파라미터, 전역 속성(Attribute), 리소스 조회, Servlet/Filter 등록, 세션/인코딩 정책 같은 컨테이너 설정.
>
> 즉, 요청 1건 단위가 아니라 애플리케이션 전체 생명주기에서 공통으로 사용하는 컨텍스트.

---

## Servlet Context Override Method

### `getContextPath()`

- **Description**: 웹 애플리케이션의 컨텍스트 경로(global prefix)를 반환.
- **Returns**: 컨텍스트 경로 문자열 반환.(루트 애플리케이션인 경우 빈 문자열).

### `getContext(String uripath)`

- **Description**: 주어진 URI 경로에 해당하는 다른 웹 애플리케이션의 `ServletContext`를 조회.
- **Parameters**: `uripath` - 조회할 웹 애플리케이션의 URI 경로(컨텍스트 경로).
- **Returns**: 매핑된 `ServletContext`가 있으면 반환하고, 없으면 `null`을 반환.

#### Legacy Tech

- **과거**:
    - 자원 효율을 위해 하나의 Servlet Container(이하 WAS) 에서 여러 웹 애플리케이션(WAR)을 호스팅하는 경우가 많음.
    - 이때 애플리케이션 간에 리소스 공유나 통신이 필요할 때 `getContext()`를 활용하여 다른 애플리케이션의 컨텍스트에 접근하곤 했음.
- **현대**:
    - SpringBoot와 같은 내장 WAS 기반의 실행형 JAR 배포가 표준이 되면서 **1 내장 WAS 1 애플리케이션** 구조가 일반화됨.
    - 애플리케이션 간의 격리 및 호스팅 책임이 WAS에서 Docker와 같은 가상화/컨테이너 기술로 완전히 이동.
    - 보안상의 이유로 애플리케이션 간의 직접적인 컨텍스트 접근이 제한되는 경우가 많아졌음.

### 버전 관리

#### `getMajorVersion()` &  `getMinorVersion()`

- **Description**: 지원하는 Servlet API 메이저 버전과 마이너 버전을 제공.
- **Returns**: 버전 값(`int`)을 반환.

#### `getEffectiveMajorVersion()` & `getEffectiveMinorVersion()`

- **Description**: 실제 적용된 Servlet API 메이저 버전과 마이너 버전을 제공.
- **Returns**: 버전 값(`int`)을 반환.

#### Servlet API 버전별 특징

| 버전  | 핵심 특징                                           | 호환성 메모               | 비고                     |
|-----|-------------------------------------------------|----------------------|------------------------|
| 2.5 | 어노테이션 기반 리소스 주입 활용 확대, `web.xml` 중심 설정          | `javax.servlet` 사용   | 레거시 환경에서 자주 보임         |
| 3.0 | 비동기 요청 처리(`AsyncContext`), 프로그래밍 방식 컴포넌트 등록     | `javax.servlet` 사용   | 동시성 처리 기반 강화           |
| 3.1 | 논블로킹 I/O(`ReadListener`, `WriteListener`) 도입    | `javax.servlet` 사용   | 고성능 스트리밍 처리 지원         |
| 4.0 | HTTP/2, 서버 푸시(`PushBuilder`) 지원                 | `javax.servlet` 사용   | 프로토콜 기능 확장             |
| 5.0 | Jakarta EE 전환으로 `javax.*` -> `jakarta.*` 패키지 변경 | 소스 import 전면 수정 필요   | 마이그레이션 영향 가장 큼         |
| 6.0 | 최신 Java 기준선 반영, API 정비                          | `jakarta.servlet` 사용 | Spring Boot 3+/4 환경 핵심 |

### 정적 파일 컨트롤

#### `getMimeType(String file)`

- **Description**: 파일명 또는 경로에 대한 MIME 타입을 조회.
- **Returns**: MIME 타입 문자열을 반환하며, 없으면 `null`을 반환.

#### `getResourcePaths(String path)`

- **Description**: 지정 경로 하위 리소스 경로 목록을 조회.
- **Returns**: 리소스 경로 집합(`Set<String>`)을 반환.

#### `getResource(String path)`

- **Description**: 웹 애플리케이션 리소스를 URL객체로 반환.
- **Returns**: 리소스 URL을 반환하며, 없으면 `null`을 반환
- **URL 객체**:
    - java.net.URL 클래스의 인스턴스로, 리소스의 위치를 나타냄.
    - 파일 시스템, 클래스패스, 네트워크 등 다양한 위치의 리소스를 가리킬 수 있음.
    - URL 객체를 통해 리소스에 접근하여 내용을 읽거나 사용할 수 있음.

#### `getResourceAsStream(String path)`

- **Description**: 웹 애플리케이션 리소스를 스트림으로 조회.
- **Returns**: `InputStream`을 반환하며, 없으면 `null`을 반환.

#### `getRealPath(String path)`

- **Description**: 웹 경로를 실제 파일 시스템 경로로 변환.
- **Returns**: 실제 경로 문자열을 반환하며, 변환할 수 없으면 `null`을 반환.

### Request Dispatcher

- **RequestDispatcher**:
    - `RequestDispatcher`는 요청을 다른 리소스(서블릿, JSP, HTML 등)로 포워딩하거나 포함할 수 있는 객체.
    - `forward()` 메서드를 사용하여 요청을 다른 리소스로 전달하거나, `include()` 메서드를 사용하여 현재 응답에 다른 리소스의 출력을 포함 가능.

#### `getRequestDispatcher(String path)`

- **Description**: 경로 기반 요청 디스패처를 생성하거나 조회.
- **Returns**: `RequestDispatcher`를 반환하며, 없으면 `null`을 반환.

#### `getNamedDispatcher(String name)`

- **Description**: 이름 기반 요청 디스패처를 생성하거나 조회.
- **Returns**: `RequestDispatcher`를 반환하며, 없으면 `null`을 반환.

