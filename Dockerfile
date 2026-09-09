FROM eclipse-temurin:21-jdk-jammy

RUN apt-get update && \
    apt-get install -y \
        tesseract-ocr \
        tesseract-ocr-eng \
        tesseract-ocr-hin \
        tesseract-ocr-tel && \
    rm -rf /var/lib/apt/lists/*

RUN echo "=== TESSERACT VERSION ===" && \
    tesseract --version && \
    echo "=== TESSERACT LANGUAGES ===" && \
    tesseract --list-langs && \
    echo "=== TESSDATA FILES ===" && \
    ls -lh /usr/share/tesseract-ocr/5/tessdata/

ENV TESSDATA_PREFIX=/usr/share/tesseract-ocr/5/tessdata

WORKDIR /app

COPY . .

RUN chmod +x mvnw && \
    ./mvnw clean package -DskipTests

CMD ["sh", "-c", "java -jar target/*.jar"]