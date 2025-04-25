package org.example.service;

import org.example.model.*;
import org.example.repository.PayrollRepository;
import org.example.repository.ExplanationLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExplanationService {

    private static final Logger logger = LoggerFactory.getLogger(ExplanationService.class);
    private final PayrollService payrollService;
    private final ExplanationLogRepository explanationLogRepository;
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    @Autowired
    public ExplanationService(PayrollService payrollService, ExplanationLogRepository explanationLogRepository) {
        this.payrollService = payrollService;
        this.explanationLogRepository = explanationLogRepository;
    }

    public ExplanationResponse generateExplanation(String employeeId, String question) {
        YearMonth currentMonth = YearMonth.now();
        YearMonth previousMonth = currentMonth.minusMonths(1);

        // Parse question to determine intent and time period
        Map<String, Object> questionContext = parseQuestion(question);
        String intent = (String) questionContext.get("intent");
        YearMonth targetMonth = (YearMonth) questionContext.getOrDefault("targetMonth", currentMonth);
        YearMonth comparisonMonth = targetMonth.minusMonths(1);

        logger.info("Question intent: {}, targetMonth: {}, comparisonMonth: {}", intent, targetMonth, comparisonMonth);

        // Handle different question intents
        ExplanationResponse response;
        switch (intent) {
            case "PAY_DROP":
                response = explainPayDrop(employeeId, targetMonth, comparisonMonth);
                break;
            case "DEDUCTIONS":
                response = listDeductions(employeeId, targetMonth);
                break;
            case "BONUS":
                response = checkBonus(employeeId, targetMonth);
                break;
            case "TAX":
                response = checkTaxWithheld(employeeId, targetMonth);
                break;
            case "OVERTIME":
                response = explainOvertimeChange(employeeId, targetMonth, comparisonMonth);
                break;
            case "NET_PAY":
                response = getNetPay(employeeId, targetMonth);
                break;
            case "NEW_DEDUCTIONS":
                response = checkNewDeductions(employeeId, targetMonth, comparisonMonth);
                break;
            case "HEALTH_INSURANCE":
                response = explainHealthInsuranceChange(employeeId, targetMonth, comparisonMonth);
                break;
            default:
                PayrollRepository payrollRepository = payrollService.getPayrollRepository();
                PayrollTransaction currentPayroll = payrollRepository.findPayrollByEmployeeIdAndMonth(employeeId, targetMonth);
                PayrollTransaction previousPayroll = payrollRepository.findPayrollByEmployeeIdAndMonth(employeeId, comparisonMonth);

                PayChange payChange = payrollService.analyzePayChangeBetweenMonths(employeeId, targetMonth, comparisonMonth);
                if (payChange == null) {
                    response = createNoDataResponse(targetMonth, currentPayroll, previousPayroll);
                } else {
                    response = buildExplanationResponse(payChange, targetMonth);
                }
                break;
        }

        // Log the explanation
        logExplanation(employeeId, intent, targetMonth.format(MONTH_FORMATTER), response.getExplanation(), "NLP", response.getLanguage());
        return response;
    }

    private void logExplanation(String employeeId, String intent, String payPeriod, String explanationText, String generatedBy, String language) {
        ExplanationLog explanationLog = new ExplanationLog();
        explanationLog.setEmployeeId(employeeId);
        explanationLog.setIntent(intent);
        explanationLog.setPayPeriod(payPeriod);
        explanationLog.setExplanationText(explanationText);
        explanationLog.setGeneratedBy(generatedBy);
        explanationLog.setLanguage(language);
        explanationLog.setTimestamp(LocalDateTime.now());

        try {
            explanationLogRepository.saveExplanationLog(explanationLog);
            logger.info("Explanation logged successfully for employeeId: {}, payPeriod: {}", employeeId, payPeriod);
        } catch (Exception e) {
            logger.error("Failed to log explanation for employeeId: {}, payPeriod: {}. Error: {}", employeeId, payPeriod, e.getMessage());
        }
    }

    private Map<String, Object> parseQuestion(String question) {
        Map<String, Object> context = new HashMap<>();
        String lowerQuestion = question.toLowerCase();
        YearMonth targetMonth = YearMonth.now();

        if (lowerQuestion.contains("last month")) {
            targetMonth = YearMonth.now().minusMonths(1);
        } else if (lowerQuestion.contains("march")) {
            targetMonth = YearMonth.of(2025, 3);
        } else if (lowerQuestion.contains("april")) {
            targetMonth = YearMonth.of(2025, 4);
        }

        if (lowerQuestion.contains("pay drop") || lowerQuestion.contains("why did my pay")) {
            context.put("intent", "PAY_DROP");
        } else if (lowerQuestion.contains("deductions")) {
            context.put("intent", "DEDUCTIONS");
        } else if (lowerQuestion.contains("bonus")) {
            context.put("intent", "BONUS");
        } else if (lowerQuestion.contains("tax")) {
            context.put("intent", "TAX");
        } else if (lowerQuestion.contains("overtime")) {
            context.put("intent", "OVERTIME");
        } else if (lowerQuestion.contains("net pay")) {
            context.put("intent", "NET_PAY");
        } else if (lowerQuestion.contains("new deductions")) {
            context.put("intent", "NEW_DEDUCTIONS");
        } else if (lowerQuestion.contains("health insurance") || lowerQuestion.contains("healthcare")) {
            context.put("intent", "HEALTH_INSURANCE");
        } else {
            context.put("intent", "GENERIC");
        }

        context.put("targetMonth", targetMonth);
        return context;
    }

    private ExplanationResponse createNoDataResponse(YearMonth month, PayrollTransaction currentPayroll, PayrollTransaction previousPayroll) {
        ExplanationResponse response = new ExplanationResponse();
        String explanation;
        if (currentPayroll == null && previousPayroll == null) {
            explanation = String.format("No payroll data found for %s or the previous month (%s).",
                    month.format(MONTH_FORMATTER), month.minusMonths(1).format(MONTH_FORMATTER));
        } else if (currentPayroll == null) {
            explanation = String.format("No payroll data found for %s.", month.format(MONTH_FORMATTER));
        } else {
            explanation = String.format("No payroll data found for the previous month (%s).",
                    month.minusMonths(1).format(MONTH_FORMATTER));
        }
        response.setExplanation(explanation);
        response.setPayPeriod(month.format(MONTH_FORMATTER));
        response.setNetChange(BigDecimal.ZERO);
        response.setReasons(Collections.emptyList());
        response.setLanguage("en-US");
        return response;
    }

    private ExplanationResponse explainPayDrop(String employeeId, YearMonth currentMonth, YearMonth previousMonth) {
        PayChange payChange = payrollService.analyzePayChangeBetweenMonths(employeeId, currentMonth, previousMonth);
        if (payChange == null) {
            PayrollRepository payrollRepository = payrollService.getPayrollRepository();
            PayrollTransaction currentPayroll = payrollRepository.findPayrollByEmployeeIdAndMonth(employeeId, currentMonth);
            PayrollTransaction previousPayroll = payrollRepository.findPayrollByEmployeeIdAndMonth(employeeId, previousMonth);
            return createNoDataResponse(currentMonth, currentPayroll, previousPayroll);
        }

        if (payChange.getNetPayChange().compareTo(BigDecimal.ZERO) >= 0) {
            ExplanationResponse response = new ExplanationResponse();
            response.setExplanation("Your net pay did not drop in " + currentMonth.getMonth().toString().toLowerCase() + ".");
            response.setPayPeriod(currentMonth.format(MONTH_FORMATTER));
            response.setNetChange(payChange.getNetPayChange());
            response.setReasons(Collections.emptyList());
            response.setLanguage("en-US");
            return response;
        }

        return buildExplanationResponse(payChange, currentMonth);
    }

    private ExplanationResponse listDeductions(String employeeId, YearMonth targetMonth) {
        List<DeductionBreakdown> deductions = payrollService.getDeductionRepository()
                .findByEmployeeIdAndMonth(employeeId, targetMonth);
        if (deductions.isEmpty()) {
            return createNoDataResponse(targetMonth, null, null);
        }

        StringBuilder explanation = new StringBuilder("Your deductions for " + targetMonth.getMonth().toString().toLowerCase() + " are: ");
        List<PayChangeReason> reasons = new ArrayList<>();
        for (DeductionBreakdown deduction : deductions) {
            reasons.add(new PayChangeReason("Deduction", capitalizeDeductionType(deduction.getDeductionType()),
                    "$" + deduction.getAmount()));
            explanation.append(deduction.getDeductionType()).append(" ($").append(deduction.getAmount()).append("), ");
        }

        ExplanationResponse response = new ExplanationResponse();
        response.setExplanation(explanation.toString().replaceAll(", $", "."));
        response.setPayPeriod(targetMonth.format(MONTH_FORMATTER));
        response.setNetChange(BigDecimal.ZERO);
        response.setReasons(reasons);
        response.setLanguage("en-US");
        return response;
    }

    private ExplanationResponse checkBonus(String employeeId, YearMonth targetMonth) {
        PayrollTransaction payroll = payrollService.getPayrollRepository()
                .findPayrollByEmployeeIdAndMonth(employeeId, targetMonth);
        if (payroll == null) {
            return createNoDataResponse(targetMonth, null, null);
        }

        String explanation = payroll.getBonus().compareTo(BigDecimal.ZERO) > 0
                ? "You received a bonus of $" + payroll.getBonus() + " in " + targetMonth.getMonth().toString().toLowerCase() + "."
                : "You did not receive a bonus in " + targetMonth.getMonth().toString().toLowerCase() + ".";

        ExplanationResponse response = new ExplanationResponse();
        response.setExplanation(explanation);
        response.setPayPeriod(targetMonth.format(MONTH_FORMATTER));
        response.setNetChange(payroll.getBonus());
        response.setReasons(payroll.getBonus().compareTo(BigDecimal.ZERO) > 0
                ? List.of(new PayChangeReason("Bonus", "Performance", "$" + payroll.getBonus()))
                : Collections.emptyList());
        response.setLanguage("en-US");
        return response;
    }

    private ExplanationResponse checkTaxWithheld(String employeeId, YearMonth targetMonth) {
        PayrollTransaction payroll = payrollService.getPayrollRepository()
                .findPayrollByEmployeeIdAndMonth(employeeId, targetMonth);
        if (payroll == null) {
            return createNoDataResponse(targetMonth, null, null);
        }

        ExplanationResponse response = new ExplanationResponse();
        response.setExplanation("Your tax withheld for " + targetMonth.getMonth().toString().toLowerCase() + " was $" + payroll.getTaxWithheld() + ".");
        response.setPayPeriod(targetMonth.format(MONTH_FORMATTER));
        response.setNetChange(payroll.getTaxWithheld());
        response.setReasons(List.of(new PayChangeReason("Tax", "Withholding", "$" + payroll.getTaxWithheld())));
        response.setLanguage("en-US");
        return response;
    }

    private ExplanationResponse explainOvertimeChange(String employeeId, YearMonth currentMonth, YearMonth previousMonth) {
        PayChange payChange = payrollService.analyzePayChangeBetweenMonths(employeeId, currentMonth, previousMonth);
        if (payChange == null) {
            PayrollRepository payrollRepository = payrollService.getPayrollRepository();
            PayrollTransaction currentPayroll = payrollRepository.findPayrollByEmployeeIdAndMonth(employeeId, currentMonth);
            PayrollTransaction previousPayroll = payrollRepository.findPayrollByEmployeeIdAndMonth(employeeId, previousMonth);
            return createNoDataResponse(currentMonth, currentPayroll, previousPayroll);
        }

        String explanation;
        List<PayChangeReason> reasons = new ArrayList<>();
        if (payChange.getOvertimeChange().compareTo(BigDecimal.ZERO) > 0) {
            explanation = "Your overtime pay increased by $" + payChange.getOvertimeChange() + " in " + currentMonth.getMonth().toString().toLowerCase() + ".";
            reasons.add(new PayChangeReason("Overtime", "Hours", formatDelta(payChange.getOvertimeChange())));
        } else if (payChange.getOvertimeChange().compareTo(BigDecimal.ZERO) < 0) {
            explanation = "Your overtime pay decreased by $" + payChange.getOvertimeChange().abs() + " in " + currentMonth.getMonth().toString().toLowerCase() + ".";
            reasons.add(new PayChangeReason("Overtime", "Hours", formatDelta(payChange.getOvertimeChange())));
        } else {
            explanation = "Your overtime pay did not change in " + currentMonth.getMonth().toString().toLowerCase() + ".";
        }

        ExplanationResponse response = new ExplanationResponse();
        response.setExplanation(explanation);
        response.setPayPeriod(currentMonth.format(MONTH_FORMATTER));
        response.setNetChange(payChange.getOvertimeChange());
        response.setReasons(reasons);
        response.setLanguage("en-US");
        return response;
    }

    private ExplanationResponse getNetPay(String employeeId, YearMonth targetMonth) {
        PayrollTransaction payroll = payrollService.getPayrollRepository()
                .findPayrollByEmployeeIdAndMonth(employeeId, targetMonth);
        if (payroll == null) {
            return createNoDataResponse(targetMonth, null, null);
        }

        ExplanationResponse response = new ExplanationResponse();
        response.setExplanation("Your net pay for " + targetMonth.getMonth().toString().toLowerCase() + " was $" + payroll.getNetPay() + ".");
        response.setPayPeriod(targetMonth.format(MONTH_FORMATTER));
        response.setNetChange(payroll.getNetPay());
        response.setReasons(List.of(new PayChangeReason("NetPay", "Total", "$" + payroll.getNetPay())));
        response.setLanguage("en-US");
        return response;
    }

    private ExplanationResponse checkNewDeductions(String employeeId, YearMonth currentMonth, YearMonth previousMonth) {
        List<DeductionBreakdown> currentDeductions = payrollService.getDeductionRepository()
                .findByEmployeeIdAndMonth(employeeId, currentMonth);
        List<DeductionBreakdown> previousDeductions = payrollService.getDeductionRepository()
                .findByEmployeeIdAndMonth(employeeId, previousMonth);

        if (currentDeductions.isEmpty() || previousDeductions.isEmpty()) {
            PayrollRepository payrollRepository = payrollService.getPayrollRepository();
            PayrollTransaction currentPayroll = payrollRepository.findPayrollByEmployeeIdAndMonth(employeeId, currentMonth);
            PayrollTransaction previousPayroll = payrollRepository.findPayrollByEmployeeIdAndMonth(employeeId, previousMonth);
            return createNoDataResponse(currentMonth, currentPayroll, previousPayroll);
        }

        Set<String> previousTypes = previousDeductions.stream()
                .map(DeductionBreakdown::getDeductionType)
                .collect(Collectors.toSet());
        List<DeductionBreakdown> newDeductions = currentDeductions.stream()
                .filter(d -> !previousTypes.contains(d.getDeductionType()))
                .collect(Collectors.toList());

        String explanation;
        List<PayChangeReason> reasons = new ArrayList<>();
        if (newDeductions.isEmpty()) {
            explanation = "There were no new deductions in " + currentMonth.getMonth().toString().toLowerCase() + ".";
        } else {
            explanation = "New deductions in " + currentMonth.getMonth().toString().toLowerCase() + ": ";
            for (DeductionBreakdown deduction : newDeductions) {
                reasons.add(new PayChangeReason("Deduction", capitalizeDeductionType(deduction.getDeductionType()),
                        "$" + deduction.getAmount()));
                explanation += deduction.getDeductionType() + " ($" + deduction.getAmount() + "), ";
            }
            explanation = explanation.replaceAll(", $", ".");
        }

        ExplanationResponse response = new ExplanationResponse();
        response.setExplanation(explanation);
        response.setPayPeriod(currentMonth.format(MONTH_FORMATTER));
        response.setNetChange(BigDecimal.ZERO);
        response.setReasons(reasons);
        response.setLanguage("en-US");
        return response;
    }

    private ExplanationResponse explainHealthInsuranceChange(String employeeId, YearMonth currentMonth, YearMonth previousMonth) {
        List<DeductionBreakdown> currentDeductions = payrollService.getDeductionRepository()
                .findByEmployeeIdAndMonth(employeeId, currentMonth);
        List<DeductionBreakdown> previousDeductions = payrollService.getDeductionRepository()
                .findByEmployeeIdAndMonth(employeeId, previousMonth);

        if (currentDeductions.isEmpty() || previousDeductions.isEmpty()) {
            PayrollRepository payrollRepository = payrollService.getPayrollRepository();
            PayrollTransaction currentPayroll = payrollRepository.findPayrollByEmployeeIdAndMonth(employeeId, currentMonth);
            PayrollTransaction previousPayroll = payrollRepository.findPayrollByEmployeeIdAndMonth(employeeId, previousMonth);
            return createNoDataResponse(currentMonth, currentPayroll, previousPayroll);
        }

        DeductionBreakdown currentHealth = currentDeductions.stream()
                .filter(d -> d.getDeductionType().equalsIgnoreCase("healthcare"))
                .findFirst().orElse(null);
        DeductionBreakdown previousHealth = previousDeductions.stream()
                .filter(d -> d.getDeductionType().equalsIgnoreCase("healthcare"))
                .findFirst().orElse(null);

        String explanation;
        List<PayChangeReason> reasons = new ArrayList<>();
        if (currentHealth == null || previousHealth == null) {
            explanation = "No healthcare deduction data available for comparison.";
        } else {
            BigDecimal change = currentHealth.getAmount().subtract(previousHealth.getAmount());
            if (change.compareTo(BigDecimal.ZERO) > 0) {
                explanation = "Your healthcare deduction increased by $" + change + " in " + currentMonth.getMonth().toString().toLowerCase() + ".";
                reasons.add(new PayChangeReason("Deduction", "Healthcare", formatDelta(change)));
            } else if (change.compareTo(BigDecimal.ZERO) < 0) {
                explanation = "Your healthcare deduction decreased by $" + change.abs() + " in " + currentMonth.getMonth().toString().toLowerCase() + ".";
                reasons.add(new PayChangeReason("Deduction", "Healthcare", formatDelta(change)));
            } else {
                explanation = "Your healthcare deduction did not change in " + currentMonth.getMonth().toString().toLowerCase() + ".";
            }
        }

        ExplanationResponse response = new ExplanationResponse();
        response.setExplanation(explanation);
        response.setPayPeriod(currentMonth.format(MONTH_FORMATTER));
        response.setNetChange(currentHealth != null ? currentHealth.getAmount() : BigDecimal.ZERO);
        response.setReasons(reasons);
        response.setLanguage("en-US");
        return response;
    }

    private ExplanationResponse buildExplanationResponse(PayChange payChange, YearMonth currentMonth) {
        ExplanationResponse response = new ExplanationResponse();

        response.setPayPeriod(currentMonth.format(MONTH_FORMATTER));
        response.setNetChange(payChange.getNetPayChange());
        response.setLanguage("en-US");

        List<PayChangeReason> reasons = new ArrayList<>();
        if (payChange.getBasePayChange().abs().compareTo(new BigDecimal("1.00")) > 0) {
            reasons.add(new PayChangeReason("Salary", "Base Pay", formatDelta(payChange.getBasePayChange())));
        }
        if (payChange.getBonusChange().abs().compareTo(new BigDecimal("1.00")) > 0) {
            reasons.add(new PayChangeReason("Bonus", "Performance", formatDelta(payChange.getBonusChange())));
        }
        if (payChange.getOvertimeChange().abs().compareTo(new BigDecimal("1.00")) > 0) {
            reasons.add(new PayChangeReason("Overtime", "Hours", formatDelta(payChange.getOvertimeChange())));
        }
        if (payChange.getTaxChange().abs().compareTo(new BigDecimal("1.00")) > 0) {
            reasons.add(new PayChangeReason("Tax", "Withholding", formatDelta(payChange.getTaxChange().negate())));
        }
        for (Map.Entry<String, BigDecimal> entry : payChange.getDeductionChanges().entrySet()) {
            if (entry.getValue().abs().compareTo(new BigDecimal("1.00")) > 0) {
                reasons.add(new PayChangeReason("Deduction", capitalizeDeductionType(entry.getKey()),
                        formatDelta(entry.getValue().negate())));
            }
        }

        reasons.sort((r1, r2) -> {
            BigDecimal val1 = new BigDecimal(r1.getDelta().replace("$", "").replace("+", ""));
            BigDecimal val2 = new BigDecimal(r2.getDelta().replace("$", "").replace("+", ""));
            return val2.abs().compareTo(val1.abs());
        });

        response.setReasons(reasons);
        String explanation = generateNarrativeExplanation(payChange, reasons, currentMonth);
        response.setExplanation(explanation);

        return response;
    }

    private String formatDelta(BigDecimal amount) {
        String prefix = amount.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
        return prefix + "$" + amount.abs().toString();
    }

    private String capitalizeDeductionType(String type) {
        if (type == null || type.isEmpty()) return "Other";
        return type.substring(0, 1).toUpperCase() + type.substring(1).toLowerCase();
    }

    private String generateNarrativeExplanation(PayChange payChange, List<PayChangeReason> reasons, YearMonth month) {
        StringBuilder explanation = new StringBuilder();
        String monthName = month.getMonth().toString();
        monthName = monthName.charAt(0) + monthName.substring(1).toLowerCase();

        if (payChange.getNetPayChange().compareTo(BigDecimal.ZERO) < 0) {
            explanation.append("Your ").append(monthName)
                    .append(" net pay decreased by $")
                    .append(payChange.getNetPayChange().abs())
                    .append(" due to ");
        } else if (payChange.getNetPayChange().compareTo(BigDecimal.ZERO) > 0) {
            explanation.append("Your ").append(monthName)
                    .append(" net pay increased by $")
                    .append(payChange.getNetPayChange().abs())
                    .append(" due to ");
        } else {
            explanation.append("Your ").append(monthName)
                    .append(" net pay remained the same despite some changes in ");
        }

        List<PayChangeReason> topReasons = reasons.stream().limit(3).collect(Collectors.toList());
        if (topReasons.isEmpty()) {
            explanation.append("changes in multiple small factors.");
        } else if (topReasons.size() == 1) {
            explanation.append(getLowerCaseLabel(topReasons.get(0))).append(".");
        } else if (topReasons.size() == 2) {
            explanation.append(getLowerCaseLabel(topReasons.get(0)))
                    .append(" and ")
                    .append(getLowerCaseLabel(topReasons.get(1)))
                    .append(".");
        } else {
            explanation.append(getLowerCaseLabel(topReasons.get(0)))
                    .append(", ")
                    .append(getLowerCaseLabel(topReasons.get(1)))
                    .append(", and ")
                    .append(getLowerCaseLabel(topReasons.get(2)))
                    .append(".");
        }

        return explanation.toString();
    }

    private String getLowerCaseLabel(PayChangeReason reason) {
        String result = "changes in " + reason.getLabel().toLowerCase();
        if (reason.getType().equals("Deduction") && reason.getDelta().startsWith("+")) {
            result = "increased " + reason.getLabel().toLowerCase() + " deductions";
        } else if (reason.getType().equals("Deduction") && reason.getDelta().startsWith("-")) {
            result = "decreased " + reason.getLabel().toLowerCase() + " deductions";
        } else if (reason.getType().equals("Bonus") && reason.getDelta().startsWith("-")) {
            result = "no " + reason.getLabel().toLowerCase() + " bonus";
        } else if (reason.getType().equals("Bonus") && reason.getDelta().startsWith("+")) {
            result = "a new " + reason.getLabel().toLowerCase() + " bonus";
        } else if (reason.getType().equals("Salary") && reason.getDelta().startsWith("+")) {
            result = "increased base salary";
        } else if (reason.getType().equals("Salary") && reason.getDelta().startsWith("-")) {
            result = "decreased base salary";
        } else if (reason.getType().equals("Overtime") && reason.getDelta().startsWith("+")) {
            result = "additional overtime hours";
        } else if (reason.getType().equals("Overtime") && reason.getDelta().startsWith("-")) {
            result = "reduced overtime hours";
        } else if (reason.getType().equals("Tax") && reason.getDelta().startsWith("+")) {
            result = "increased tax withholdings";
        } else if (reason.getType().equals("Tax") && reason.getDelta().startsWith("-")) {
            result = "decreased tax withholdings";
        }
        return result;
    }
}