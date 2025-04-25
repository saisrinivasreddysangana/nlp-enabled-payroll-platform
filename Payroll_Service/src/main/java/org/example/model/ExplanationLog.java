package org.example.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ExplanationLog {
    private Long explanationId;
    private String employeeId;
    private String intent;
    private String payPeriod;
    private String explanationText;
    private String generatedBy;
    private String language;
    private LocalDateTime timestamp;
}
