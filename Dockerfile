# firesafety-be 백엔드 이미지. 소스 빌드 후 실행용 JRE만 남기는 멀티스테이지 빌드.
# 온프레미스 물리서버(x86_64)에서 그대로 빌드해서 쓸 경우 --platform 옵션은 필요 없다.
# 개발 Mac(Apple Silicon)에서 이미지를 만들어 옮길 때만
# `docker buildx build --platform linux/amd64 ...`로 빌드할 것.

# 1단계: Gradle 빌드
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew

COPY src ./src
# 테스트는 CI/로컬에서 이미 검증하므로 이미지 빌드 시간을 줄이기 위해 스킵
RUN ./gradlew bootJar -x test --no-daemon

# 2단계: 실행 이미지 (JDK 아닌 JRE만 포함해 이미지 크기 축소)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# root 권한 실행 방지
RUN addgroup -S firesafety && adduser -S firesafety -G firesafety
COPY --from=build /workspace/build/libs/*.jar app.jar
USER firesafety

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
