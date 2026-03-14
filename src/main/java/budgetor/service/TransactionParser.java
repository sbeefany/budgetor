package budgetor.service;

import budgetor.dto.ParsedTransactionDto;
import java.util.List;

public interface TransactionParser {
    ParsedTransactionDto parse(String text, List<String> availableCategories);
}
