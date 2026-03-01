package budgetor.service;

import budgetor.domain.Category;
import budgetor.domain.Transaction;
import budgetor.domain.TransactionType;
import budgetor.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SummaryServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private SummaryServiceImpl summaryService;

    private LocalDateTime start;
    private LocalDateTime end;
    private Category foodCategory;
    private Category transportCategory;
    private Category salaryCategory;

    @BeforeEach
    void setUp() {
        start = LocalDateTime.of(2026, 3, 1, 0, 0);
        end = LocalDateTime.of(2026, 3, 31, 23, 59);

        foodCategory = new Category("Еда", TransactionType.EXPENSE, true);
        transportCategory = new Category("Транспорт", TransactionType.EXPENSE, true);
        salaryCategory = new Category("Зарплата", TransactionType.INCOME, true);
    }

    @Test
    void getSummary_shouldReturnEmptySummaryWhenNoTransactions() {
        // given
        when(transactionRepository.findByTransactionDateBetweenAndType(start, end, TransactionType.INCOME))
                .thenReturn(List.of());
        when(transactionRepository.findByTransactionDateBetweenAndType(start, end, TransactionType.EXPENSE))
                .thenReturn(List.of());

        // when
        var summary = summaryService.getSummary(start, end);

        // then
        assertThat(summary.totalIncome()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.totalExpenses()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.expensesByCategory()).isEmpty();
    }

    @Test
    void getSummary_shouldCalculateTotalIncomeAndExpenses() {
        // given
        var income1 = new Transaction(new BigDecimal("50000"), salaryCategory, "Аванс", start.plusDays(1),
                TransactionType.INCOME);
        var income2 = new Transaction(new BigDecimal("30000"), salaryCategory, "Остаток", start.plusDays(15),
                TransactionType.INCOME);

        var expense1 = new Transaction(new BigDecimal("1500"), foodCategory, "Обед", start.plusDays(2),
                TransactionType.EXPENSE);
        var expense2 = new Transaction(new BigDecimal("500"), transportCategory, "Метро", start.plusDays(3),
                TransactionType.EXPENSE);

        when(transactionRepository.findByTransactionDateBetweenAndType(start, end, TransactionType.INCOME))
                .thenReturn(List.of(income1, income2));
        when(transactionRepository.findByTransactionDateBetweenAndType(start, end, TransactionType.EXPENSE))
                .thenReturn(List.of(expense1, expense2));

        // when
        var summary = summaryService.getSummary(start, end);

        // then
        assertThat(summary.totalIncome()).isEqualByComparingTo(new BigDecimal("80000"));
        assertThat(summary.totalExpenses()).isEqualByComparingTo(new BigDecimal("2000"));
    }

    @Test
    void getSummary_shouldGroupExpensesByCategory() {
        // given
        var expense1 = new Transaction(new BigDecimal("1500"), foodCategory, "Обед", start.plusDays(2),
                TransactionType.EXPENSE);
        var expense2 = new Transaction(new BigDecimal("500"), foodCategory, "Ужин", start.plusDays(2),
                TransactionType.EXPENSE);
        var expense3 = new Transaction(new BigDecimal("100"), transportCategory, "Автобус", start.plusDays(3),
                TransactionType.EXPENSE);

        when(transactionRepository.findByTransactionDateBetweenAndType(start, end, TransactionType.INCOME))
                .thenReturn(List.of());
        when(transactionRepository.findByTransactionDateBetweenAndType(start, end, TransactionType.EXPENSE))
                .thenReturn(List.of(expense1, expense2, expense3));

        // when
        var summary = summaryService.getSummary(start, end);

        // then
        assertThat(summary.expensesByCategory()).hasSize(2);

        var foodSummary = summary.expensesByCategory().stream()
                .filter(dto -> dto.categoryName().equals("Еда"))
                .findFirst()
                .orElseThrow();
        assertThat(foodSummary.totalAmount()).isEqualByComparingTo(new BigDecimal("2000"));

        var transportSummary = summary.expensesByCategory().stream()
                .filter(dto -> dto.categoryName().equals("Транспорт"))
                .findFirst()
                .orElseThrow();
        assertThat(transportSummary.totalAmount()).isEqualByComparingTo(new BigDecimal("100"));
    }
}
