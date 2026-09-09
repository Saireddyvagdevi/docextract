package docextract;

import org.springframework.stereotype.Service;

@Service
public class DataMasker {

    public String maskAadhaar(String aadhaarNumber) {

        if (aadhaarNumber == null || aadhaarNumber.isBlank()) {
            return aadhaarNumber;
        }

        // Already masked
        if (aadhaarNumber.startsWith("XXXX")) {
            return aadhaarNumber;
        }

        // Remove spaces
        String digits = aadhaarNumber.replaceAll("\\s+", "");

        // Mask first 8 digits
        if (digits.matches("\\d{12}")) {
            return "XXXX XXXX " + digits.substring(8);
        }

        return aadhaarNumber;
    }
}