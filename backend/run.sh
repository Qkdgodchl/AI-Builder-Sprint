#!/bin/bash
echo "🚀 잇다 ITDA 백엔드 서버를 시작합니다..."
cd "$(dirname "$0")"

if command -v gradle &> /dev/null; then
    gradle bootRun
elif [ -f "./gradlew" ]; then
    ./gradlew bootRun
else
    echo "⚠️ Gradle이 설치되어 있지 않거나 gradlew 스크립트가 없습니다."
    echo "💡 IntelliJ 또는 STS/Eclipse IDE에서 PixelCareApplication.java 를 실행해주세요!"
fi
