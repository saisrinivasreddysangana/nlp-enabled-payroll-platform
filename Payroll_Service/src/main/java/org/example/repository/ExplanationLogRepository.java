package org.example.repository;



import org.example.model.ExplanationLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Repository
public class ExplanationLogRepository {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public ExplanationLogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void saveExplanationLog(ExplanationLog explanationLog) {
        String sql = "INSERT INTO explanation_logs (employee_id, intent, pay_period, explanation_text, generated_by, language, timestamp) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(
                sql,
                explanationLog.getEmployeeId(),
                explanationLog.getIntent(),
                explanationLog.getPayPeriod(),
                explanationLog.getExplanationText(),
                explanationLog.getGeneratedBy(),
                explanationLog.getLanguage(),
                explanationLog.getTimestamp() != null ? explanationLog.getTimestamp() : LocalDateTime.now()
        );
    }
}
