package docextract;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

@Service
public class OcrService {

    /*
     * Maximum image dimension used by OCR.
     *
     * This prevents very large uploaded images from consuming
     * too much memory on Render's free instance.
     */
    private static final int MAX_DIMENSION = 4000;

    /*
     * Windows:
     *   C:\Program Files\Tesseract-OCR\tessdata
     *
     * Render/Linux:
     *   /opt/tessdata
     *
     * We use /opt/tessdata because the Dockerfile explicitly
     * places eng, hin and tel traineddata files there.
     */
    private String getTessDataPath() {

        String os = System.getProperty("os.name")
                .toLowerCase(Locale.ROOT);

        if (os.contains("win")) {
            return "C:\\Program Files\\Tesseract-OCR\\tessdata";
        }

        return "/opt/tessdata";
    }

    public String extractText(File imageFile) {

        Tesseract tesseract = new Tesseract();

        /*
         * Tell Tess4J exactly where the language files are.
         */
        tesseract.setDatapath(getTessDataPath());

        /*
         * Support:
         * English + Hindi + Telugu
         *
         * This allows documents containing combinations such as:
         * Telugu + English
         * Hindi + English
         * English only
         */
        tesseract.setLanguage("eng+hin+tel");

        /*
         * LSTM OCR engine.
         */
        tesseract.setOcrEngineMode(1);

        /*
         * Sparse text layout.
         * Useful for Aadhaar/PAN documents where text may be
         * separated into different areas.
         */
        tesseract.setPageSegMode(11);

        /*
         * Preserve spaces between words.
         */
        tesseract.setVariable(
                "preserve_interword_spaces",
                "1"
        );

        /*
         * Tell Tesseract the approximate input resolution.
         */
        tesseract.setVariable(
                "user_defined_dpi",
                "200"
        );

        File processedImage = null;

        try {

            BufferedImage original =
                    ImageIO.read(imageFile);

            if (original == null) {
                throw new IOException(
                        "Unable to read image: " + imageFile.getName()
                );
            }

            /*
             * Resize only when necessary.
             *
             * We intentionally avoid 3x enlargement because it
             * caused unnecessary memory usage on Render.
             */
            BufferedImage image =
                    resizeForOcr(original);

            /*
             * If the image was resized, create a temporary PNG.
             */
            if (image != original) {

                processedImage = File.createTempFile(
                        "ocr-",
                        ".png"
                );

                ImageIO.write(
                        image,
                        "png",
                        processedImage
                );

            } else {

                processedImage = imageFile;
            }

            /*
             * Perform ONE OCR pass.
             *
             * This keeps processing time and memory usage lower.
             */
            String text =
                    tesseract.doOCR(processedImage);

            if (text == null) {
                return "";
            }

            return text.trim();

        } catch (IOException | TesseractException e) {

            throw new RuntimeException(
                    "OCR failed for file: "
                            + imageFile.getName(),
                    e
            );

        } finally {

            /*
             * Delete only our temporary processed image.
             * Never delete the original uploaded file.
             */
            if (processedImage != null
                    && processedImage != imageFile
                    && processedImage.exists()) {

                if (!processedImage.delete()) {
                    processedImage.deleteOnExit();
                }
            }
        }
    }

    /*
     * Resize image to a safe size for OCR.
     *
     * Large images are reduced.
     *
     * Smaller images can be enlarged up to 1.5x,
     * but never beyond MAX_DIMENSION.
     */
    private BufferedImage resizeForOcr(
            BufferedImage original) {

        int width = original.getWidth();
        int height = original.getHeight();

        int max = Math.max(width, height);

        double scale;

        if (max > MAX_DIMENSION) {

            scale =
                    (double) MAX_DIMENSION / max;

        } else if (max < 2500) {

            scale = Math.min(
                    1.5,
                    (double) MAX_DIMENSION / max
            );

        } else {

            /*
             * Image is already large enough.
             */
            return original;
        }

        int newWidth =
                Math.max(1, (int) Math.round(width * scale));

        int newHeight =
                Math.max(1, (int) Math.round(height * scale));

        BufferedImage resized =
                new BufferedImage(
                        newWidth,
                        newHeight,
                        BufferedImage.TYPE_INT_RGB
                );

        java.awt.Graphics2D graphics =
                resized.createGraphics();

        /*
         * Bilinear interpolation gives a good balance
         * between OCR quality and memory/CPU usage.
         */
        graphics.setRenderingHint(
                java.awt.RenderingHints.KEY_INTERPOLATION,
                java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR
        );

        graphics.setRenderingHint(
                java.awt.RenderingHints.KEY_RENDERING,
                java.awt.RenderingHints.VALUE_RENDER_SPEED
        );

        graphics.setRenderingHint(
                java.awt.RenderingHints.KEY_ANTIALIASING,
                java.awt.RenderingHints.VALUE_ANTIALIAS_OFF
        );

        graphics.drawImage(
                original,
                0,
                0,
                newWidth,
                newHeight,
                null
        );

        graphics.dispose();

        return resized;
    }
}