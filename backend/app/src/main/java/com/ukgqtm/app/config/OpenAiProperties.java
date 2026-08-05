package com.ukgqtm.app.config;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.ai.openai")
public class OpenAiProperties {
    private String apiKey = "";
    private String model = "gpt-5.6-sol";
    private URI baseUrl = URI.create("https://api.openai.com/v1");
    private Duration connectTimeout = Duration.ofSeconds(10);
    private Duration readTimeout = Duration.ofMinutes(3);
    private int maxExtractedCharacters = 500_000;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public URI getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(URI baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }

    public int getMaxExtractedCharacters() {
        return maxExtractedCharacters;
    }

    public void setMaxExtractedCharacters(int maxExtractedCharacters) {
        this.maxExtractedCharacters = maxExtractedCharacters;
    }
}
