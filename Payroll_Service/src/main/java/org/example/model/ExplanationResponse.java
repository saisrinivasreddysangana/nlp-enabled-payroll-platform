package org.example.model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ExplanationResponse {
    private String explanation;
    private String payPeriod;
    private BigDecimal netChange;
    private List<PayChangeReason> reasons;
    private String language;
}

