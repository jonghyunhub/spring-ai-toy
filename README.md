### Development environment setup

## Git Hook
This setting makes run `lint` on every commit.

```
$ git config core.hookspath .githooks
```

## IntelliJ IDEA
This setting makes it easier to run the `test code` out of the box.

```
// Gradle Build and run with IntelliJ IDEA
Build, Execution, Deployment > Build Tools > Gradle > Run tests using > IntelliJ IDEA	
```

# Getting start

# 1. 기존 컨테이너와 볼륨 삭제 (데이터 초기화)
cd docker
docker-compose down -v

# 2. PostgreSQL 재시작 (init 스크립트 실행됨)
docker-compose up -d

# 3. 로그 확인 (테이블 생성 확인)
docker-compose logs postgres


# Ai-chat bot 기능 구현

## 사용자 기능
- 사용자 회원가입 및 로그인
- 인증처리 JWT(JSON Web Tokens) 방식
- 다른 API에서 사용자 요청 토큰을 통해 인증이 가능해야 함

### 계획
- 간단한 구현을 위해 Interceptor 기반의 필터로 구현 
- req/res 이전에 요청을 가로채고 인증처리후 응답반환
- 회원가입/로그인 제외한 모든 요청은 인증 처리
- 비밀번호 암호화
- 이메일 기준으로 같은 이메일 회원가입 동시성 예외처리

## AI chat 기능
- 사용자가 ai와 채팅을 위한 쓰레드라는 개념이 있고, 한 사용자가 N개의 쓰레드를 만들수 있다. (사용자 : 스레드 => 1 : N 관계)
- 각 채팅은 상태가 없기 때문에, 채팅의 상태를 관리하기 위한 개념이 바로 쓰레드이다. (스레드 : 채팅 => 1 : N 관계)
- 하나의 쓰레드안에 여러 채팅(질문-답변)의 내용이 담기며 해당 채팅의 집합을 통해 컨텍스트를 전달한다.

### 계획
- AI 모델 연동은 나중에 하되 연동 구현체를 Mocking 하여 요구사항에 맞는 두가지 방식 구현
  - Streaming 응답 방식 (응답을 잘라서 chunk 형태로 나눠서 반환) -> sse 방식으로 구현
  - 일반 응답(동기식으로 대기하였다가 ai 모델 응답 다 나오면 한번에 반환)
- 처리 흐름
   1. 스레드 존재 여부 & 권한 검증 (threadRepository.findByIdAndUserId())
   2. 이전 채팅 조회 (컨텍스트 생성용)
   3. AI API 호출
      - 일반 모드: 동기 호출 → 완전한 답변 수신
      - 스트리밍 모드: SSE 스트림 반환
   4. Chat 저장 (question + answer)
   5. Thread의 lastChatAt 업데이트
- 스트리밍 방식 구현 옵션
  - Option A: WebFlux (Reactive)
    - Flux<ChatStreamChunk> 반환
    - AI API SSE 스트림을 그대로 클라이언트에 전달
    - 장점: 메모리 효율적, 실시간 전송
    - 단점: Spring WebFlux 의존성 추가 필요
  - Option B: SseEmitter (MVC)
    - Spring MVC의 SseEmitter 사용
    - 별도 스레드에서 AI API 호출, 청크마다 emit
    - 장점: 기존 MVC 구조 유지
    - 단점: 스레드 관리 필요
  - 빠른 구현을 위해 SseEmitter 채택, 현재 단계에서 고가용성의 성능이 불필요
- Sse(Server Sent Event) 
  - Web Socket과 비슷하게 커넥션을 맺고 응답이 끝나면 해당 커넥션을 끊는게 아닌 일부 유지하면서 서버가 패킷을 클라이언트한테 먼저 보낼수 있는 방식
  - 클라이언트가 요청 시 Accept: text/event-stream 헤더를 보내고, 서버는 응답에 Content-Type: text/event-stream을 설정

## 사용자 피드백 관리 기능
- Chat 에 대한 피드백 생성
- 자신이 만든 Chat에만 피드백 생성 가능
- 관리자는 모든 대화에 대해 피드백 생성가능
- 하나의 Chat, '여러 사용자가 만든' 피드백 가능 => Chat : Feedback (1:N 관계)
  - Chat 만든 유저 기준 Chat :  Feedback => 1:1
  - 관리자 기준(여러명의 관리자가 한명씩 피드백 생성 가능) Chat : Feedback => 1:N