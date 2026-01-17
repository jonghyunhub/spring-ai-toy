# jonghyun-boilerplate

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

