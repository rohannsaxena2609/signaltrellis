package com.signaltrellis.capture;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@ConditionalOnProperty(name = "signaltrellis.ai.enabled", havingValue = "true")
public class OmniRouteFactExtractor {

    private static final String SYSTEM_INSTRUCTIONS = """
            Extract explicitly stated personal document facts from the supplied page text.
            Return only candidate facts. Never infer missing values or convert ambiguous dates.
            For every candidate, provide the exact page number and a short verbatim sourceQuote copied from that page.
            Include only dates, vendor or product names, amounts, currencies, warranty periods, and return deadlines.
            Confidence is a rough model estimate from 0 to 1, not calibrated; all output requires user review.
            If no supported facts exist, return an empty facts list.
            """;

    private final ChatClient chatClient;
    private final String model;

    public OmniRouteFactExtractor(ChatClient.Builder chatClientBuilder,
                                  @Value("${signaltrellis.ai.model:auto/best-free}") String model,
                                  @Value("${spring.ai.openai.api-key:}") String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Set SIGNALTRELLIS_AI_API_KEY to enable OmniRoute extraction");
        }
        this.chatClient = chatClientBuilder.build();
        this.model = model;
    }

    public List<FactSuggestion> extract(String documentText) {
        ModelExtractionResponse response = chatClient.prompt()
                .system(SYSTEM_INSTRUCTIONS)
                .user(documentText)
                .call()
                .entity(ModelExtractionResponse.class);
        if (response == null || response.facts() == null) return List.of();
        return response.facts();
    }

    public String model() {
        return model;
    }

    public record ModelExtractionResponse(List<FactSuggestion> facts) { }

    public record FactSuggestion(String type, String value, Integer sourcePage,
                                 String sourceQuote, BigDecimal confidence) { }
}
