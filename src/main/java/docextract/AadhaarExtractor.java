package docextract;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

@Service
public class AadhaarExtractor {

    public Map<String, String> extractFields(String text) {

        Map<String, String> fields = new HashMap<>();

        if (text == null || text.trim().isEmpty()) {
            fields.put("error", "No text extracted from document");
            return fields;
        }

        // Normalize OCR text
        String normalizedText = text
                .replace("\r", "\n")
                .replaceAll("[ \\t]+", " ")
                .trim();

        /*
         * =========================================================
         * 1. EXTRACT AADHAAR NUMBER
         * =========================================================
         */

        Pattern aadhaarPattern = Pattern.compile(
                "(?<!\\d)(\\d{4}\\s*\\d{4}\\s*\\d{4})(?!\\d)"
        );

        Matcher aadhaarMatcher = aadhaarPattern.matcher(normalizedText);

        if (aadhaarMatcher.find()) {

            String aadhaarNumber = aadhaarMatcher.group(1)
                    .replaceAll("\\s+", " ")
                    .trim();

            fields.put("aadhaarNumber", aadhaarNumber);

        } else {

            fields.put("aadhaarError",
                    "Aadhaar number not found");
        }

        /*
         * =========================================================
         * 2. EXTRACT DATE OF BIRTH
         * =========================================================
         */

        Pattern dobPattern = Pattern.compile(
                "(?i)(?:DOB|D\\.O\\.B|Date of Birth|जन्म|పుట్టిన)\\s*[:\\-/]?\\s*" +
                "(\\d{1,2}[/-]\\d{1,2}[/-]\\d{4})"
        );

        Matcher dobMatcher = dobPattern.matcher(normalizedText);

        if (dobMatcher.find()) {

            fields.put("dob", dobMatcher.group(1));

        } else {

            // Fallback: find any date in the document
            Pattern generalDatePattern = Pattern.compile(
                    "\\b(\\d{1,2}[/-]\\d{1,2}[/-]\\d{4})\\b"
            );

            Matcher generalDateMatcher =
                    generalDatePattern.matcher(normalizedText);

            if (generalDateMatcher.find()) {
                fields.put("dob", generalDateMatcher.group(1));
            } else {
                fields.put("dobError", "Date of birth not found");
            }
        }

        /*
         * =========================================================
         * 3. EXTRACT GENDER
         * =========================================================
         *
         * Searches the complete OCR text.
         * Works even when Telugu/Hindi labels are present.
         *
         * IMPORTANT:
         * FEMALE is checked before MALE because FEMALE contains MALE.
         * =========================================================
         */

        String gender = extractGender(normalizedText);

        if (gender != null) {
            fields.put("gender", gender);
        } else {
            fields.put("genderError",
                    "Gender must be Male, Female or Other");
        }

        /*
         * =========================================================
         * 4. EXTRACT NAME
         * =========================================================
         */

        String name = extractName(normalizedText);

        if (name != null && !name.isEmpty()) {
            fields.put("name", name);
        } else {
            fields.put("nameError", "Name not found");
        }

        return fields;
    }

    /*
     * =============================================================
     * GENDER EXTRACTION
     * =============================================================
     */

    private String extractGender(String text) {

        /*
         * English gender words.
         *
         * FEMALE must come before MALE.
         */
        Pattern englishGenderPattern = Pattern.compile(
                "(?i)\\b(FEMALE|MALE|OTHER)\\b"
        );

        Matcher englishMatcher =
                englishGenderPattern.matcher(text);

        if (englishMatcher.find()) {

            String value = englishMatcher.group(1).toUpperCase();

            if (value.equals("MALE")) {
                return "Male";
            }

            if (value.equals("FEMALE")) {
                return "Female";
            }

            if (value.equals("OTHER")) {
                return "Other";
            }
        }

        /*
         * English single-letter gender values.
         * Example: Gender: M or Gender: F
         */
        Pattern shortGenderPattern = Pattern.compile(
                "(?i)(?:GENDER|SEX|लिंग|లింగము|లింగం)" +
                "\\s*[:\\-/]?\\s*([MF])\\b"
        );

        Matcher shortMatcher =
                shortGenderPattern.matcher(text);

        if (shortMatcher.find()) {

            String value = shortMatcher.group(1).toUpperCase();

            if (value.equals("M")) {
                return "Male";
            }

            if (value.equals("F")) {
                return "Female";
            }
        }

        /*
         * Telugu gender words.
         */
        if (text.contains("పురుషుడు")
                || text.contains("పురుష")
                || text.contains("మగ")) {

            return "Male";
        }

        if (text.contains("స్త్రీ")
                || text.contains("మహిళ")) {

            return "Female";
        }

        /*
         * Hindi gender words.
         */
        if (text.contains("पुरुष")
                || text.contains("लड़का")) {

            return "Male";
        }

        if (text.contains("महिला")
                || text.contains("स्त्री")
                || text.contains("लड़की")) {

            return "Female";
        }

        return null;
    }

    /*
     * =============================================================
     * NAME EXTRACTION
     * =============================================================
     */

    private String extractName(String text) {

        /*
         * First try the existing English "Name:" format.
         */
        Pattern namePattern = Pattern.compile(
                "(?im)^\\s*Name\\s*:\\s*(.+?)\\s*$"
        );

        Matcher nameMatcher = namePattern.matcher(text);

        if (nameMatcher.find()) {

            String name = nameMatcher.group(1).trim();

            if (!name.isEmpty()) {
                return name;
            }
        }

        /*
         * Fallback for OCR text where the name appears before DOB.
         *
         * Example:
         * Praveen Kumar Duddilla
         * DOB: 04/01/1981
         */
        Pattern beforeDobPattern = Pattern.compile(
                "(?is)(?:^|\\n)\\s*([A-Za-z][A-Za-z .]{2,60})" +
                "\\s*\\n\\s*(?:.*DOB|.*D\\.O\\.B)"
        );

        Matcher beforeDobMatcher =
                beforeDobPattern.matcher(text);

        if (beforeDobMatcher.find()) {

            String name = beforeDobMatcher.group(1).trim();

            if (!name.isEmpty()
                    && !name.equalsIgnoreCase("Government of India")) {

                return name;
            }
        }

        return null;
    }
}