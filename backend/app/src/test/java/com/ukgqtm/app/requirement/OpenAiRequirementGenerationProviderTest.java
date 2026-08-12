package com.ukgqtm.app.requirement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ukgqtm.ai.api.RequirementGenerationProvider.GenerationRequest;
import com.ukgqtm.app.config.OpenAiProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OpenAiRequirementGenerationProviderTest {
    @Test
    void callsResponsesApiOnTheBackendAndParsesStructuredOutput() {
        OpenAiProperties properties = new OpenAiProperties();
        properties.setApiKey("server-only-key");
        properties.setModel("configured-model");
        RestClient.Builder builder = RestClient.builder().baseUrl(properties.getBaseUrl().toString());
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiRequirementGenerationProvider provider =
                new OpenAiRequirementGenerationProvider(properties, new ObjectMapper(), builder.build());
        server.expect(requestTo("https://api.openai.com/v1/responses"))
                .andExpect(header("Authorization", "Bearer server-only-key"))
                .andExpect(content().string(containsString(
                        "Do not create\\nstandalone requirements from those design details")))
                .andExpect(content().string(containsString(
                        "never how Boomi or another\\nimplementation technology performs it")))
                .andRespond(withSuccess(
                        """
                        {"output":[{"type":"message","content":[{"type":"output_text","text":"{\\"requirements\\":[{\\"header\\":\\"Clock in\\",\\"description\\":\\"Capture employee time.\\",\\"acceptanceCriteria\\":[\\"The time is stored.\\"],\\"assumptions\\":[],\\"dependencies\\":[]}]}"}]}]}
                        """,
                        MediaType.APPLICATION_JSON));

        var generated = provider.generate(new GenerationRequest("requirements.csv", "Clock in, Capture time"));

        assertThat(generated.model()).isEqualTo("configured-model");
        assertThat(generated.requirements()).singleElement().satisfies(requirement -> {
            assertThat(requirement.header()).isEqualTo("Clock in");
            assertThat(requirement.acceptanceCriteria()).containsExactly("The time is stored.");
        });
        server.verify();
    }

    @Test
    void refusesToCallOpenAiWithoutServerConfiguration() {
        var provider = new OpenAiRequirementGenerationProvider(
                new OpenAiProperties(), new ObjectMapper(), RestClient.builder());

        assertThatThrownBy(() -> provider.generate(new GenerationRequest("requirements.csv", "content")))
                .isInstanceOf(RequirementGenerationException.class)
                .hasMessageContaining("not configured");
    }
}
