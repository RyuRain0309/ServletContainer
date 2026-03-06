# Servlet

## Servlet

> Servlet은 Client의 HTTP Request을 받아 처리하고, 그 결과를 다시 클라이언트에게 Response으로 돌려주는 자바 클래스 <br>
> 과거에는 정적인 HTML 문서만 보여주던 웹 서버에, 데이터베이스를 조회하거나 연산을 수행하는 등의 `동적인 기능`을 부여하기 위해 탄생

### 특징:

- jakarta.servlet.Servlet 인터페이스를 구현해야 합니다. (실제로는 대부분 HttpServlet 클래스를 상속)
- Spring의 심장인 DispatcherServlet 역시 수많은 서블릿 중 하나로, 모든 요청을 혼자 다 받아서 각 컨트롤러로 분배하는 역할.

### 생명주기 (Lifecycle):

- init(): 객체가 생성될 때 단 한 번 호출되어 초기화 작업을 수행.
- service(): 클라이언트의 요청이 들어올 때마다 호출. 내부적으로 doGet, doPost 등으로 나뉘어 실제 로직 처리
- destroy(): 서버가 종료되거나 서블릿이 수정되어 메모리에서 내려갈 때 호출.

## Servlet Context

> ServletContext는 하나의 웹 애플리케이션 당 하나만 생성되는 거대한 `공용 메모리 공간`이자 `환경 설정 객체`. <br>
> 서블릿들이 서로 소통하고 서버의 환경 정보를 읽어올 수 있도록 도와주는 매개체 역할.

### 주요 역할:

- 전역 데이터 공유: setAttribute()와 getAttribute()를 통해 애플리케이션 내의 모든 서블릿이 데이터를 공유.
- 애플리케이션 설정 정보 제공: 설정 파일(web.xml 등)에 적힌 초기화 파라미터를 로딩.
- 동적 등록 및 관리: 애플리케이션이 실행되는 도중에 새로운 서블릿이나 필터를 메모리에 등록 가능.
- 스프링과의 관계: Spring의 거대한 Bean 보관소인 ApplicationContext는 ServletContext와 함께 동작하며, Servlet들이 Spring Bean에 접근할 수 있도록 연결.

## Servlet Container

> Servlet Container는 개발자가 만든 서블릿들의 생명주기를 관리하고, 네트워크 통신을 대신 처리해 주는 거대한 프로그램. <br>
> 대표적인 예로 Tomcat, Jetty, Undertow가 있으며, 'WAS(Web Application Server)'의 핵심 부품

### 핵심 역할:

- 요청 및 응답 처리: Request 파싱하여 HttpServletRequest, HttpServletResponse 객체로 만들어 서블릿에게 전달.
- 생명주기 관리: 서블릿 클래스를 메모리에 `로딩`, `초기화`, `정리`하는 역할.
- 멀티 스레딩 처리: 동시에 여러 요청에 대비해서, 미리 Thread을 대기시켜 두었다가 요청마다 하나씩 할당.
- 보안 및 라우팅: 어떤 URL 주소로 요청이 왔을 때 어떤 서블릿을 실행할지 결정하고, 접근 권한을 제어.