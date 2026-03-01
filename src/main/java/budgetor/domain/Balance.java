package budgetor.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "user_balance")
public class Balance {

    @Id
    private Long id = 1L;

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal amount = BigDecimal.ZERO;

    public Balance() {
    }

    public Balance(BigDecimal amount) {
        this.amount = amount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
