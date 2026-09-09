package docextract;

import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class AadhaarTextFormatter {

    public String format(Map<String, String> fields) {

        StringBuilder formattedText = new StringBuilder();

        if (fields.containsKey("name")) {
            formattedText.append("Name: ")
                    .append(fields.get("name"))
                    .append("\n");
        }

        if (fields.containsKey("dateOfBirth")) {
            formattedText.append("Date of Birth: ")
                    .append(fields.get("dateOfBirth"))
                    .append("\n");
        }

        if (fields.containsKey("gender")) {
            formattedText.append("Gender: ")
                    .append(fields.get("gender"))
                    .append("\n");
        }

        if (fields.containsKey("aadhaarNumber")) {
            formattedText.append("Aadhaar Number: ")
                    .append(fields.get("aadhaarNumber"))
                    .append("\n");
        }

        return formattedText.toString().trim();
    }
}