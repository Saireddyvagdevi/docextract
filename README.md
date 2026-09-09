# DocExtract

DocExtract is a full-stack document extraction system that automatically identifies **Aadhaar and PAN documents**, extracts relevant information using OCR and rule-based processing, validates the extracted fields, and stores document records in PostgreSQL.

## Features

* Upload PDF and image documents
* Automatic document type detection
* OCR-based text extraction using Tesseract
* Aadhaar number extraction and validation
* Aadhaar name, date of birth, and gender extraction
* Multilingual Aadhaar OCR support:

  * English
  * Telugu + English
  * Hindi + English
* PAN number extraction and validation
* PAN name, father's name, and date of birth extraction
* Document and field validation
* Invalid document detection
* PostgreSQL database persistence
* Document history
* React-based web interface
* Deployed frontend and backend

## Supported Documents

### Aadhaar

The system extracts:

* Aadhaar Number
* Name
* Date of Birth
* Gender

Aadhaar documents containing English, Telugu + English, and Hindi + English text are supported.

### PAN

The system extracts:

* PAN Number
* Name
* Father's Name
* Date of Birth

## Technology Stack

### Frontend

* React.js
* Vite
* HTML
* CSS
* JavaScript

### Backend

* Java 21
* Spring Boot
* Maven
* REST APIs

### Document Processing

* Tesseract OCR
* Apache PDFBox
* Java Regular Expressions
* Rule-based extraction and validation

### Database

* PostgreSQL
* Neon

### Testing & Deployment

* Postman
* HTTP Client
* Git & GitHub
* Render

## System Architecture

```text
React Frontend
      |
      | HTTP Request
      v
Spring Boot Backend
      |
      +----------------------+
      |                      |
      v                      v
Document Type Detector    OCR Service
                          Tesseract
                              |
                              v
                    Field Extraction
                    & Validation
                              |
                              v
                       PostgreSQL
                           Neon
```

## Processing Flow

```text
Upload Document
      |
      v
Detect Document Type
      |
      v
Extract Text using OCR
      |
      v
Identify Relevant Fields
      |
      v
Validate Extracted Data
      |
      v
Store Document Record
      |
      v
Return Structured Response
      |
      v
Display Results in React UI
```

## Project Structure

```text
docextract/
│
├── src/
│   └── main/
│       ├── java/
│       │   └── docextract/
│       │       ├── AadhaarExtractor.java
│       │       ├── AadhaarTextFormatter.java
│       │       ├── BackendApplication.java
│       │       ├── DataMasker.java
│       │       ├── DocumentTypeDetector.java
│       │       ├── DocumentValidator.java
│       │       ├── OcrController.java
│       │       ├── OcrService.java
│       │       ├── PanExtractor.java
│       │       │
│       │       ├── controller/
│       │       │   └── DocumentController.java
│       │       │
│       │       ├── dto/
│       │       │   └── DocumentResponse.java
│       │       │
│       │       ├── model/
│       │       │   └── Document.java
│       │       │
│       │       ├── repository/
│       │       │   └── DocumentRepository.java
│       │       │
│       │       └── service/
│       │           ├── DocumentService.java
│       │           └── FileExtractionService.java
│       │
│       └── resources/
│           └── application.properties
│
├── frontend/
│   ├── src/
│   │   ├── App.jsx
│   │   ├── App.css
│   │   ├── index.css
│   │   └── main.jsx
│   ├── public/
│   ├── package.json
│   └── vite.config.js
│
├── pom.xml
├── Dockerfile
├── test-upload.http
└── README.md
```

## Local Setup

### Prerequisites

Install:

* Java 21
* Maven
* Node.js and npm
* Tesseract OCR
* PostgreSQL or Neon
* Git

### Clone the Repository

```bash
git clone <your-github-repository-url>
cd docextract
```

### Backend Setup

Configure the required database and application properties in:

```text
src/main/resources/application.properties
```

Run the Spring Boot backend.

On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

The backend runs on:

```text
http://localhost:8080
```

### Frontend Setup

Open another terminal and navigate to:

```bash
cd frontend
```

Install dependencies:

```bash
npm install
```

Start the development server:

```bash
npm run dev
```

Vite will display the local frontend URL in the terminal.

## API

### Upload Document

```http
POST /api/documents/upload
```

Upload a PDF or image using `multipart/form-data`.

### Get Document History

```http
GET /api/documents
```

Returns previously processed document records stored in PostgreSQL.

## Example Output

### Aadhaar

```text
Document Type: AADHAAR

Name: Sarathkumar N P
DOB: 21/05/1997
Gender: Male
Aadhaar Number: 2554 8151 2997
```

### PAN

```text
Document Type: PAN

Name: DURAISAMY
Father Name: D MANIKANDAN
DOB: 16/07/1986
PAN Number: BNZPM2501F
```

## Validation

The application performs document-specific validation, including:

* Aadhaar number format validation
* PAN number format validation
* Date validation
* Required field validation
* Document type validation
* Invalid document detection

## Deployment

The application is deployed using **Render**.

```text
React Frontend
      |
      v
Render Static Site
      |
      v
Spring Boot REST API
      |
      v
Render Web Service
      |
      v
Neon PostgreSQL
```

## Security Considerations

This project is intended for academic and demonstration purposes.

For production use, additional security measures should be implemented, including:

* Authentication and authorization
* HTTPS enforcement
* Secure file storage
* File size and file type restrictions
* Sensitive-data encryption
* Stronger validation
* Access control
* Secure handling and deletion of uploaded documents

## Future Enhancements

* Support for additional document types
* Improved OCR for low-quality documents
* Additional regional language support
* Advanced document classification
* Secure authentication and authorization
* Cloud-based file storage
* Enhanced frontend dashboard
* Automated testing

## Project Status

**Completed and Deployed**

DocExtract currently supports Aadhaar and PAN document extraction with OCR, validation, multilingual Aadhaar processing, PostgreSQL persistence, document history, and a React web interface.
