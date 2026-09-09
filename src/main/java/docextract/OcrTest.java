package docextract;

import java.io.File;

public class OcrTest {

    public static void main(String[] args) {

        OcrService ocrService = new OcrService();

        File imageFile = new File(
        "C:\\Users\\vagde\\OneDrive\\Documents\\Desktop\\test-aadhaar.png"
);

        if (!imageFile.exists()) {
            System.out.println("Image file not found!");
            System.out.println("Path: " + imageFile.getAbsolutePath());
            return;
        }

        String extractedText = ocrService.extractText(imageFile);

        System.out.println("========== OCR RESULT ==========");
        System.out.println(extractedText);
        System.out.println("================================");
    }
}