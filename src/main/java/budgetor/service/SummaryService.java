package budgetor.service;

import budgetor.service.dto.SummaryDto;
import java.time.LocalDateTime;

public interface SummaryService {
    SummaryDto getSummary(LocalDateTime start, LocalDateTime end);
}
