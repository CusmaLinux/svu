package co.edu.itp.svu.service;

import co.edu.itp.svu.domain.Oficina;
import co.edu.itp.svu.domain.Pqrs;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GeminiService {

    private final Logger log = LoggerFactory.getLogger(GeminiService.class);

    private final Client geminiClient;

    @Autowired
    public GeminiService(Client geminiClient) {
        this.geminiClient = geminiClient;
    }

    public String suggestOffice(Pqrs pqrs, List<Oficina> offices, String fileText) {
        if (geminiClient == null) {
            log.error("Gemini Client is not initialized. Cannot suggest office.");
            return null;
        }

        try {
            String officeContext = offices
                .stream()
                .map(office -> String.format("- %s: %s", office.getNombre(), office.getDescripcion()))
                .collect(Collectors.joining("\n"));

            String promptText = String.format(
                "You are an expert assistant for a Public University's Unique Window System (Sistema de Ventanilla Única). " +
                "Your task is to suggest the most appropriate office to handle a given PQRS (Petition, Complaint, Claim, or Suggestion).\n\n" +
                "Analyze the following PQRS details:\n" +
                "- Type: %s\n" +
                "- Title: %s\n" +
                "- Description: %s\n\n" +
                "The following text was extracted from attached files:\n%s\n\n" +
                "Here is the list of available offices and their functions:\n%s\n\n" +
                "Based on all the information, which office should handle this PQRS? " +
                "Return only the exact name of the suggested office from the list provided. Do not add any extra text or explanation.",
                pqrs.getType(),
                pqrs.getTitulo(),
                pqrs.getDescripcion(),
                fileText.isEmpty() ? "No files attached." : fileText,
                officeContext
            );

            GenerateContentResponse response = geminiClient.models.generateContent("gemini-2.5-flash", promptText, null);

            String suggestedOfficeName = response.text();

            log.debug("Gemini suggested office: {}", suggestedOfficeName);

            return suggestedOfficeName;
        } catch (Exception e) {
            log.error("Error while calling Gemini API: {}", e.getMessage(), e);
            return null;
        }
    }
}
