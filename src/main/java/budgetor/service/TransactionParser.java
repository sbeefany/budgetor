package budgetor.service;

import budgetor.dto.ParsedTransactionDto;
import org.springframework.core.io.Resource;
import java.util.List;

public interface TransactionParser {
    ParsedTransactionDto parse(String text, List<String> availableCategories);
    ParsedTransactionDto parse(Resource imageResource, List<String> availableCategories);
}
