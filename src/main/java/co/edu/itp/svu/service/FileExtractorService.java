package co.edu.itp.svu.service;

import co.edu.itp.svu.domain.ArchivoAdjunto;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FileExtractorService {

    private final Logger log = LoggerFactory.getLogger(FileExtractorService.class);

    @Value("${application.file.upload-dir}")
    private String uploadDir;

    public String extractTextFromAttachments(Set<ArchivoAdjunto> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return "";
        }

        return attachments.stream().map(this::extractTextFromFile).collect(Collectors.joining("\n\n---\n\n"));
    }

    public String extractTextFromFile(ArchivoAdjunto attachment) {
        String fileName = new File(attachment.getUrlArchivo()).getName();

        try {
            Path filePath = Paths.get(uploadDir, fileName);
            File file = filePath.toFile();

            if (!file.exists()) {
                log.warn("File not found at path: {}", filePath);
                return "";
            }

            String mimeType = attachment.getTipo();
            if (mimeType == null) {
                try {
                    mimeType = Files.probeContentType(filePath);
                } catch (IOException e) {
                    log.warn("Could not determine MIME type for file: {}", fileName, e);
                }
            }

            log.debug("Extracting text from file: {} with MIME type: {}", fileName, mimeType);

            if ("application/pdf".equalsIgnoreCase(mimeType)) {
                return extractTextFromPdf(file);
            } else if (mimeType != null && mimeType.startsWith("image/")) {
                return extractTextFromImage(file);
            } else if (mimeType != null && mimeType.startsWith("text/")) {
                return extractTextFromTextFile(file);
            } else {
                log.warn("Unsupported file type for text extraction: {}. Skipping file: {}", mimeType, fileName);
                return "";
            }
        } catch (Exception e) {
            log.error("Error processing file {}: {}", fileName, e.getMessage(), e);
            return "";
        }
    }

    private String extractTextFromPdf(File file) {
        try (PDDocument document = org.apache.pdfbox.Loader.loadPDF(file)) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            return pdfStripper.getText(document);
        } catch (IOException e) {
            log.error("Error extracting text from PDF file {}: {}", file.getName(), e.getMessage(), e);
            return "";
        }
    }

    private String extractTextFromTextFile(File file) {
        try {
            return Files.readString(file.toPath());
        } catch (IOException e) {
            log.error("Error reading text file {}: {}", file.getName(), e.getMessage(), e);
            return "";
        }
    }

    /**
     * Extracts text from an image file using Tesseract OCR.
     * This method can be adapted to dynamically choose the language.
     *
     * @param file The image file to process.
     * @return The extracted text as a String, or an empty string if an error occurs.
     */
    private String extractTextFromImage(File file) {
        String englishText = ocrWithLanguage(file, "eng");
        String spanishText = ocrWithLanguage(file, "spa");

        return englishText + "\n" + spanishText;
    }

    /**
     * Performs OCR on a given file using the specified language.
     *
     * @param file     The image file.
     * @param language The language to use for OCR (e.g., "eng" or "spa").
     * @return The extracted text.
     */
    private String ocrWithLanguage(File file, String language) {
        Tesseract tesseract = new Tesseract();
        try {
            URL tessdataURL = getClass().getResource("/tessdata");
            if (tessdataURL == null) {
                log.error(
                    "FATAL: Cannot find the 'tessdata' directory. Please ensure 'src/main/resources/tessdata' exists and contains your .traineddata files."
                );
                return "";
            }

            File tessdataFolder = new File(tessdataURL.toURI());

            tesseract.setDatapath(tessdataFolder.getAbsolutePath());

            tesseract.setLanguage(language);

            log.info("Performing OCR on {} with language: {}", file.getName(), language);
            return tesseract.doOCR(file);
        } catch (TesseractException e) {
            log.error("Error extracting text from image {} with language {}: {}", file.getName(), language, e.getMessage(), e);
            return "";
        } catch (URISyntaxException e) {
            log.error("Error converting tessdata folder path. This can happen with unusual characters in the project path.", e);
            return "";
        }
    }
}
