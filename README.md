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
- OpenAI 에 대화 요청을 보내고 응답을 받는다. (이 작업을 스레드 라는 단위로 명명)
- 사용자 : 스레드 => 1 : N 관계
- 각 요청을 보낼때 하나의 스레드에서 