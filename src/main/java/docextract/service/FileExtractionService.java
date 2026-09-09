package docextract.service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import docextract.OcrService;

@Service
public class FileExtractionService {

    private final OcrService ocrService;

    public FileExtractionService(OcrService ocrService) {
        this.ocrService = ocrService;
    }

    public String extractText(MultipartFile file) throws IOException {

        String fileName = file.getOriginalFilename();

        if (fileName == null) {
            throw new IllegalArgumentException("File name is missing.");
        }

        String lowerFileName = fileName.toLowerCase();

        if (lowerFileName.endsWith(".pdf")) {
            return extractPdf(file);
        }

        if (lowerFileName.endsWith(".png")
                || lowerFileName.endsWith(".jpg")
                || lowerFileName.endsWith(".jpeg")) {

            return extractImage(file);
        }

        if (lowerFileName.endsWith(".txt")) {
            return new String(file.getBytes());
        }

        throw new IllegalArgumentException(
                "Unsupported file type. Please upload PDF, image, or text file."
        );
    }

    private String extractImage(MultipartFile file) throws IOException {

        String originalFileName = file.getOriginalFilename();

        String extension = ".png";

        if (originalFileName != null) {

            String lowerName = originalFileName.toLowerCase();

            if (lowerName.endsWith(".jpg")) {
                extension = ".jpg";
            } else if (lowerName.endsWith(".jpeg")) {
                extension = ".jpeg";
            } else if (lowerName.endsWith(".png")) {
                extension = ".png";
            }
        }

        File tempImage = File.createTempFile(
                "uploaded-image-",
                extension
        );

        try {

            file.transferTo(tempImage);

            // =====================================================
            // OCR
            // =====================================================

            String extractedText =
                    ocrService.extractText(tempImage);

            // =====================================================
            // DEBUG OUTPUT
            // =====================================================

            System.out.println(
                    "========== IMAGE OCR TEXT =========="
            );

            if (extractedText == null
                    || extractedText.trim().isEmpty()) {

                System.out.println(
                        "OCR RESULT IS EMPTY"
                );

            } else {

                System.out.println(
                        extractedText
                );
            }

            System.out.println(
                    "===================================="
            );

            return extractedText;

        } finally {

            if (tempImage.exists()) {
                tempImage.delete();
            }
        }
    }

    private String extractPdf(MultipartFile file) throws IOException {

        File tempPdf = File.createTempFile(
                "uploaded-document-",
                ".pdf"
        );

        StringBuilder finalText = new StringBuilder();

        try {

            file.transferTo(tempPdf);

            try (PDDocument document = Loader.loadPDF(tempPdf)) {

                PDFTextStripper stripper =
                        new PDFTextStripper();

                String pdfText =
                        stripper.getText(document);

                if (pdfText != null
                        && pdfText.trim().length() > 10) {

                    return pdfText.trim();
                }

                PDFRenderer renderer =
                        new PDFRenderer(document);

                int totalPages =
                        document.getNumberOfPages();

                for (int page = 0;
                        page < totalPages;
                        page++) {

                    System.out.println(
                            "========== OCR PDF PAGE "
                                    + (page + 1)
                                    + " / "
                                    + totalPages
                                    + " =========="
                    );

                    BufferedImage pageImage =
                            renderer.renderImageWithDPI(
                                    page,
                                    150,
                                    ImageType.RGB
                            );

                    File tempPageImage =
                            File.createTempFile(
                                    "pdf-page-",
                                    ".png"
                            );

                    try {

                        ImageIO.write(
                                pageImage,
                                "png",
                                tempPageImage
                        );

                        String pageText =
                                ocrService.extractText(
                                        tempPageImage
                                );

                        if (pageText != null
                                && !pageText.trim().isEmpty()) {

                            finalText.append(
                                    pageText.trim()
                            );

                            finalText.append(
                                    "\n"
                            );
                        }

                    } finally {

                        if (tempPageImage.exists()) {
                            tempPageImage.delete();
                        }

                        pageImage.flush();
                    }
                }
            }

            System.out.println(
                    "========== FINAL PDF TEXT =========="
            );

            System.out.println(
                    finalText
            );

            return finalText.toString().trim();

        } finally {

            if (tempPdf.exists()) {
                tempPdf.delete();
            }
        }
    }
}