package org.example.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DeductionBreakdown {
    private String employeeId;
    private LocalDate payPeriodEnd;
    private String deductionType;
    private BigDecimal amount;
    private BigDecimal employerMatch;
    private String category;
}

