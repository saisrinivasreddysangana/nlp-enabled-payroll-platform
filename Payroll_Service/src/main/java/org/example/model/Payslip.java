package org.example.model;


import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class Payslip {
    private String employeeId;
    private LocalDate payPeriodStart;
    private LocalDate payPeriodEnd;
    private BigDecimal baseSalary;
    private BigDecimal bonus;
    private BigDecimal overtime;
    private BigDecimal grossPay;
    private BigDecimal taxWithheld;
    private BigDecimal totalDeductions;
    private List<DeductionBreakdown> deductions;
    private BigDecimal netPay;
    private String currency;
    private BigDecimal yearToDateGrossPay;
    private BigDecimal yearToDateNetPay;
}
