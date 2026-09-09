FROM eclipse-temurin:21-jdk-jammy

# Install Tesseract OCR and all required languages
RUN apt-get update && \
    apt-get install -y \
        tesseract-ocr \
        tesseract-ocr-eng \
        tesseract-ocr-hin \
        tesseract-ocr-tel && \
    rm -rf /var/lib/apt/lists/*

# Create our own fixed tessdata directory
RUN mkdir -p /opt/tessdata && \
    find /usr/share/tesseract-ocr -type f -name "eng.traineddata" -exec cp {} /opt/tessdata/ \; && \
    find /usr/share/tesseract-ocr -type f -name "hin.traineddata" -exec cp {} /opt/tessdata/ \; && \
    find /usr/share/tesseract-ocr -type f -name "tel.traineddata" -exec cp {} /opt/tessdata/ \; && \
    echo "===== TESSDATA FILES =====" && \
    ls -lh /opt/tessdata && \
    test -f /opt/tessdata/eng.traineddata && \
    test -f /opt/tessdata/hin.traineddata && \
    test -f /opt/tessdata/tel.traineddata

# Tesseract expects TESSDATA_PREFIX to be the parent of tessdata
ENV TESSDATA_PREFIX=/opt

WORKDIR /app

COPY . .

RUN chmod +x mvnw && \
    ./mvnw clean package -DskipTests

CMD ["sh", "-c", "java -jar target/*.jar"]