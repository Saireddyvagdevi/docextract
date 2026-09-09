package docextract;

import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

@Service
public class DocumentTypeDetector {

    public String detectDocumentType(String text) {

        if (text == null || text.trim().isEmpty()) {
            return "UNKNOWN";
        }

        // Normalize OCR text
        String lowerText = text
                .toLowerCase()
                .replaceAll("\\s+", " ")
                .trim();

        // =========================================================
        // PAN NUMBER DETECTION
        // =========================================================
        // PAN format: 5 letters + 4 digits + 1 letter
        //
        // Example:
        // FEGPD5131E
        // BNZPM2501F
        // =========================================================

        Pattern panPattern = Pattern.compile(
                "\\b[A-Z]{5}\\d{4}[A-Z]\\b",
                Pattern.CASE_INSENSITIVE
        );

        if (panPattern.matcher(text).find()) {
            return "PAN";
        }

        // =========================================================
        // AADHAAR DETECTION
        // =========================================================

        if (lowerText.contains("aadhaar")
                || lowerText.contains("uidai")
                || lowerText.contains("unique identification")
                || lowerText.contains("government of india")
                || lowerText.contains("giverenmentonnaameen")
                || lowerText.contains("geineaawaes")
                || lowerText.contains("date of birth")
                || lowerText.contains("dob")
                || lowerText.contains("male")
                || lowerText.contains("female")) {

            return "AADHAAR";
        }

        // =========================================================
        // PAN TEXT DETECTION
        // =========================================================

        if (lowerText.contains("income tax department")
                || lowerText.contains("income tax")
                || lowerText.contains("permanent account number")
                || lowerText.contains("permanent ac")
                || lowerText.contains("pan card")
                || lowerText.contains("father's name")
                || lowerText.contains("fathers name")) {

            return "PAN";
        }

        // =========================================================
        // DRIVING LICENCE
        // =========================================================

        if (lowerText.contains("driving licence")
                || lowerText.contains("driving license")) {

            return "DRIVING_LICENSE";
        }

        // =========================================================
        // PASSPORT
        // =========================================================

        if (lowerText.contains("passport")) {

            return "PASSPORT";
        }

        return "UNKNOWN";
    }
}