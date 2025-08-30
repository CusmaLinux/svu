package co.edu.itp.svu.config;

import com.google.genai.Client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeminiClientConfig {

    @Value("${gemini.api-key}")
    private String apiKey;

    @Bean
    public Client geminiClient() {
        if (apiKey == null || apiKey.isBlank() || "YOUR_GOOGLE_API_KEY".equals(apiKey)) {
            return null;
        }

        return Client.builder().apiKey(apiKey).build();
    }
}
