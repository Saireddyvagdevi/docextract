FROM eclipse-temurin:21-jdk-jammy

# Install Tesseract OCR and language data
RUN apt-get update && \
    apt-get install -y tesseract-ocr tesseract-ocr-eng tesseract-ocr-hin tesseract-ocr-tel && \
    rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy Maven project
COPY . .

# Build the Spring Boot application
RUN chmod +x mvnw && ./mvnw clean package -DskipTests

# Start the application
CMD ["sh", "-c", "java -jar target/*.jar"]