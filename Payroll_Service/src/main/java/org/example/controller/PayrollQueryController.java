package org.example.controller;

import jakarta.validation.constraints.NotBlank;
import org.example.model.ExplanationResponse;
import org.example.model.Payslip;
import org.example.model.QueryRequest;
import org.example.service.ExplanationService;
import org.example.service.PayrollService;
import org.example.util.PayslipPdfGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

@Validated
@RestController
@RequestMapping("/api")
public class PayrollQueryController {
    private static final Logger logger = LoggerFactory.getLogger(PayrollQueryController.class);

    private final ExplanationService explanationService;
    private final PayrollService payrollService;

    @Autowired
    public PayrollQueryController(ExplanationService explanationService,
                                   PayrollService payrollService)  {
        this.explanationService = explanationService;
        this.payrollService = payrollService;
    }

    @PostMapping("/query")
    public ResponseEntity<ExplanationResponse> explainPayChange(@RequestBody QueryRequest request) {
        String employeeId = request.getEmployeeId();
        String question = request.getQuestion();

        ExplanationResponse explanation = explanationService.generateExplanation(employeeId, question);
        return ResponseEntity.ok(explanation);
    }

    @GetMapping("/payslip")
    public ResponseEntity<byte[]> generatePayslip(
            @RequestParam("employeeId") @NotBlank String employeeId,
            @RequestParam("yearMonth") @NotBlank String yearMonthStr) {
        logger.info("Generating payslip PDF for employeeId: {}, yearMonth: {}", employeeId, yearMonthStr);
        try {
            YearMonth yearMonth = YearMonth.parse(yearMonthStr);
            Payslip payslip = payrollService.generatePayslip(employeeId, yearMonth);
            if (payslip == null) {
                return ResponseEntity.notFound().build();
            }

            // Generate PDF
            byte[] pdfBytes = PayslipPdfGenerator.generatePayslipPdf(payslip);

            // Set headers for PDF download
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment",
                    String.format("payslip_%s_%s.pdf", employeeId, yearMonthStr));
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return new ResponseEntity<>(pdfBytes, headers, org.springframework.http.HttpStatus.OK);
        } catch (DateTimeParseException e) {
            logger.error("Invalid yearMonth format: {}", yearMonthStr, e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error generating payslip PDF for employeeId: {}, yearMonth: {}", employeeId, yearMonthStr, e);
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
