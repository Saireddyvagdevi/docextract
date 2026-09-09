package docextract.dto;

import java.util.Map;

public class DocumentResponse {

    private Long id;
    private String fileName;
    private String fileType;
    private String documentType;
    private String extractedText;
    private Map<String, String> fields;

    public DocumentResponse() {
    }

    public DocumentResponse(
            Long id,
            String fileName,
            String fileType,
            String documentType,
            String extractedText,
            Map<String, String> fields) {

        this.id = id;
        this.fileName = fileName;
        this.fileType = fileType;
        this.documentType = documentType;
        this.extractedText = extractedText;
        this.fields = fields;
    }

    public Long getId() {
        return id;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public String getDocumentType() {
        return documentType;
    }

    public String getExtractedText() {
        return extractedText;
    }

    public Map<String, String> getFields() {
        return fields;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public void setExtractedText(String extractedText) {
        this.extractedText = extractedText;
    }

    public void setFields(Map<String, String> fields) {
        this.fields = fields;
    }
}