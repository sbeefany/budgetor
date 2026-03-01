package budgetor.service;

import budgetor.domain.Category;
import budgetor.domain.Goal;
import budgetor.domain.GoalType;
import budgetor.domain.TransactionType;
import budgetor.dto.GoalProgressDto;
import budgetor.repository.GoalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoalServiceTest {

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private TransactionService transactionService;

    @Mock
    private CategoryService categoryService;

    @Captor
    private ArgumentCaptor<Goal> goalCaptor;

    private GoalService goalService;

    @BeforeEach
    void setUp() {
        goalService = new GoalServiceImpl(goalRepository, transactionService, categoryService);
    }

    @Test
    void shouldCreateSavingsGoal() {
        // Given
        var targetAmount = new BigDecimal("100000.00");
        var targetDate = LocalDateTime.now().plusMonths(6);
        var name = "На Машину";

        // When
        goalService.createSavingsGoal(targetAmount, targetDate, name);

        // Then
        verify(goalRepository).save(goalCaptor.capture());
        var savedGoal = goalCaptor.getValue();

        assertThat(savedGoal.getTargetType()).isEqualTo(GoalType.SAVINGS);
        assertThat(savedGoal.getTargetAmount()).isEqualTo(targetAmount);
        assertThat(savedGoal.getEndDate()).isEqualTo(targetDate);
        assertThat(savedGoal.getName()).isEqualTo(name);
        assertThat(savedGoal.getStartDate()).isNotNull();
        assertThat(savedGoal.getCategory()).isNull();
    }

    @Test
    void shouldCreateBudgetGoal() {
        // Given
        var categoryName = "Еда";
        var targetAmount = new BigDecimal("15000.00");
        var startDate = LocalDateTime.now().withDayOfMonth(1);
        var endDate = startDate.plusMonths(1);

        var category = new Category(categoryName, TransactionType.EXPENSE, true);
        when(categoryService.getCategoryByName(categoryName)).thenReturn(Optional.of(category));

        // When
        goalService.createBudgetGoal(categoryName, targetAmount, startDate, endDate);

        // Then
        verify(goalRepository).save(goalCaptor.capture());
        var savedGoal = goalCaptor.getValue();

        assertThat(savedGoal.getTargetType()).isEqualTo(GoalType.BUDGET);
        assertThat(savedGoal.getTargetAmount()).isEqualTo(targetAmount);
        assertThat(savedGoal.getStartDate()).isEqualTo(startDate);
        assertThat(savedGoal.getEndDate()).isEqualTo(endDate);
        assertThat(savedGoal.getCategory()).isEqualTo(category);
        assertThat(savedGoal.getName()).isEqualTo("Бюджет: " + categoryName);
    }

    @Test
    void shouldThrowExceptionWhenCreatingBudgetGoalForNonExistentCategory() {
        // Given
        var categoryName = "Неизвестная";
        when(categoryService.getCategoryByName(categoryName)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> goalService.createBudgetGoal(categoryName, BigDecimal.TEN, LocalDateTime.now(),
                LocalDateTime.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Category not found");

        verify(goalRepository, never()).save(any());
    }

    @Test
    void shouldCalculateProgressForSavingsGoal() {
        // Given
        var goal = new Goal(GoalType.SAVINGS, null, new BigDecimal("100000.00"), LocalDateTime.now(),
                LocalDateTime.now().plusMonths(5), "Машина");

        when(goalRepository.findAll()).thenReturn(List.of(goal));
        when(transactionService.getCurrentBalance()).thenReturn(new BigDecimal("50000.00")); // 50%

        // When
        var progresses = goalService.getAllGoalProgresses();

        // Then
        assertThat(progresses).hasSize(1);
        var dto = progresses.get(0);

        assertThat(dto.type()).isEqualTo(GoalType.SAVINGS);
        assertThat(dto.targetAmount()).isEqualTo(new BigDecimal("100000.00"));
        assertThat(dto.currentAmount()).isEqualTo(new BigDecimal("50000.00"));
        assertThat(dto.progressPercentage()).isEqualTo(new BigDecimal("50.00"));
    }

    @Test
    void shouldCalculateProgressForBudgetGoal() {
        // Given
        var category = new Category("Еда", TransactionType.EXPENSE, true);
        var start = LocalDateTime.now().minusDays(10);
        var end = LocalDateTime.now().plusDays(20);
        var goal = new Goal(GoalType.BUDGET, category, new BigDecimal("20000.00"), start, end, "Бюджет: Еда");

        when(goalRepository.findAll()).thenReturn(List.of(goal));
        when(transactionService.calculateTotalSpentCategory(category, start, end, TransactionType.EXPENSE))
                .thenReturn(new BigDecimal("5000.00")); // 25% spent

        // When
        var progresses = goalService.getAllGoalProgresses();

        // Then
        assertThat(progresses).hasSize(1);
        var dto = progresses.get(0);

        assertThat(dto.type()).isEqualTo(GoalType.BUDGET);
        assertThat(dto.targetAmount()).isEqualTo(new BigDecimal("20000.00"));
        assertThat(dto.currentAmount()).isEqualTo(new BigDecimal("5000.00"));
        assertThat(dto.progressPercentage()).isEqualTo(new BigDecimal("25.00"));
    }

    @Test
    void shouldHandleZeroTargetAmountWithoutDivisionByZero() {
        // Given
        var goal = new Goal(GoalType.SAVINGS, null, BigDecimal.ZERO, LocalDateTime.now(),
                LocalDateTime.now().plusMonths(5), "Нулевая цель");

        when(goalRepository.findAll()).thenReturn(List.of(goal));
        when(transactionService.getCurrentBalance()).thenReturn(new BigDecimal("1000.00"));

        // When
        var progresses = goalService.getAllGoalProgresses();

        // Then
        assertThat(progresses).hasSize(1);
        var dto = progresses.get(0);

        assertThat(dto.progressPercentage()).isEqualTo(BigDecimal.ZERO); // Failsafe
    }
}
