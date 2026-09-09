package docextract;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

@Service
public class PanExtractor {

    public Map<String, String> extractFields(String text) {

        Map<String, String> fields = new HashMap<>();

        if (text == null || text.isBlank()) {
            return fields;
        }

        text = text.replace("\r", "");

        // =========================================================
        // 1. PAN NUMBER
        // =========================================================

        Pattern panPattern = Pattern.compile(
                "\\b[A-Z]{5}\\d{4}[A-Z]\\b",
                Pattern.CASE_INSENSITIVE
        );

        Matcher panMatcher = panPattern.matcher(text);

        if (panMatcher.find()) {

            String panNumber =
                    panMatcher.group()
                            .toUpperCase()
                            .trim();

            fields.put("panNumber", panNumber);

        } else {

            // OCR may insert one extra letter.
            // Example:
            // FEGPDS5131E
            // Correct:
            // FEGPD5131E

            Pattern ocrPanPattern = Pattern.compile(
                    "\\b([A-Z]{6})(\\d{4})([A-Z])\\b",
                    Pattern.CASE_INSENSITIVE
            );

            Matcher ocrPanMatcher =
                    ocrPanPattern.matcher(text);

            if (ocrPanMatcher.find()) {

                String sixLetters =
                        ocrPanMatcher.group(1)
                                .toUpperCase();

                String digits =
                        ocrPanMatcher.group(2);

                String lastLetter =
                        ocrPanMatcher.group(3)
                                .toUpperCase();

                String correctedPan =
                        sixLetters.substring(0, 5)
                                + digits
                                + lastLetter;

                fields.put(
                        "panNumber",
                        correctedPan
                );
            }
        }

        // =========================================================
        // 2. DATE OF BIRTH
        // =========================================================

        Pattern dobPattern = Pattern.compile(
                "\\b(\\d{2}[\\/\\-.]\\d{2}[\\/\\-.]\\d{4})\\b"
        );

        Matcher dobMatcher =
                dobPattern.matcher(text);

        if (dobMatcher.find()) {

            String dob =
                    dobMatcher.group(1)
                            .replace("-", "/")
                            .replace(".", "/")
                            .trim();

            fields.put("dob", dob);
        }

        // =========================================================
        // 3. LABEL-BASED NAME
        // =========================================================

        /*
         * Handles:
         *
         * Name
         * NITU BHURVE
         *
         * Father's Name
         * DURGA PRASAD DHURVE
         */

        Pattern nameLabelPattern = Pattern.compile(
                "(?i)(?:^|\\n)\\s*"
                + "(?:Name\\s*/\\s*Name|Name)"
                + "\\s*\\n\\s*"
                + "([A-Za-z][A-Za-z .'-]{2,})"
        );

        Matcher nameLabelMatcher =
                nameLabelPattern.matcher(text);

        if (nameLabelMatcher.find()) {

            String name =
                    cleanName(nameLabelMatcher.group(1));

            if (isValidPersonName(name)) {

                fields.put("name", name);
            }
        }

        // =========================================================
        // 4. LABEL-BASED FATHER NAME
        // =========================================================

        Pattern fatherLabelPattern = Pattern.compile(
                "(?i)(?:Father'?s\\s*Name)"
                + "\\s*\\n\\s*"
                + "([A-Za-z][A-Za-z .'-]{2,})"
        );

        Matcher fatherLabelMatcher =
                fatherLabelPattern.matcher(text);

        if (fatherLabelMatcher.find()) {

            String fatherName =
                    cleanName(
                            fatherLabelMatcher.group(1)
                    );

            if (isValidPersonName(fatherName)) {

                fields.put(
                        "fatherName",
                        fatherName
                );
            }
        }

        // =========================================================
        // 5. FIND VALID NAME LINES BEFORE DOB
        // =========================================================

        if (fields.containsKey("dob")) {

            String dob =
                    fields.get("dob");

            int dobIndex =
                    text.indexOf(dob);

            if (dobIndex > 0) {

                String beforeDob =
                        text.substring(0, dobIndex);

                String[] lines =
                        beforeDob.split("\\n");

                List<String> candidates =
                        new ArrayList<>();

                for (String rawLine : lines) {

                    String line =
                            cleanName(rawLine);

                    if (!isValidPersonName(line)) {
                        continue;
                    }

                    candidates.add(line);
                }

                /*
                 * For a normal PAN card:
                 *
                 * NAME
                 * FATHER NAME
                 * DOB
                 *
                 * Therefore the last two valid person-name
                 * lines before DOB are the best candidates.
                 */

                if (!fields.containsKey("name")
                        && candidates.size() >= 1) {

                    fields.put(
                            "name",
                            candidates.get(
                                    candidates.size() - 1
                            )
                    );
                }

                if (!fields.containsKey("fatherName")
                        && candidates.size() >= 2) {

                    fields.put(
                            "fatherName",
                            candidates.get(
                                    candidates.size() - 2
                            )
                    );
                }
            }
        }

        return fields;
    }

    // =============================================================
    // CLEAN NAME
    // =============================================================

    private String cleanName(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replaceAll("\\s+", " ")
                .trim();
    }

    // =============================================================
    // VALID PERSON NAME
    // =============================================================

    private boolean isValidPersonName(String value) {

        if (value == null || value.isBlank()) {
            return false;
        }

        String name =
                value.trim();

        // Must contain only English name characters
        if (!name.matches(
                "[A-Za-z]+(?:[ .'-][A-Za-z]+)*"
        )) {
            return false;
        }

        // Avoid very short OCR garbage
        if (name.length() < 3) {
            return false;
        }

        // Avoid known PAN headings
        String upper =
                name.toUpperCase();

        if (upper.equals("INCOME TAX")) {
            return false;
        }

        if (upper.equals("INCOME TAX DEPARTMENT")) {
            return false;
        }

        if (upper.equals("DEPARTMENT")) {
            return false;
        }

        if (upper.equals("GOVT OF INDIA")) {
            return false;
        }

        if (upper.equals("GOVT. OF INDIA")) {
            return false;
        }

        if (upper.equals("GOVERNMENT OF INDIA")) {
            return false;
        }

        if (upper.equals("PERMANENT ACCOUNT NUMBER")) {
            return false;
        }

        if (upper.equals("PERMANENT ACCOUNT NUMBER CARD")) {
            return false;
        }

        if (upper.equals("SIGNATURE")) {
            return false;
        }

        if (upper.equals("NAME")) {
            return false;
        }

        if (upper.equals("FATHER'S NAME")) {
            return false;
        }

        if (upper.equals("FATHERS NAME")) {
            return false;
        }

        return true;
    }
}