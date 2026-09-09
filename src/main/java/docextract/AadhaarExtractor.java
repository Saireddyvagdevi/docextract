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
                "\\b(\\d{4}\\s*\\d{4}\\s*\\d{4})\\b"
        );

        Matcher aadhaarMatcher =
                aadhaarPattern.matcher(text);

        if (aadhaarMatcher.find()) {

            String aadhaar =
                    aadhaarMatcher.group(1)
                            .replaceAll("\\s+", " ")
                            .trim();

            fields.put("aadhaarNumber", aadhaar);
        }


        // =========================================================
        // 2. DATE OF BIRTH
        // =========================================================

        /*
         * Aadhaar DOB format:
         * DD/MM/YYYY
         *
         * Example:
         * 04/01/1981
         * 21/05/1997
         */

        Pattern dobPattern = Pattern.compile(
                "(?i)(?:DOB|D0B|Date\\s*of\\s*Birth)"
                + "\\s*[:\\-]?\\s*"
                + "(\\d{2}[\\/\\-.]\\d{2}[\\/\\-.]\\d{4})"
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
        // 3. GENDER
        // =========================================================

        Pattern genderPattern = Pattern.compile(
                "(?i)\\b(MALE|FEMALE)\\b"
        );

        Matcher genderMatcher =
                genderPattern.matcher(text);

        if (genderMatcher.find()) {

            String gender =
                    genderMatcher.group(1).toLowerCase();

            gender =
                    Character.toUpperCase(gender.charAt(0))
                    + gender.substring(1);

            fields.put("gender", gender);
        }


        // =========================================================
        // 4. NAME
        // =========================================================

        /*
         * Usually Aadhaar OCR gives:
         *
         * Praveen Kumar Duddilla
         * XXXXX/DOB: 04/01/1981
         *
         * So we take the line immediately before
         * the DOB line as the name.
         */

        Pattern namePattern = Pattern.compile(
                "(?m)^\\s*([A-Za-z][A-Za-z .'-]{2,})\\s*"
                + "\\n\\s*.*?"
                + "(?:DOB|D0B|Date\\s*of\\s*Birth)"
                + "\\s*[:\\-]?\\s*"
                + "\\d{2}[\\/\\-.]\\d{2}[\\/\\-.]\\d{4}"
        );

        Matcher nameMatcher =
                namePattern.matcher(text);

        if (nameMatcher.find()) {

            String name =
                    nameMatcher.group(1).trim();

            name = name.replaceAll("\\s+$", "");

            fields.put("name", name);
        }


        return fields;
    }
}

