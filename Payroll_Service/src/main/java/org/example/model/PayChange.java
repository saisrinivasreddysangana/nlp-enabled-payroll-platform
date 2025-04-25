package org.example.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;

@Data
public class PayChange {
    private YearMonth currentMonth;
    private YearMonth previousMonth;
    private BigDecimal netPayChange;
    private BigDecimal basePayChange;
    private BigDecimal bonusChange;
    private BigDecimal overtimeChange;
    private BigDecimal taxChange;
    private BigDecimal totalDeductionsChange;
    private Map<String, BigDecimal> deductionChanges = new HashMap<>();

    public PayChange(YearMonth currentMonth, YearMonth previousMonth) {
        this.currentMonth = currentMonth;
        this.previousMonth = previousMonth;
    }
}

