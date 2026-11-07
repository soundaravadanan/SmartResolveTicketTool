package com.smartresolve.service;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.smartresolve.domain.TicketCategory;
import com.smartresolve.domain.TicketPriority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

class TicketTriageServiceTest {

    private final ChatClient.Builder chatClientBuilder = mock(ChatClient.Builder.class);
    private final ChatClient chatClient = mock(ChatClient.class);
    private final ChatClient.ChatClientRequestSpec request = mock(ChatClient.ChatClientRequestSpec.class);
    private final ChatClient.CallResponseSpec response = mock(ChatClient.CallResponseSpec.class);
    private TicketTriageService service;

    @BeforeEach
    void setUp() {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(request);
        when(request.system(anyString())).thenReturn(request);
        when(request.user(ArgumentMatchers.<Consumer<ChatClient.PromptUserSpec>>any())).thenReturn(request);
        when(request.options(isA(OllamaOptions.class))).thenReturn(request);
        when(request.call()).thenReturn(response);
        service = new TicketTriageService(chatClientBuilder);
    }

    @Test
    void shouldReturnStructuredTriageRecommendation() {
        TicketTriageRecommendation expected = new TicketTriageRecommendation(
                "Payroll login issue",
                TicketCategory.ACCESS_ISSUE,
                TicketPriority.HIGH,
                List.of("Check whether the account is locked"),
                "Support is reviewing the login issue."
        );
        when(response.entity(TicketTriageRecommendation.class)).thenReturn(expected);

        TicketTriageRecommendation actual = service.recommend("Payroll login", "Invalid credentials");

        assertEquals(expected, actual);
        ArgumentCaptor<OllamaOptions> options = ArgumentCaptor.forClass(OllamaOptions.class);
        verify(request).options(options.capture());
        assertEquals("json", options.getValue().getFormat());
    }

    @Test
    void shouldReportUnavailableModelWhenProviderCallFails() {
        when(response.entity(TicketTriageRecommendation.class))
                .thenThrow(new ResourceAccessException("Connection refused"));

        TicketTriageException exception = assertThrows(
                TicketTriageException.class,
                () -> service.recommend("Payroll login", "Invalid credentials")
        );

        assertTrue(exception.getMessage().contains("Ollama is running"));
    }

    @Test
    void shouldRejectIncompleteModelOutput() {
        when(response.entity(TicketTriageRecommendation.class)).thenReturn(null);

        TicketTriageException exception = assertThrows(
                TicketTriageException.class,
                () -> service.recommend("Payroll login", "Invalid credentials")
        );

        assertEquals("AI returned no recommendation", exception.getMessage());
        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());
    }

    @Test
    void shouldIdentifyAnEmptyTroubleshootingStep() {
        when(response.entity(TicketTriageRecommendation.class)).thenReturn(
                new TicketTriageRecommendation(
                        "Payroll login issue",
                        TicketCategory.ACCESS_ISSUE,
                        TicketPriority.HIGH,
                        List.of(" "),
                        "Support is reviewing the login issue."
                )
        );

        TicketTriageException exception = assertThrows(
                TicketTriageException.class,
                () -> service.recommend("Payroll login", "Invalid credentials")
        );

        assertEquals("AI recommendation contains an empty troubleshooting step", exception.getMessage());
        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());
    }

    @Test
    void shouldIdentifyAnOverlongSummary() {
        when(response.entity(TicketTriageRecommendation.class)).thenReturn(
                new TicketTriageRecommendation(
                        "x".repeat(1001),
                        TicketCategory.ACCESS_ISSUE,
                        TicketPriority.HIGH,
                        List.of("Check whether the account is locked"),
                        "Support is reviewing the login issue."
                )
        );

        TicketTriageException exception = assertThrows(
                TicketTriageException.class,
                () -> service.recommend("Payroll login", "Invalid credentials")
        );

        assertEquals("AI summary is too long; please retry with a shorter ticket description", exception.getMessage());
    }

    @Test
    void shouldConvertMalformedJsonIntoActionableTriageError() {
        JsonMappingException malformedJson = JsonMappingException.from(
                (com.fasterxml.jackson.core.JsonParser) null,
                "Unexpected character in troubleshootingSteps"
        );
        when(response.entity(TicketTriageRecommendation.class))
                .thenThrow(new IllegalArgumentException("Invalid AI output", malformedJson));

        TicketTriageException exception = assertThrows(
                TicketTriageException.class,
                () -> service.recommend("Forget password", "Forget my password, need to reset")
        );

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());
        assertTrue(exception.getMessage().contains("malformed JSON"));
    }
}
