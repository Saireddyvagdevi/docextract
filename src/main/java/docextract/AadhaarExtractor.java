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

        if (text == null || text.isBlank()) {
            return fields;
        }

        text = text.replace("\r", "");

        // =========================================================
        // 1. AADHAAR NUMBER
        // =========================================================

        Pattern aadhaarPattern = Pattern.compile(
                "(?<!\\d)(\\d{4}\\s*\\d{4}\\s*\\d{4})(?!\\d)"
        );

        Matcher aadhaarMatcher = aadhaarPattern.matcher(text);

        if (aadhaarMatcher.find()) {
            String aadhaar = aadhaarMatcher.group(1)
                    .replaceAll("\\s+", " ")
                    .trim();

            fields.put("aadhaarNumber", aadhaar);
        }


        // =========================================================
        // 2. DATE OF BIRTH
        // =========================================================

        Pattern dobPattern = Pattern.compile(
                "(?i)(?:DOB|D0B|Date\\s*of\\s*Birth)"
                        + "\\s*[:\\-]?\\s*"
                        + "(\\d{2}[\\/\\-.]\\d{2}[\\/\\-.]\\d{4})"
        );

        Matcher dobMatcher = dobPattern.matcher(text);

        if (dobMatcher.find()) {

            String dob = dobMatcher.group(1)
                    .replace("-", "/")
                    .replace(".", "/")
                    .trim();

            fields.put("dob", dob);
        }


        // =========================================================
        // 3. GENDER
        // =========================================================

        String gender = extractGender(text);

        if (gender != null) {
            fields.put("gender", gender);
        } else {
            fields.put(
                    "genderError",
                    "Gender must be Male, Female or Other"
            );
        }


        // =========================================================
        // 4. NAME
        // =========================================================

        String name = extractName(text);

        if (name != null) {
            fields.put("name", name);
        }


        return fields;
    }


    // =============================================================
    // GENDER EXTRACTION
    // =============================================================

    private String extractGender(String text) {

        // ---------------------------------------------------------
        // English
        // ---------------------------------------------------------

        if (Pattern.compile("(?i)\\bMALE\\b").matcher(text).find()) {
            return "Male";
        }

        if (Pattern.compile("(?i)\\bFEMALE\\b").matcher(text).find()) {
            return "Female";
        }


        // ---------------------------------------------------------
        // Telugu
        // ---------------------------------------------------------

        // పురుషుడు = Male
        // OCR variation seen in your document: వురుషుడు
        if (text.contains("పురుషుడు")
                || text.contains("వురుషుడు")
                || text.contains("పురుష")) {
            return "Male";
        }

        // స్త్రీ / మహిళ = Female
        if (text.contains("స్త్రీ")
                || text.contains("మహిళ")) {
            return "Female";
        }


        // ---------------------------------------------------------
        // Hindi
        // ---------------------------------------------------------

        // पुरुष = Male
        if (text.contains("पुरुष")) {
            return "Male";
        }

        // महिला / स्त्री = Female
        if (text.contains("महिला")
                || text.contains("स्त्री")) {
            return "Female";
        }


        return null;
    }


    // =============================================================
    // NAME EXTRACTION
    // =============================================================

    private String extractName(String text) {

        /*
         * IMPORTANT:
         *
         * Your Telugu Aadhaar OCR contains garbage:
         *
         * ROO BmEe
         * ...
         * స్రవీణ్‌ కుమార్‌
         * Praveen Kumar Duddilla
         * @&VDOB: 04/01/1981
         *
         * Therefore we should NOT simply take the first
         * English-looking line.
         *
         * We search for the English name immediately before
         * the DOB information.
         */


        // ---------------------------------------------------------
        // Strategy 1:
        // English name directly before DOB
        // ---------------------------------------------------------

        Pattern nameBeforeDob = Pattern.compile(
                "(?mi)^\\s*"
                        + "([A-Za-z][A-Za-z .'-]{2,})"
                        + "\\s*\\n\\s*"
                        + "[^\\n]*"
                        + "(?:DOB|D0B|Date\\s*of\\s*Birth)"
                        + "\\s*[:\\-]?\\s*"
                        + "\\d{2}[\\/\\-.]\\d{2}[\\/\\-.]\\d{4}"
        );

        Matcher matcher = nameBeforeDob.matcher(text);

        if (matcher.find()) {

            String name = cleanName(matcher.group(1));

            if (isValidName(name)) {
                return name;
            }
        }


        // ---------------------------------------------------------
        // Strategy 2:
        // Search backwards from DOB for the nearest valid
        // English name line.
        // ---------------------------------------------------------

        Pattern dobPositionPattern = Pattern.compile(
                "(?i)(?:DOB|D0B|Date\\s*of\\s*Birth)"
        );

        Matcher dobPositionMatcher =
                dobPositionPattern.matcher(text);

        if (dobPositionMatcher.find()) {

            String beforeDob =
                    text.substring(0, dobPositionMatcher.start());

            String[] lines = beforeDob.split("\\n");

            /*
             * Search the previous several lines.
             * This avoids selecting "ROO BmEe" at the top.
             */

            for (int i = lines.length - 1;
                 i >= Math.max(0, lines.length - 8);
                 i--) {

                String candidate =
                        cleanName(lines[i]);

                if (isValidName(candidate)) {
                    return candidate;
                }
            }
        }


        // ---------------------------------------------------------
        // Strategy 3:
        // Look for a normal multi-word English name anywhere
        // in the OCR.
        // ---------------------------------------------------------

        Pattern generalNamePattern = Pattern.compile(
                "(?m)^\\s*"
                        + "([A-Z][A-Za-z]+"
                        + "(?:\\s+[A-Z][A-Za-z]+)+)"
                        + "\\s*$"
        );

        Matcher generalMatcher =
                generalNamePattern.matcher(text);

        while (generalMatcher.find()) {

            String candidate =
                    cleanName(generalMatcher.group(1));

            if (isValidName(candidate)) {
                return candidate;
            }
        }


        return null;
    }


    // =============================================================
    // NAME CLEANING
    // =============================================================

    private String cleanName(String name) {

        if (name == null) {
            return null;
        }

        name = name.trim();

        // Remove OCR punctuation at beginning/end
        name = name.replaceAll("^[^A-Za-z]+", "");
        name = name.replaceAll("[^A-Za-z.' -]+$", "");

        // Normalize spaces
        name = name.replaceAll("\\s+", " ").trim();

        return name;
    }


    // =============================================================
    // NAME VALIDATION
    // =============================================================

    private boolean isValidName(String name) {

        if (name == null || name.isBlank()) {
            return false;
        }

        // Too short
        if (name.length() < 4) {
            return false;
        }

        // Must contain at least two alphabetic words
        String[] words = name.split("\\s+");

        if (words.length < 2) {
            return false;
        }

        // Every character should belong to a normal English name
        if (!name.matches("[A-Za-z][A-Za-z.' -]*")) {
            return false;
        }

        /*
         * Reject obvious OCR garbage.
         *
         * This is specifically useful for your:
         * ROO BmEe
         *
         * but we don't rely only on this list.
         */

        String lower = name.toLowerCase();

        if (lower.equals("roo bmee")) {
            return false;
        }

        if (lower.equals("roo bmee")) {
            return false;
        }

        // Name should contain mostly alphabetic characters
        long letters = name.chars()
                .filter(Character::isLetter)
                .count();

        long total = name.chars()
                .filter(c -> Character.isLetter(c)
                        || c == ' '
                        || c == '.'
                        || c == '-'
                        || c == '\'')
                .count();

        if (total == 0) {
            return false;
        }

        double ratio = (double) letters / total;

        return ratio >= 0.70;
    }
}