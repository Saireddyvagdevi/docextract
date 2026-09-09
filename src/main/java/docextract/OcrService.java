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

    /*
     * Keep all three languages.
     *
     * eng = English
     * hin = Hindi
     * tel = Telugu
     */
    private static final String OCR_LANGUAGES =
            "eng+hin+tel";

    public String extractText(File imageFile) {

        File processedImage = null;

        try {

            BufferedImage original =
                    ImageIO.read(imageFile);

            if (original == null) {

                throw new RuntimeException(
                        "Unable to read image"
                );
            }

            /*
             * =====================================================
             * MEMORY-SAFE IMAGE PROCESSING
             * =====================================================
             *
             * The old version enlarged every image 3X.
             *
             * Example:
             *
             * 2500 x 3500
             *
             * became:
             *
             * 7500 x 10500
             *
             * This consumes a very large amount of RAM and caused:
             *
             * java.lang.OutOfMemoryError: Java heap space
             *
             * on Render.
             *
             * Instead:
             *
             * - Small images are enlarged only up to 1.5X.
             * - Large images are kept at their original size.
             * - Very large images are reduced to a safe maximum.
             */

            BufferedImage imageForOCR =
                    prepareImageForOCR(original);

            /*
             * We create a temporary PNG because Tess4J/Tesseract
             * works reliably with the image file.
             */

            processedImage =
                    File.createTempFile(
                            "ocr-image-",
                            ".png"
                    );

            ImageIO.write(
                    imageForOCR,
                    "png",
                    processedImage
            );

            /*
             * Release image references as early as possible.
             */

            imageForOCR.flush();
            original.flush();

            /*
             * =====================================================
             * SINGLE OCR PASS
             * =====================================================
             *
             * PSM 11 works well for documents such as:
             *
             * Aadhaar
             * PAN
             * multilingual documents
             *
             * A single OCR pass is considerably lighter than
             * running Tesseract twice.
             */

            return performOCR(
                    processedImage,
                    11
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Image processing failed: "
                            + e.getMessage(),
                    e
            );

        } finally {

            if (processedImage != null
                    && processedImage.exists()) {

                processedImage.delete();
            }
        }
    }

    // =============================================================
    // MEMORY-SAFE IMAGE PREPARATION
    // =============================================================

    private BufferedImage prepareImageForOCR(
            BufferedImage original) {

        int originalWidth =
                original.getWidth();

        int originalHeight =
                original.getHeight();

        /*
         * Maximum dimension allowed for OCR.
         *
         * This prevents very large PDF pages from consuming
         * excessive Java heap memory.
         */

        final int MAX_DIMENSION = 4000;

        /*
         * If the image is already large enough,
         * do NOT enlarge it.
         */

        int largestDimension =
                Math.max(
                        originalWidth,
                        originalHeight
                );

        if (largestDimension >= MAX_DIMENSION) {

            double scale =
                    (double) MAX_DIMENSION
                            / largestDimension;

            int newWidth =
                    Math.max(
                            1,
                            (int) (
                                    originalWidth
                                            * scale
                            )
                    );

            int newHeight =
                    Math.max(
                            1,
                            (int) (
                                    originalHeight
                                            * scale
                            )
                    );

            return resizeImage(
                    original,
                    newWidth,
                    newHeight
            );
        }

        /*
         * For smaller images, allow a maximum 1.5X enlargement.
         *
         * This helps OCR quality without the huge memory usage
         * caused by 3X enlargement.
         */

        int newWidth =
                Math.min(
                        originalWidth * 3 / 2,
                        MAX_DIMENSION
                );

        int newHeight =
                Math.min(
                        originalHeight * 3 / 2,
                        MAX_DIMENSION
                );

        /*
         * If enlargement is unnecessary,
         * simply return the original image.
         */

        if (newWidth <= originalWidth
                && newHeight <= originalHeight) {

            return original;
        }

        return resizeImage(
                original,
                newWidth,
                newHeight
        );
    }

    // =============================================================
    // IMAGE RESIZE
    // =============================================================

    private BufferedImage resizeImage(
            BufferedImage original,
            int width,
            int height) {

        BufferedImage resized =
                new BufferedImage(
                        width,
                        height,
                        BufferedImage.TYPE_INT_RGB
                );

        Graphics2D graphics =
                resized.createGraphics();

        graphics.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR
        );

        graphics.setRenderingHint(
                RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_SPEED
        );

        graphics.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_OFF
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

        return resized;
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

        /*
         * IMPORTANT:
         *
         * Keep English + Hindi + Telugu.
         *
         * This allows documents such as:
         *
         * Telugu + English Aadhaar
         * Hindi + English Aadhaar
         * English PAN
         * Other multilingual documents
         */

        tesseract.setLanguage(
                OCR_LANGUAGES
        );

        tesseract.setPageSegMode(
                pageSegmentationMode
        );

        /*
         * LSTM OCR engine.
         */

        tesseract.setOcrEngineMode(1);

        /*
         * Preserve spacing between words.
         */

        tesseract.setVariable(
                "preserve_interword_spaces",
                "1"
        );

        /*
         * Disable unnecessary OCR features that can consume
         * additional processing resources.
         */

        tesseract.setVariable(
                "user_defined_dpi",
                "200"
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

