package budgetor.service;

import budgetor.dto.ParsedTransactionDto;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.parser.BeanOutputParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
public class TransactionParserImpl implements TransactionParser {

    private final ChatClient chatClient;
    private final Resource systemPromptResource;
    private final Resource userPromptResource;

    public TransactionParserImpl(ChatClient chatClient,
                                 @Value("classpath:/prompts/parser-system-prompt.st") Resource systemPromptResource,
                                 @Value("classpath:/prompts/parser-user-prompt.st") Resource userPromptResource) {
        this.chatClient = chatClient;
        this.systemPromptResource = systemPromptResource;
        this.userPromptResource = userPromptResource;
    }

    @Override
    public ParsedTransactionDto parse(String text, List<String> availableCategories) {
        try {
            var outputParser = new BeanOutputParser<>(ParsedTransactionDto.class);
            String format = outputParser.getFormat();

            String systemPromptText = systemPromptResource.getContentAsString(StandardCharsets.UTF_8);
            var systemPromptTemplate = new PromptTemplate(systemPromptText);
            var systemMessageBody = systemPromptTemplate.createMessage(Map.of("format", format));
            var systemMessage = new SystemMessage(systemMessageBody.getContent());

            var userPromptTemplate = new PromptTemplate(userPromptResource);
            var userMessage = userPromptTemplate.createMessage(Map.of(
                    "text", text,
                    "categories", String.join(", ", availableCategories)
            ));

            var prompt = new Prompt(List.of(systemMessage, userMessage));

            var response = chatClient.call(prompt);
            String content = response.getResult().getOutput().getContent();

            return outputParser.parse(content);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse transaction text", e);
        }
    }
}
