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
    private final Resource visionSystemPromptResource;
    private final Resource visionUserPromptResource;

    public TransactionParserImpl(ChatClient chatClient,
                                 @Value("classpath:/prompts/parser-system-prompt.st") Resource systemPromptResource,
                                 @Value("classpath:/prompts/parser-user-prompt.st") Resource userPromptResource,
                                 @Value("classpath:/prompts/vision-parser-system-prompt.st") Resource visionSystemPromptResource,
                                 @Value("classpath:/prompts/vision-parser-user-prompt.st") Resource visionUserPromptResource) {
        this.chatClient = chatClient;
        this.systemPromptResource = systemPromptResource;
        this.userPromptResource = userPromptResource;
        this.visionSystemPromptResource = visionSystemPromptResource;
        this.visionUserPromptResource = visionUserPromptResource;
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

    @Override
    public ParsedTransactionDto parse(Resource imageResource, List<String> availableCategories) {
        try {
            var outputParser = new BeanOutputParser<>(ParsedTransactionDto.class);
            String format = outputParser.getFormat();

            String systemPromptText = visionSystemPromptResource.getContentAsString(StandardCharsets.UTF_8);
            var systemPromptTemplate = new PromptTemplate(systemPromptText);
            var systemMessageBody = systemPromptTemplate.createMessage(Map.of("format", format));
            var systemMessage = new SystemMessage(systemMessageBody.getContent());

            var userPromptTemplate = new PromptTemplate(visionUserPromptResource);
            var userMessage = userPromptTemplate.createMessage(Map.of(
                    "categories", String.join(", ", availableCategories)
            ));

            // Note: Media integration in Spring AI UserMessage depends on the constructor or method used.
            // Using UserMessage with Media for vision.
            var visionMessage = new org.springframework.ai.chat.messages.UserMessage(
                    userMessage.getContent(),
                    List.of(new org.springframework.ai.chat.messages.Media(
                            org.springframework.util.MimeTypeUtils.IMAGE_JPEG, // Defaulting to JPEG for now, ideally derived from resource
                            imageResource
                    ))
            );

            var prompt = new Prompt(List.of(systemMessage, visionMessage));

            var response = chatClient.call(prompt);
            String content = response.getResult().getOutput().getContent();

            return outputParser.parse(content);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse transaction image", e);
        }
    }
}
