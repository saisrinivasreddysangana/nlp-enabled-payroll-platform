package org.example.model;

import lombok.Data;

@Data
public class PayChangeReason {
    private String type;
    private String label;
    private String delta;

    public PayChangeReason(String type, String label, String delta) {
        this.type = type;
        this.label = label;
        this.delta = delta;
    }
}

