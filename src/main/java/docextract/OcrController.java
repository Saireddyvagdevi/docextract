package docextract;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ocr")
public class OcrController {

    private final OcrService ocrService;
    private final AadhaarExtractor aadhaarExtractor;
    private final DocumentTypeDetector documentTypeDetector;
    private final DocumentValidator documentValidator;
    private final DataMasker dataMasker;

    public OcrController(
            OcrService ocrService,
            AadhaarExtractor aadhaarExtractor,
            DocumentTypeDetector documentTypeDetector,
            DocumentValidator documentValidator,
            DataMasker dataMasker) {

        this.ocrService = ocrService;
        this.aadhaarExtractor = aadhaarExtractor;
        this.documentTypeDetector = documentTypeDetector;
        this.documentValidator = documentValidator;
        this.dataMasker = dataMasker;
    }

    @PostMapping
    public Map<String, Object> extractText(
            @RequestParam("file") MultipartFile file) throws IOException {

        File tempFile = File.createTempFile("ocr-", ".png");

        try {
            // Step 1: Save uploaded image temporarily
            file.transferTo(tempFile);

            // Step 2: OCR
            String extractedText = ocrService.extractText(tempFile);

            // Step 3: Automatically detect document type
            String documentType =
                    documentTypeDetector.detectDocumentType(extractedText);

            // Step 4: Prepare response
            Map<String, Object> response = new HashMap<>();

            response.put("documentType", documentType);
            response.put("rawText", extractedText);

            // Step 5: Extract and validate Aadhaar fields
            if ("AADHAAR".equals(documentType)) {

                Map<String, String> fields =
                        aadhaarExtractor.extractFields(extractedText);

                // Validate extracted fields
                fields = documentValidator.validateAadhaar(fields);

                // Mask Aadhaar number before returning response
                if (fields.containsKey("aadhaarNumber")) {

                    String maskedAadhaar =
                            dataMasker.maskAadhaar(
                                    fields.get("aadhaarNumber"));

                    fields.put("aadhaarNumber", maskedAadhaar);
                }

                response.put("fields", fields);

            } else {

                response.put(
                        "fields",
                        new HashMap<String, String>()
                );
            }

            return response;

        } finally {
            // Delete temporary file
            tempFile.delete();
        }
    }
}