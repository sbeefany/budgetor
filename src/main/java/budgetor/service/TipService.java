package budgetor.service;

public interface TipService {
    /**
     * Analyzes spending history, balances, and goals, returning a financial tip
     * generated via Spring AI.
     * 
     * @return A localized (Russian) string containing the tip.
     */
    String generateTip();
}
