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

### log

#### `log(String msg)`

- **Description**: 컨테이너 로그에 일반 메시지를 기록.
- **Returns**: `void`

#### `log(String message, Throwable throwable)`

- **Description**: 컨테이너 로그에 예외와 함께 메시지를 기록.
- **Returns**: `void`

### `getServerInfo()`

- **Description**: 서블릿 컨테이너 서버 정보를 반환
- **Returns**: 서버 정보 문자열을 반환.

### Init Parameter

- **초기화 파라미터(Init Parameter)**: 웹 애플리케이션의 설정 정보를 key-value 형태로 저장하는 공간.
    - `web.xml`과 같은 배포 서술자에서 정의하거나, 애플리케이션 코드에서 동적으로 등록할 수 있음.
    - 서블릿이나 필터가 초기화될 때 필요한 설정 값을 제공하는 데 주로 사용됨.

#### `getInitParameter(String name)`

- **Description**: 초기화 파라미터 값을 조회.
- **Returns**: 파라미터 값 문자열을 반환, 없으면 `null

#### `getInitParameterNames()`

- **Description**: 초기화 파라미터 이름 목록을 조회.
- **Returns**: 초기화 파라미터 이름 열거형(`Enumeration<String>`)을 반환.

#### `setInitParameter(String name, String value)`

- **Description**: 초기화 파라미터를 등록합니다.
- **Returns**: 성공하면 `true`, 이미 존재하면 `false`를 반환합니다.

### `getServletContextName()`

- **Description**: 웹 애플리케이션의 컨텍스트 이름을 반환.
- **Returns**: 컨텍스트 이름 문자열을 반환.

### Servlet 등록

- 

#### `addServlet(String servletName, String className)`

- **Description**: 클래스명 기반으로 서블릿을 등록.
- **Returns**: 동적 서블릿 등록 객체를 반환하며, 실패 시 `null`을 반환.

#### `addServlet(String servletName, Servlet servlet)`

- **Description**: 인스턴스 기반으로 서블릿을 등록.
- **Returns**: 동적 서블릿 등록 객체를 반환하며, 실패 시 `null`을 반환.

#### `addServlet(String servletName, Class<? extends Servlet> servletClass)`

- **Description**: 클래스 타입 기반으로 서블릿을 등록.
- **Returns**: 동적 서블릿 등록 객체를 반환하며, 실패 시 `null`을 반환.

### `addJspFile(String jspName, String jspFile)`

- **Description**: JSP 파일을 서블릿처럼 등록합니다.
- **Returns**: 동적 서블릿 등록 객체를 반환하며, 실패 시 `null`을 반환합니다.
- **호출 주체**: 애플리케이션 개발자
- **호출 시점**: JSP 파일 등록이 필요한 시점에 애플리케이션 코드에서 명시적으로 호출합니다.
- **호출 이유**: 동적 JSP 등록을 통해 애플리케이션 요구에 맞게 JSP를 추가하기 위해 필요합니다.

### `createServlet(Class<T> c)`

- **Description**: 서블릿 클래스를 인스턴스화합니다.
- **Returns**: 생성된 서블릿 인스턴스를 반환합니다.
- **호출 주체**: 프레임워크/컨테이너
- **호출 시점**: 서블릿 인스턴스가 필요한 시점에 프레임워크에 의해 자동으로 호출됩니다.
- **호출 이유**: 서블릿 생명주기 관리, 의존성 주입 등을 프레임워크가 처리하기 위해 필요합니다.

### `getServletRegistration(String servletName)`

- **Description**: 이름으로 등록된 서블릿 정보를 조회합니다.
- **Returns**: `ServletRegistration`을 반환하며, 없으면 `null`을 반환합니다.
- **호출 주체**: 둘 다 가능
- **호출 시점**: 서블릿 정보가 필요한 시점에 애플리케이션 코드에서 명시적으로 호출합니다.
- **호출 이유**: 서블릿 메타정보 조회, 동적 처리 로직 구현을 위해 필요합니다.

### `getServletRegistrations()`

- **Description**: 등록된 전체 서블릿 정보를 조회합니다.
- **Returns**: 서블릿 등록 맵(`Map<String, ? extends ServletRegistration>`)을 반환합니다.
- **호출 주체**: 둘 다 가능
- **호출 시점**: 서블릿 정보가 필요한 시점에 애플리케이션 코드에서 명시적으로 호출합니다.
- **호출 이유**: 서블릿 메타정보 조회, 동적 처리 로직 구현을 위해 필요합니다.

### `addFilter(String filterName, String className)`

- **Description**: 클래스명 기반으로 필터를 등록합니다.
- **Returns**: 동적 필터 등록 객체를 반환하며, 실패 시 `null`을 반환합니다.
- **호출 주체**: 애플리케이션 개발자
- **호출 시점**: 필터 등록이 필요한 시점에 애플리케이션 코드에서 명시적으로 호출합니다.
- **호출 이유**: 동적 필터 등록을 통해 애플리케이션 요구에 맞게 필터를 추가하기 위해 필요합니다.

### `addFilter(String filterName, Filter filter)`

- **Description**: 인스턴스 기반으로 필터를 등록합니다.
- **Returns**: 동적 필터 등록 객체를 반환하며, 실패 시 `null`을 반환합니다.
- **호출 주체**: 애플리케이션 개발자
- **호출 시점**: 필터 등록이 필요한 시점에 애플리케이션 코드에서 명시적으로 호출합니다.
- **호출 이유**: 동적 필터 등록을 통해 애플리케이션 요구에 맞게 필터를 추가하기 위해 필요합니다.

### `addFilter(String filterName, Class<? extends Filter> filterClass)`

- **Description**: 클래스 타입 기반으로 필터를 등록합니다.
- **Returns**: 동적 필터 등록 객체를 반환하며, 실패 시 `null`을 반환합니다.
- **호출 주체**: 애플리케이션 개발자
- **호출 시점**: 필터 등록이 필요한 시점에 애플리케이션 코드에서 명시적으로 호출합니다.
- **호출 이유**: 동적 필터 등록을 통해 애플리케이션 요구에 맞게 필터를 추가하기 위해 필요합니다.

### `createFilter(Class<T> c)`

- **Description**: 필터 클래스를 인스턴스화합니다.
- **Returns**: 생성된 필터 인스턴스를 반환합니다.
- **호출 주체**: 프레임워크/컨테이너
- **호출 시점**: 필터 인스턴스가 필요한 시점에 프레임워크에 의해 자동으로 호출됩니다.
- **호출 이유**: 필터 생명주기 관리, 의존성 주입 등을 프레임워크가 처리하기 위해 필요합니다.

### `getFilterRegistration(String filterName)`

- **Description**: 이름으로 등록된 필터 정보를 조회합니다.
- **Returns**: `FilterRegistration`을 반환하며, 없으면 `null`을 반환합니다.
- **호출 주체**: 둘 다 가능
- **호출 시점**: 필터 정보가 필요한 시점에 애플리케이션 코드에서 명시적으로 호출합니다.
- **호출 이유**: 필터 메타정보 조회, 동적 처리 로직 구현을 위해 필요합니다.

### `getFilterRegistrations()`

- **Description**: 등록된 전체 필터 정보를 조회합니다.
- **Returns**: 필터 등록 맵(`Map<String, ? extends FilterRegistration>`)을 반환합니다.
- **호출 주체**: 둘 다 가능
- **호출 시점**: 필터 정보가 필요한 시점에 애플리케이션 코드에서 명시적으로 호출합니다.
- **호출 이유**: 필터 메타정보 조회, 동적 처리 로직 구현을 위해 필요합니다.

### `getSessionCookieConfig()`

- **Description**: 세션 쿠키 설정 객체를 조회합니다.
- **Returns**: `SessionCookieConfig`를 반환합니다.
- **호출 주체**: 애플리케이션 개발자
- **호출 시점**: 세션 쿠키 설정이 필요한 시점에 애플리케이션 코드에서 명시적으로 호출합니다.
- **호출 이유**: 세션 쿠키 속성(예: 경로, 도메인, 보안 등) 설정을 위해 필요합니다.

### `setSessionTrackingModes(Set<SessionTrackingMode> modes)`

- **Description**: 사용할 세션 추적 모드를 설정합니다.
- **Returns**: 반환값이 없습니다 (`void`).
- **호출 주체**: 애플리케이션 개발자
- **호출 시점**: 세션 추적 모드 설정이 필요한 시점에 애플리케이션 코드에서 명시적으로 호출합니다.
- **호출 이유**: 애플리케이션 요구에 맞는 세션 추적 방식(예: 쿠키, URL 리라이트 등)을 설정하기 위해 필요합니다.

### `getDefaultSessionTrackingModes()`

- **Description**: 컨테이너 기본 세션 추적 모드를 조회합니다.
- **Returns**: 기본 모드 집합(`Set<SessionTrackingMode>`)을 반환합니다.
- **호출 주체**: 프레임워크/컨테이너
- **호출 시점**: 애플리케이션 부팅 초기화 과정이나 요청 처리 파이프라인 내부에서 자동으로 호출됩니다.
- **호출 이유**: 컨테이너 기본 설정에 따른 세션 추적 방식 확인을 위해 필요합니다.

### `getEffectiveSessionTrackingModes()`

- **Description**: 실제 적용된 세션 추적 모드를 조회합니다.
- **Returns**: 적용 모드 집합(`Set<SessionTrackingMode>`)을 반환합니다.
- **호출 주체**: 프레임워크/컨테이너
- **호출 시점**: 애플리케이션 부팅 초기화 과정이나 요청 처리 파이프라인 내부에서 자동으로 호출됩니다.
- **호출 이유**: 현재 컨텍스트에서 실제로 적용 중인 세션 추적 방식을 확인하기 위해 필요합니다.

### `addListener(String className)`

- **Description**: 리스너를 클래스명으로 등록합니다.
- **Returns**: 반환값이 없습니다 (`void`).
- **호출 주체**: 애플리케이션 개발자
- **호출 시점**: 리스너 등록이 필요한 시점에 애플리케이션 코드에서 명시적으로 호출합니다.
- **호출 이유**: 특정 이벤트에 대한 리스너를 동적으로 등록하기 위해 필요합니다.

### `addListener(T t)`

- **Description**: 리스너 인스턴스를 등록합니다.
- **Returns**: 반환값이 없습니다 (`void`).
- **호출 주체**: 애플리케이션 개발자
- **호출 시점**: 리스너 등록이 필요한 시점에 애플리케이션 코드에서 명시적으로 호출합니다.
- **호출 이유**: 특정 이벤트에 대한 리스너를 동적으로 등록하기 위해 필요합니다.

### `addListener(Class<? extends EventListener> listenerClass)`

- **Description**: 리스너 클래스를 등록합니다.
- **Returns**: 반환값이 없습니다 (`void`).
- **호출 주체**: 애플리케이션 개발자
- **호출 시점**: 리스너 등록이 필요한 시점에 애플리케이션 코드에서 명시적으로 호출합니다.
- **호출 이유**: 특정 이벤트에 대한 리스너를 동적으로 등록하기 위해 필요합니다.

### `createListener(Class<T> c)`

- **Description**: 리스너 클래스를 인스턴스화합니다.
- **Returns**: 생성된 리스너 인스턴스를 반환합니다.
- **호출 주체**: 프레임워크/컨테이너
- **호출 시점**: 리스너 인스턴스가 필요한 시점에 프레임워크에 의해 자동으로 호출됩니다.
- **호출 이유**: 리스너 생명주기 관리, 의존성 주입 등을 프레임워크가 처리하기 위해 필요합니다.

### `getJspConfigDescriptor()`

- **Description**: JSP 설정 디스크립터를 조회합니다.
- **Returns**: `JspConfigDescriptor`를 반환하며, 없으면 `null`을 반환합니다.
- **호출 주체**: 프레임워크/컨테이너
- **호출 시점**: JSP 관련 정보가 필요한 시점에 프레임워크에 의해 자동으로 호출됩니다.
- **호출 이유**: JSP 설정 정보(예: 태그 라이브러리, 초기화 파라미터 등) 조회를 위해 필요합니다.

### `declareRoles(String... roleNames)`

- **Description**: 애플리케이션 보안 역할을 선언합니다.
- **Returns**: 반환값이 없습니다 (`void`).
- **호출 주체**: 애플리케이션 개발자
- **호출 시점**: 보안 역할 설정이 필요한 시점에 애플리케이션 코드에서 명시적으로 호출합니다.
- **호출 이유**: 애플리케이션에서 사용하는 보안 역할을 명시적으로 선언하기 위해 필요합니다.

### `getVirtualServerName()`

- **Description**: 가상 서버 이름을 반환합니다.
- **Returns**: 가상 서버 이름 문자열을 반환합니다.
- **호출 주체**: 프레임워크/컨테이너
- **호출 시점**: 애플리케이션 부팅 초기화 과정이나 요청 처리 파이프라인 내부에서 자동으로 호출됩니다.
- **호출 이유**: 가상 서버 환경에서의 서블릿 컨테이너 동작을 지원하기 위해 필요합니다.

### `getSessionTimeout()`

- **Description**: 세션 기본 타임아웃(분)을 조회합니다.
- **Returns**: 세션 타임아웃 값(`int`)을 반환합니다.
- **호출 주체**: 둘 다 가능
- **호출 시점**: 세션 타임아웃 값이 필요한 시점에 애플리케이션 코드에서 명시적으로 호출합니다.
- **호출 이유**: 세션 관리 로직에서 기본 타임아웃 값을 참조하기 위해 필요합니다.

### `setSessionTimeout(int sessionTimeout)`

- **Description**: 세션 기본 타임아웃(분)을 설정합니다.
- **Returns**: 반환값이 없습니다 (`void`).
- **호출 주체**: 애플리케이션 개발자
- **호출 시점**: 세션 타임아웃 설정이 필요한 시점에 애플리케이션 코드에서 명시적으로 호출합니다.
- **호출 이유**: 애플리케이션 요구에 맞는 세션 타임아웃 값을 설정하기 위해 필요합니다.

### `getRequestCharacterEncoding()`

- **Description**: 기본 요청 문자 인코딩을 조회합니다.
- **Returns**: 인코딩 이름 문자열을 반환합니다.
- **호출 주체**: 둘 다 가능
- **호출 시점**: 요청 문자 인코딩 값이 필요한 시점에 애플리케이션 코드에서 명시적으로 호출합니다.
- **호출 이유**: 요청 데이터 처리 시 문자 인코딩을 올바르게 적용하기 위해 필요합니다.

### `setRequestCharacterEncoding(String encoding)`

- **Description**: 기본 요청 문자 인코딩을 설정합니다.
- **Returns**: 반환값이 없습니다 (`void`).
- **호출 주체**: 애플리케이션 개발자
- **호출 시점**: 요청 문자 인코딩 설정이 필요한 시점에 애플리케이션 코드에서 명시적으로 호출합니다.
- **호출 이유**: 요청 데이터 처리 시 사용할 문자 인코딩을 지정하기 위해 필요합니다.

### `getResponseCharacterEncoding()`

- **Description**: 기본 응답 문자 인코딩을 조회합니다.
- **Returns**: 인코딩 이름 문자열을 반환합니다.
- **호출 주체**: 둘 다 가능
- **호출 시점**: 응답 문자 인코딩 값이 필요한 시점에 애플리케이션 코드에서 명시적으로 호출합니다.
- **호출 이유**: 응답 데이터 처리 시 문자 인코딩을 올바르게 적용하기 위해 필요합니다.

### `setResponseCharacterEncoding(String encoding)`

- **Description**: 기본 응답 문자 인코딩을 설정합니다.
- **Returns**: 반환값이 없습니다 (`void`).
- **호출 주체**: 애플리케이션 개발자
- **호출 시점**: 응답 문자 인코딩 설정이 필요한 시점에 애플리케이션 코드에서 명시적으로 호출합니다.
- **호출 이유**: 응답 데이터 처리 시 사용할 문자 인코딩을 지정하기 위해 필요합니다.
