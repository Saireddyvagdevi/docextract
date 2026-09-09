package docextract;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

@Service
public class OcrService {

    private static final String TESSERACT_DATA_PATH =
        System.getProperty("os.name").toLowerCase().contains("win")
                ? "C:\\Program Files\\Tesseract-OCR\\tessdata"
                : "/usr/share/tesseract-ocr/5/tessdata";
    private static final String OCR_LANGUAGES =
            "eng+hin+tel";

    public String extractText(File imageFile) {

        File enlargedImage = null;

        try {

            BufferedImage original =
                    ImageIO.read(imageFile);

            if (original == null) {
                throw new RuntimeException(
                        "Unable to read image"
                );
            }

            // =====================================================
            // KEEP ORIGINAL WORKING 3X OCR
            // =====================================================

            int width =
                    original.getWidth() * 3;

            int height =
                    original.getHeight() * 3;

            BufferedImage enlarged =
                    new BufferedImage(
                            width,
                            height,
                            BufferedImage.TYPE_INT_RGB
                    );

            Graphics2D graphics =
                    enlarged.createGraphics();

            graphics.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC
            );

            graphics.setRenderingHint(
                    RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY
            );

            graphics.drawImage(
                    original,
                    0,
                    0,
                    width,
                    height,
                    null
            );

            graphics.dispose();

            enlargedImage =
                    File.createTempFile(
                            "ocr-image-",
                            ".png"
                    );

            ImageIO.write(
                    enlarged,
                    "png",
                    enlargedImage
            );

            // =====================================================
            // ORIGINAL OCR - DO NOT CHANGE
            // =====================================================

            String originalText =
                    performOCR(
                            enlargedImage,
                            11
                    );

            /*
             * This is the OCR result that already works for:
             *
             * NITU BHURVE
             * 17/06/1996
             * FEGPD5131E
             *
             * We keep it exactly as it is.
             */

            // =====================================================
            // TARGETED SECOND OCR FOR FATHER NAME
            // =====================================================

            if (looksLikePan(originalText)) {

                String fatherText =
                        extractFatherNameUsingOCR(
                                enlargedImage
                        );

                if (fatherText != null
                        && !fatherText.isBlank()) {

                    return originalText.trim()
                            + "\n"
                            + fatherText.trim();
                }
            }

            return originalText;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Image processing failed: "
                            + e.getMessage(),
                    e
            );

        } finally {

            if (enlargedImage != null
                    && enlargedImage.exists()) {

                enlargedImage.delete();
            }
        }
    }

    // =============================================================
    // TARGETED FATHER NAME OCR
    // =============================================================

    private String extractFatherNameUsingOCR(
            File imageFile) {

        String text;

        try {

            /*
             * PSM 6 is used only as a supporting OCR pass.
             * Its output is NEVER used to replace the original
             * name, DOB or PAN number.
             */

            text =
                    performOCR(
                            imageFile,
                            6
                    );

        } catch (Exception e) {

            return "";
        }

        if (text == null || text.isBlank()) {
            return "";
        }

        String[] lines =
                text.replace("\r", "")
                        .split("\n");

        for (int i = 0;
                i < lines.length;
                i++) {

            String current =
                    cleanLine(lines[i]);

            if (!isFatherLabel(current)) {
                continue;
            }

            /*
             * Father's Name label was found.
             * The next useful line should contain the person's name.
             */

            for (int j = i + 1;
                    j < lines.length;
                    j++) {

                String candidate =
                        cleanLine(lines[j]);

                if (candidate.isBlank()) {
                    continue;
                }

                /*
                 * Do NOT accept things such as:
                 *
                 * Date of
                 * Birth
                 * Signature
                 * Department
                 * Government
                 *
                 * Only accept a plausible English person name.
                 */

                if (isValidPersonName(candidate)) {

                    return "Father's Name\n"
                            + candidate;
                }

                /*
                 * If the next meaningful line clearly belongs
                 * to another section, stop searching.
                 */

                String lower =
                        candidate.toLowerCase();

                if (lower.contains("date")
                        || lower.contains("birth")
                        || lower.contains("signature")
                        || lower.contains("department")
                        || lower.contains("government")
                        || lower.contains("permanent")) {

                    break;
                }
            }
        }

        return "";
    }

    // =============================================================
    // FATHER LABEL DETECTION
    // =============================================================

    private boolean isFatherLabel(
            String line) {

        if (line == null || line.isBlank()) {
            return false;
        }

        String lower =
                line.toLowerCase();

        /*
         * Normal English OCR
         */

        if (lower.contains("father's name")) {
            return true;
        }

        if (lower.contains("fathers name")) {
            return true;
        }

        if (lower.contains("father name")) {
            return true;
        }

        if (lower.contains("father")) {
            return true;
        }

        /*
         * Common OCR variations
         */

        if (lower.contains("fath er")) {
            return true;
        }

        if (lower.contains("fathcr")) {
            return true;
        }

        if (lower.contains("fathers")) {
            return true;
        }

        /*
         * Hindi Father's Name label.
         *
         * पिता का नाम
         */

        if (line.contains("पिता")) {
            return true;
        }

        return false;
    }

    // =============================================================
    // PERSON NAME VALIDATION
    // =============================================================

    private boolean isValidPersonName(
            String value) {

        if (value == null
                || value.isBlank()) {

            return false;
        }

        String name =
                cleanLine(value);

        if (name.length() < 3) {
            return false;
        }

        /*
         * Only English alphabet names are accepted here.
         * This prevents OCR fragments such as "Date of" from
         * becoming the father name.
         */

        if (!name.matches(
                "[A-Za-z]+(?:[ .'-][A-Za-z]+)*"
        )) {

            return false;
        }

        String upper =
                name.toUpperCase();

        /*
         * Reject common PAN-card labels.
         */

        if (upper.equals("DATE OF")) {
            return false;
        }

        if (upper.equals("DATE OF BIRTH")) {
            return false;
        }

        if (upper.equals("BIRTH")) {
            return false;
        }

        if (upper.equals("SIGNATURE")) {
            return false;
        }

        if (upper.equals("DEPARTMENT")) {
            return false;
        }

        if (upper.equals("GOVT OF INDIA")) {
            return false;
        }

        if (upper.equals("GOVERNMENT OF INDIA")) {
            return false;
        }

        if (upper.equals("INCOME TAX")) {
            return false;
        }

        if (upper.equals("PERMANENT ACCOUNT NUMBER")) {
            return false;
        }

        if (upper.equals("PERMANENT ACCOUNT NUMBER CARD")) {
            return false;
        }

        if (upper.equals("NAME")) {
            return false;
        }

        if (upper.equals("FATHER NAME")) {
            return false;
        }

        if (upper.equals("FATHERS NAME")) {
            return false;
        }

        if (upper.equals("FATHER'S NAME")) {
            return false;
        }

        /*
         * A real name normally contains at least one space
         * on these PAN cards.
         */

        if (!name.contains(" ")) {
            return false;
        }

        return true;
    }

    // =============================================================
    // CLEAN OCR LINE
    // =============================================================

    private String cleanLine(
            String value) {

        if (value == null) {
            return "";
        }

        return value
                .replaceAll(
                        "\\s+",
                        " "
                )
                .trim();
    }

    // =============================================================
    // PAN DETECTION
    // =============================================================

    private boolean looksLikePan(
            String text) {

        if (text == null
                || text.isBlank()) {

            return false;
        }

        String lower =
                text.toLowerCase();

        /*
         * Normal PAN number.
         */

        if (text.matches(
                "(?s).*\\b[A-Za-z]{5}\\d{4}[A-Za-z]\\b.*"
        )) {

            return true;
        }

        /*
         * PAN keywords.
         */

        if (lower.contains("income tax")) {
            return true;
        }

        if (lower.contains("permanent account")) {
            return true;
        }

        if (lower.contains("pan card")) {
            return true;
        }

        if (lower.contains("account number")) {
            return true;
        }

        return false;
    }

    // =============================================================
    // OCR
    // =============================================================

    private String performOCR(
            File imageFile,
            int pageSegmentationMode) {

        Tesseract tesseract =
                new Tesseract();

        tesseract.setDatapath(
                TESSERACT_DATA_PATH
        );

        tesseract.setLanguage(
                OCR_LANGUAGES
        );

        tesseract.setPageSegMode(
                pageSegmentationMode
        );

        tesseract.setOcrEngineMode(1);

        tesseract.setVariable(
                "preserve_interword_spaces",
                "1"
        );

        try {

            return tesseract.doOCR(
                    imageFile
            );

        } catch (TesseractException e) {

            throw new RuntimeException(
                    "OCR failed: "
                            + e.getMessage(),
                    e
            );
        }
    }
}

