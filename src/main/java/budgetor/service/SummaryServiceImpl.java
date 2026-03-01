package budgetor.service;

import budgetor.domain.Transaction;
import budgetor.domain.TransactionType;
import budgetor.repository.TransactionRepository;
import budgetor.service.dto.CategorySummaryDto;
import budgetor.service.dto.SummaryDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
public class SummaryServiceImpl implements SummaryService {

        private final TransactionRepository transactionRepository;

        public SummaryServiceImpl(TransactionRepository transactionRepository) {
                this.transactionRepository = transactionRepository;
        }

        @Override
        public SummaryDto getSummary(LocalDateTime start, LocalDateTime end) {
                var incomes = transactionRepository.findByTransactionDateBetweenAndType(start, end,
                                TransactionType.INCOME);
                var expenses = transactionRepository.findByTransactionDateBetweenAndType(start, end,
                                TransactionType.EXPENSE);

                var totalIncome = incomes.stream()
                                .map(Transaction::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                var totalExpenses = expenses.stream()
                                .map(Transaction::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                var expensesByCategoryMap = expenses.stream()
                                .collect(Collectors.groupingBy(
                                                Transaction::getCategory,
                                                Collectors.mapping(
                                                                Transaction::getAmount,
                                                                Collectors.reducing(BigDecimal.ZERO,
                                                                                BigDecimal::add))));

                var expensesByCategory = expensesByCategoryMap.entrySet().stream()
                                .map(entry -> new CategorySummaryDto(entry.getKey().getName(), entry.getValue()))
                                .toList();

                return new SummaryDto(totalIncome, totalExpenses, expensesByCategory);
        }
}
