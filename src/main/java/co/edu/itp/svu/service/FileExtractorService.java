package co.edu.itp.svu.service;

import co.edu.itp.svu.domain.ArchivoAdjunto;
import java.io.File;
import java.io.IOException;
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

    private String extractTextFromImage(File file) {
        Tesseract tesseract = new Tesseract();
        try {
            return tesseract.doOCR(file);
        } catch (TesseractException e) {
            log.error("Error extracting text from image {}: {}", file.getName(), e.getMessage(), e);
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
}
