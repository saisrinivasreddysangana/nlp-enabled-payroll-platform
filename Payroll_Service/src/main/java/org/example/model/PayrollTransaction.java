package org.example.model;


import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class PayrollTransaction {
    private String employeeId;
    private LocalDate payPeriodStart;
    private LocalDate payPeriodEnd;
    private BigDecimal grossPay;
    private BigDecimal netPay;
    private BigDecimal baseSalary;
    private BigDecimal bonus;
    private BigDecimal overtime;
    private BigDecimal taxWithheld;
    private BigDecimal totalDeductions;
    private String currency;
    private LocalDateTime loadDate;

}
