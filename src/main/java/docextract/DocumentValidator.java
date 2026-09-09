package docextract;

import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class DocumentValidator {

    public Map<String, String> validateAadhaar(
            Map<String, String> fields) {

        // =====================================================
        // 1. VALIDATE NAME
        // =====================================================

        if (!fields.containsKey("name")
                || fields.get("name") == null
                || fields.get("name").isBlank()) {

            fields.put(
                    "nameError",
                    "Name is missing"
            );
        }

        // =====================================================
        // 2. VALIDATE DATE OF BIRTH
        // =====================================================

        if (!fields.containsKey("dob")
                || fields.get("dob") == null
                || !fields.get("dob")
                        .matches("\\d{2}/\\d{2}/\\d{4}")) {

            fields.put(
                    "dobError",
                    "Date of birth must be in DD/MM/YYYY format"
            );
        }

        // =====================================================
        // 3. VALIDATE GENDER
        // =====================================================

        if (!fields.containsKey("gender")
                || fields.get("gender") == null
                || !fields.get("gender")
                        .matches("(?i)Male|Female|Other")) {

            fields.put(
                    "genderError",
                    "Gender must be Male, Female or Other"
            );
        }

        // =====================================================
        // 4. VALIDATE AADHAAR NUMBER
        // =====================================================

        if (!fields.containsKey("aadhaarNumber")
                || fields.get("aadhaarNumber") == null
                || !fields.get("aadhaarNumber")
                        .matches(
                                "\\d{4}\\s\\d{4}\\s\\d{4}"
                                + "|X{4}\\sX{4}\\s\\d{4}"
                        )) {

            fields.put(
                    "aadhaarNumberError",
                    "Aadhaar number must contain 12 digits"
            );
        }

        return fields;
    }


    // =========================================================
    // PAN VALIDATION
    // =========================================================

    public Map<String, String> validatePan(
            Map<String, String> fields) {

        // =====================================================
        // 1. VALIDATE NAME
        // =====================================================

        if (!fields.containsKey("name")
                || fields.get("name") == null
                || fields.get("name").isBlank()) {

            fields.put(
                    "nameError",
                    "Name is missing"
            );
        }

        // =====================================================
        // 2. VALIDATE DATE OF BIRTH
        // =====================================================

        if (!fields.containsKey("dob")
                || fields.get("dob") == null
                || !fields.get("dob")
                        .matches("\\d{2}/\\d{2}/\\d{4}")) {

            fields.put(
                    "dobError",
                    "Date of birth must be in DD/MM/YYYY format"
            );
        }

        // =====================================================
        // 3. VALIDATE PAN NUMBER
        // =====================================================

        if (!fields.containsKey("panNumber")
                || fields.get("panNumber") == null
                || !fields.get("panNumber")
                        .matches("[A-Z]{5}\\d{4}[A-Z]")) {

            fields.put(
                    "panNumberError",
                    "PAN number must contain 10 characters"
            );
        }

        return fields;
    }
}