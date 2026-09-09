package docextract.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import docextract.AadhaarExtractor;
import docextract.AadhaarTextFormatter;
import docextract.DocumentTypeDetector;
import docextract.DocumentValidator;
import docextract.PanExtractor;
import docextract.dto.DocumentResponse;
import docextract.model.Document;
import docextract.service.DocumentService;
import docextract.service.FileExtractionService;

@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = "*")
public class DocumentController {

    private final DocumentService documentService;
    private final FileExtractionService fileExtractionService;
    private final DocumentTypeDetector documentTypeDetector;
    private final AadhaarExtractor aadhaarExtractor;
    private final PanExtractor panExtractor;
    private final DocumentValidator documentValidator;
    private final AadhaarTextFormatter aadhaarTextFormatter;

    public DocumentController(
            DocumentService documentService,
            FileExtractionService fileExtractionService,
            DocumentTypeDetector documentTypeDetector,
            AadhaarExtractor aadhaarExtractor,
            PanExtractor panExtractor,
            DocumentValidator documentValidator,
            AadhaarTextFormatter aadhaarTextFormatter) {

        this.documentService = documentService;
        this.fileExtractionService = fileExtractionService;
        this.documentTypeDetector = documentTypeDetector;
        this.aadhaarExtractor = aadhaarExtractor;
        this.panExtractor = panExtractor;
        this.documentValidator = documentValidator;
        this.aadhaarTextFormatter = aadhaarTextFormatter;
    }

    // =========================================================
    // SAVE DOCUMENT
    // =========================================================

    @PostMapping
    public Document createDocument(
            @RequestBody Document document) {

        return documentService.saveDocument(document);
    }

    // =========================================================
    // UPLOAD AND EXTRACT DOCUMENT
    // =========================================================

    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public DocumentResponse uploadDocument(
            @RequestParam("file") MultipartFile file)
            throws IOException {

        if (file == null || file.isEmpty()) {

            throw new IllegalArgumentException(
                    "Please upload a file."
            );
        }

        // =====================================================
        // 1. EXTRACT OCR TEXT
        // =====================================================

        String extractedText =
                fileExtractionService.extractText(file);

        // =====================================================
        // 2. DETECT DOCUMENT TYPE
        // =====================================================

        String documentType =
                documentTypeDetector.detectDocumentType(
                        extractedText
                );

        // =====================================================
        // 3. EXTRACT STRUCTURED FIELDS
        // =====================================================

        Map<String, String> fields =
                new HashMap<>();

        // =====================================================
        // AADHAAR
        // =====================================================

        if ("AADHAAR".equalsIgnoreCase(documentType)) {

            fields =
                    aadhaarExtractor.extractFields(
                            extractedText
                    );

            fields =
                    documentValidator.validateAadhaar(
                            fields
                    );

            /*
             * Keep Aadhaar formatting exactly as before.
             */
            extractedText =
                    aadhaarTextFormatter.format(
                            fields
                    );
        }

        // =====================================================
        // PAN
        // =====================================================

        if ("PAN".equalsIgnoreCase(documentType)) {

            fields =
                    panExtractor.extractFields(
                            extractedText
                    );

            fields =
                    documentValidator.validatePan(
                            fields
                    );

            /*
             * Create clean structured PAN text.
             *
             * Instead of displaying noisy OCR text,
             * display only the important extracted fields.
             */

            StringBuilder panText =
                    new StringBuilder();

            if (fields.containsKey("name")) {

                panText.append(
                        "Name: "
                );

                panText.append(
                        fields.get("name")
                );

                panText.append(
                        "\n"
                );
            }

            if (fields.containsKey("fatherName")) {

                panText.append(
                        "Father Name: "
                );

                panText.append(
                        fields.get("fatherName")
                );

                panText.append(
                        "\n"
                );
            }

            if (fields.containsKey("dob")) {

                panText.append(
                        "DOB: "
                );

                panText.append(
                        fields.get("dob")
                );

                panText.append(
                        "\n"
                );
            }

            if (fields.containsKey("panNumber")) {

                panText.append(
                        "PAN Number: "
                );

                panText.append(
                        fields.get("panNumber")
                );

                panText.append(
                        "\n"
                );
            }

            extractedText =
                    panText.toString().trim();
        }

        // =====================================================
        // 4. CREATE DOCUMENT
        // =====================================================

        Document document =
                new Document(
                        file.getOriginalFilename(),
                        file.getContentType(),
                        documentType,
                        extractedText,
                        fields
                );

        // =====================================================
        // 5. SAVE TO DATABASE
        // =====================================================

        Document savedDocument =
                documentService.saveDocument(
                        document
                );

        // =====================================================
        // 6. RETURN RESPONSE
        // =====================================================

        return new DocumentResponse(
                savedDocument.getId(),
                savedDocument.getFileName(),
                savedDocument.getFileType(),
                savedDocument.getDocumentType(),
                savedDocument.getExtractedText(),
                savedDocument.getFields()
        );
    }

    // =========================================================
    // GET ALL DOCUMENTS
    // =========================================================

    @GetMapping
    public List<Document> getAllDocuments() {

        return documentService.getAllDocuments();
    }

    // =========================================================
    // GET DOCUMENT BY ID
    // =========================================================

    @GetMapping("/{id}")
    public Document getDocumentById(
            @PathVariable Long id) {

        return documentService.getDocumentById(id);
    }

    // =========================================================
    // DELETE DOCUMENT
    // =========================================================

    @DeleteMapping("/{id}")
    public String deleteDocument(
            @PathVariable Long id) {

        documentService.deleteDocument(id);

        return "Document deleted successfully";
    }
}

