package budgetor;

import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Scanner;

@Component
@Profile("local-cli") // Only active if the 'local-cli' profile is enabled, to avoid breaking Telegram mode
public class LocalTerminalRunner implements CommandLineRunner {

    private final ChatClient chatClient;

    public LocalTerminalRunner(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public void run(String... args) {
        System.out.println("=================================================");
        System.out.println(" Budgetor Local CLI Started!");
        System.out.println(" Type 'exit' to quit.");
        System.out.println("=================================================");

        try (var scanner = new Scanner(System.in)) {
            while (true) {
            System.out.print("\nYou: ");
            var input = scanner.nextLine();

            if ("exit".equalsIgnoreCase(input.trim())) {
                System.out.println("Exiting Local CLI.");
                break;
            }

            if (input.isBlank()) {
                continue;
            }

            try {
                System.out.print("Budgetor AI: ...thinking...");
                var response = chatClient.call(new Prompt(input));
                var content = response.getResult().getOutput().getContent();
                System.out.print("\rBudgetor AI: " + content + " \n");
            } catch (Exception e) {
                System.out.print("\rBudgetor AI: [Error communicating with local LLM] " + e.getMessage() + " \n");
            }
        }
        }
    }
}
