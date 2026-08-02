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
CMD ["java", "-jar", "app.jar"]
