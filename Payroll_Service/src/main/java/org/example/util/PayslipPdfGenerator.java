package org.example.util;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.example.model.DeductionBreakdown;
import org.example.model.Payslip;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

public class PayslipPdfGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    public static byte[] generatePayslipPdf(Payslip payslip) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // Header
        document.add(new Paragraph("Payslip")
                .setFontSize(20)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20));

        // Employee and Pay Period Details
        document.add(new Paragraph("Employee ID: " + payslip.getEmployeeId())
                .setFontSize(12)
                .setMarginBottom(5));
        document.add(new Paragraph("Pay Period: " +
                payslip.getPayPeriodStart().format(DATE_FORMATTER) + " - " +
                payslip.getPayPeriodEnd().format(DATE_FORMATTER))
                .setFontSize(12)
                .setMarginBottom(10));

        // Earnings Section
        document.add(new Paragraph("Earnings")
                .setFontSize(14)
                .setBold()
                .setMarginTop(10)
                .setMarginBottom(5));

        Table earningsTable = new Table(UnitValue.createPercentArray(new float[]{70, 30}))
                .useAllAvailableWidth();
        earningsTable.addHeaderCell("Description");
        earningsTable.addHeaderCell("Amount (" + payslip.getCurrency() + ")");
        earningsTable.addCell("Base Salary");
        earningsTable.addCell(formatAmount(payslip.getBaseSalary()));
        earningsTable.addCell("Bonus");
        earningsTable.addCell(formatAmount(payslip.getBonus()));
        earningsTable.addCell("Overtime");
        earningsTable.addCell(formatAmount(payslip.getOvertime()));
        earningsTable.addCell(new Paragraph("Gross Pay").setBold());
        earningsTable.addCell(new Paragraph(formatAmount(payslip.getGrossPay())).setBold());
        document.add(earningsTable);

        // Deductions Section
        document.add(new Paragraph("Deductions")
                .setFontSize(14)
                .setBold()
                .setMarginTop(20)
                .setMarginBottom(5));

        Table deductionsTable = new Table(UnitValue.createPercentArray(new float[]{70, 30}))
                .useAllAvailableWidth();
        deductionsTable.addHeaderCell("Description");
        deductionsTable.addHeaderCell("Amount (" + payslip.getCurrency() + ")");
        for (DeductionBreakdown deduction : payslip.getDeductions()) {
            deductionsTable.addCell(deduction.getDeductionType());
            deductionsTable.addCell(formatAmount(deduction.getAmount()));
        }
        deductionsTable.addCell(new Paragraph("Total Deductions").setBold());
        deductionsTable.addCell(new Paragraph(formatAmount(payslip.getTotalDeductions())).setBold());
        document.add(deductionsTable);

        // Taxes Section
        document.add(new Paragraph("Taxes")
                .setFontSize(14)
                .setBold()
                .setMarginTop(20)
                .setMarginBottom(5));

        Table taxesTable = new Table(UnitValue.createPercentArray(new float[]{70, 30}))
                .useAllAvailableWidth();
        taxesTable.addHeaderCell("Description");
        taxesTable.addHeaderCell("Amount (" + payslip.getCurrency() + ")");
        taxesTable.addCell("Tax Withheld");
        taxesTable.addCell(formatAmount(payslip.getTaxWithheld()));
        document.add(taxesTable);

        // Net Pay
        document.add(new Paragraph("Net Pay")
                .setFontSize(14)
                .setBold()
                .setMarginTop(20)
                .setMarginBottom(5));

        Table netPayTable = new Table(UnitValue.createPercentArray(new float[]{70, 30}))
                .useAllAvailableWidth();
        netPayTable.addCell(new Paragraph("Net Pay").setBold());
        netPayTable.addCell(new Paragraph(formatAmount(payslip.getNetPay())).setBold());
        document.add(netPayTable);

        // Year-to-Date Totals
        document.add(new Paragraph("Year-to-Date Totals")
                .setFontSize(14)
                .setBold()
                .setMarginTop(20)
                .setMarginBottom(5));

        Table ytdTable = new Table(UnitValue.createPercentArray(new float[]{70, 30}))
                .useAllAvailableWidth();
        ytdTable.addHeaderCell("Description");
        ytdTable.addHeaderCell("Amount (" + payslip.getCurrency() + ")");
        ytdTable.addCell("YTD Gross Pay");
        ytdTable.addCell(formatAmount(payslip.getYearToDateGrossPay()));
        ytdTable.addCell("YTD Net Pay");
        ytdTable.addCell(formatAmount(payslip.getYearToDateNetPay()));
        document.add(ytdTable);

        // Footer
        document.add(new Paragraph("Generated on: " + java.time.LocalDate.now().format(DATE_FORMATTER))
                .setFontSize(10)
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginTop(20));

        document.close();
        return baos.toByteArray();
    }

    private static String formatAmount(BigDecimal amount) {
        return String.format("%.2f", amount != null ? amount : BigDecimal.ZERO);
    }
}