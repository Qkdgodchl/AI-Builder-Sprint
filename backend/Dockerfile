# Stage 1: Build
FROM gradle:8.10-jdk21-alpine AS builder
WORKDIR /app
COPY . .
RUN gradle build -x test --no-daemon

# Stage 2: Run
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
# 약정서 PDF에 한글을 쓴다. 기본 이미지에는 한글 폰트가 없어
# 폰트를 넣지 않으면 CID 대체 폰트로 떨어져 글자가 깨지거나 생성이 실패한다.
RUN apk add --no-cache font-noto-cjk
COPY --from=builder /app/build/libs/pixel-care-backend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
# 512MB 컨테이너에서 JVM 기본 힙(25%)은 너무 작아 GC가 길어지고,
# GC가 밀리면 헬스체크(5초)가 타임아웃돼 멀쩡한 인스턴스가 재시작된다.
# 힙을 70%로 명시하고, 진짜 OOM이면 좀비로 남지 않고 즉시 재기동되게 한다.
CMD ["java", "-XX:MaxRAMPercentage=70.0", "-XX:+ExitOnOutOfMemoryError", "-jar", "app.jar"]
