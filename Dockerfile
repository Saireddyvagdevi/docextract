FROM eclipse-temurin:21-jdk-jammy

# Install Tesseract OCR and English, Hindi, Telugu language data
RUN apt-get update && \
    apt-get install -y \
        tesseract-ocr \
        tesseract-ocr-eng \
        tesseract-ocr-hin \
        tesseract-ocr-tel && \
    rm -rf /var/lib/apt/lists/*

# Tell Tesseract where its language files are
ENV TESSDATA_PREFIX=/usr/share/tesseract-ocr/5/tessdata

WORKDIR /app

COPY . .

RUN chmod +x mvnw && ./mvnw clean package -DskipTests

CMD ["sh", "-c", "java -jar target/*.jar"]