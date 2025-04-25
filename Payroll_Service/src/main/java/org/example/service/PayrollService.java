package org.example.service;

import org.example.model.DeductionBreakdown;
import org.example.model.PayChange;
import org.example.model.PayrollTransaction;
import org.example.model.Payslip;
import org.example.repository.DeductionRepository;
import org.example.repository.PayrollRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.*;

@Service
public class PayrollService {

    private final PayrollRepository payrollRepository;
    private final DeductionRepository deductionRepository;

    @Autowired
    public PayrollService(PayrollRepository payrollRepository, DeductionRepository deductionRepository) {
        this.payrollRepository = payrollRepository;
        this.deductionRepository = deductionRepository;
    }

    public PayrollRepository getPayrollRepository() {
        return payrollRepository;
    }

    public DeductionRepository getDeductionRepository() {
        return deductionRepository;
    }

    public PayrollTransaction getLatestPayroll(String employeeId) {
        return payrollRepository.findLatestPayrollByEmployeeId(employeeId);
    }

    public PayChange analyzePayChangeBetweenMonths(String employeeId, YearMonth currentMonth, YearMonth previousMonth) {
        PayrollTransaction currentPayroll = payrollRepository.findPayrollByEmployeeIdAndMonth(employeeId, currentMonth);
        PayrollTransaction previousPayroll = payrollRepository.findPayrollByEmployeeIdAndMonth(employeeId, previousMonth);

        if (currentPayroll == null || previousPayroll == null) {
            return null;
        }

        PayChange payChange = new PayChange(currentMonth, previousMonth);

        // Calculate changes in pay components
        payChange.setNetPayChange(currentPayroll.getNetPay().subtract(previousPayroll.getNetPay()));
        payChange.setBasePayChange(currentPayroll.getBaseSalary().subtract(previousPayroll.getBaseSalary()));
        payChange.setBonusChange(currentPayroll.getBonus().subtract(previousPayroll.getBonus()));
        payChange.setOvertimeChange(currentPayroll.getOvertime().subtract(previousPayroll.getOvertime()));
        payChange.setTaxChange(currentPayroll.getTaxWithheld().subtract(previousPayroll.getTaxWithheld()));
        payChange.setTotalDeductionsChange(currentPayroll.getTotalDeductions().subtract(previousPayroll.getTotalDeductions()));

        // Analyze deduction changes
        List<DeductionBreakdown> currentDeductions = deductionRepository.findByEmployeeIdAndMonth(employeeId, currentMonth);
        List<DeductionBreakdown> previousDeductions = deductionRepository.findByEmployeeIdAndMonth(employeeId, previousMonth);

        Map<String, BigDecimal> currentDeductionMap = mapDeductionsByType(currentDeductions);
        Map<String, BigDecimal> previousDeductionMap = mapDeductionsByType(previousDeductions);

        // Calculate changes for each deduction type
        Map<String, BigDecimal> deductionChanges = new HashMap<>();

        // Merge all deduction types
        Set<String> allDeductionTypes = new HashSet<>();
        allDeductionTypes.addAll(currentDeductionMap.keySet());
        allDeductionTypes.addAll(previousDeductionMap.keySet());

        for (String deductionType : allDeductionTypes) {
            BigDecimal currentAmount = currentDeductionMap.getOrDefault(deductionType, BigDecimal.ZERO);
            BigDecimal previousAmount = previousDeductionMap.getOrDefault(deductionType, BigDecimal.ZERO);
            BigDecimal change = currentAmount.subtract(previousAmount);

            if (change.compareTo(BigDecimal.ZERO) != 0) {
                deductionChanges.put(deductionType, change);
            }
        }

        // Set the entire map of deduction changes at once
        payChange.setDeductionChanges(deductionChanges);

        return payChange;
    }

    public Payslip generatePayslip(String employeeId, YearMonth yearMonth) {
        // Fetch payroll transaction for the specified month
        PayrollTransaction payroll = payrollRepository.findPayrollByEmployeeIdAndMonth(employeeId, yearMonth);
        if (payroll == null) {
            return null;
        }

        // Fetch deductions for the specified month
        List<DeductionBreakdown> deductions = deductionRepository.findByEmployeeIdAndMonth(employeeId, yearMonth);

        // Calculate YTD earnings
        BigDecimal[] ytdEarnings = payrollRepository.getYearToDateEarnings(employeeId, yearMonth.getYear(), yearMonth);

        // Create payslip
        Payslip payslip = new Payslip();
        payslip.setEmployeeId(employeeId);
        payslip.setPayPeriodStart(payroll.getPayPeriodStart());
        payslip.setPayPeriodEnd(payroll.getPayPeriodEnd());
        payslip.setBaseSalary(payroll.getBaseSalary());
        payslip.setBonus(payroll.getBonus());
        payslip.setOvertime(payroll.getOvertime());
        payslip.setGrossPay(payroll.getGrossPay());
        payslip.setTaxWithheld(payroll.getTaxWithheld());
        payslip.setTotalDeductions(payroll.getTotalDeductions());
        payslip.setDeductions(deductions);
        payslip.setNetPay(payroll.getNetPay());
        payslip.setCurrency(payroll.getCurrency());
        payslip.setYearToDateGrossPay(ytdEarnings[0]);
        payslip.setYearToDateNetPay(ytdEarnings[1]);

        return payslip;
    }

    private Map<String, BigDecimal> mapDeductionsByType(List<DeductionBreakdown> deductions) {
        Map<String, BigDecimal> deductionMap = new HashMap<>();
        for (DeductionBreakdown deduction : deductions) {
            deductionMap.put(deduction.getDeductionType(), deduction.getAmount());
        }
        return deductionMap;
    }
}