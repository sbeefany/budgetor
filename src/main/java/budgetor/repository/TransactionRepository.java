package budgetor.repository;

import budgetor.domain.Transaction;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import budgetor.domain.Category;
import budgetor.domain.TransactionType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findByCategoryAndTransactionDateBetweenAndType(Category category, LocalDateTime start,
            LocalDateTime end, TransactionType type);

    @EntityGraph(attributePaths = { "category" })
    List<Transaction> findByTransactionDateBetweenAndType(LocalDateTime start, LocalDateTime end, TransactionType type);
}
