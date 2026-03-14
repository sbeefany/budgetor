package budgetor.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.io.ByteArrayResource;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionParserTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatResponse chatResponse;

    @Mock
    private Generation generation;

    @Captor
    private ArgumentCaptor<Prompt> promptCaptor;

    private TransactionParser transactionParser;

    @BeforeEach
    void setUp() {
        var systemPrompt = new ByteArrayResource("System {format}".getBytes(StandardCharsets.UTF_8));
        var userPrompt = new ByteArrayResource("User {text} {categories}".getBytes(StandardCharsets.UTF_8));
        
        transactionParser = new TransactionParserImpl(chatClient, systemPrompt, userPrompt);
    }

    @Test
    void shouldCallChatClientWithCorrectPrompt() {
        // given
        String userText = "Обед 350";
        List<String> categories = List.of("Еда", "Транспорт");

        String dummyResponse = "{\"amount\":350,\"categoryName\":\"Еда\",\"description\":\"Обед\",\"type\":\"EXPENSE\"}";
        when(chatClient.call(any(Prompt.class))).thenReturn(chatResponse);
        when(chatResponse.getResult()).thenReturn(generation);
        when(generation.getOutput()).thenReturn(new org.springframework.ai.chat.messages.AssistantMessage(dummyResponse));

        // when
        transactionParser.parse(userText, categories);

        // then
        verify(chatClient).call(promptCaptor.capture());
        Prompt capturedPrompt = promptCaptor.getValue();
        
        String promptContent = capturedPrompt.getContents();
        
        assertThat(promptContent)
                .contains("Обед 350")
                .contains("Еда")
                .contains("Транспорт");
    }
}
