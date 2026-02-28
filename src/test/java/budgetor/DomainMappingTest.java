package budgetor;

import budgetor.domain.Category;
import budgetor.domain.Goal;
import budgetor.domain.GoalType;
import budgetor.domain.Transaction;
import budgetor.domain.TransactionType;
import budgetor.repository.CategoryRepository;
import budgetor.repository.GoalRepository;
import budgetor.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class DomainMappingTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private GoalRepository goalRepository;

    @Test
    void testSaveAndRetrieveEntities() {
        // 1. Save and verify Category
        Category category = new Category("Еда", TransactionType.EXPENSE, true);
        Category savedCategory = categoryRepository.save(category);

        assertThat(savedCategory.getId()).isNotNull();
        assertThat(savedCategory.getName()).isEqualTo("Еда");

        // Verify custom findByNameIgnoreCase
        assertThat(categoryRepository.findByNameIgnoreCase("еда")).isPresent();

        // 2. Save and verify Transaction
        Transaction transaction = new Transaction(
                new BigDecimal("350.50"),
                savedCategory,
                "Ужин в кафе",
                LocalDateTime.now(),
                TransactionType.EXPENSE);
        Transaction savedTransaction = transactionRepository.save(transaction);

        assertThat(savedTransaction.getId()).isNotNull();
        assertThat(savedTransaction.getAmount()).isEqualByComparingTo("350.50");
        assertThat(savedTransaction.getCategory().getId()).isEqualTo(savedCategory.getId());

        // 3. Save and verify Goal
        Goal goal = new Goal(
                GoalType.BUDGET,
                savedCategory,
                new BigDecimal("15000.00"),
                LocalDateTime.now(),
                LocalDateTime.now().plusMonths(1),
                "Бюджет на еду");
        Goal savedGoal = goalRepository.save(goal);

        assertThat(savedGoal.getId()).isNotNull();
        assertThat(savedGoal.getTargetAmount()).isEqualByComparingTo("15000.00");
        assertThat(savedGoal.getTargetType()).isEqualTo(GoalType.BUDGET);
    }
}
