package org.example.repository;

import org.example.model.DeductionBreakdown;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Date;
import java.util.List;

@Repository
public class DeductionRepository {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public DeductionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<DeductionBreakdown> deductionRowMapper = (rs, rowNum) -> {
        DeductionBreakdown deduction = new DeductionBreakdown();
        deduction.setEmployeeId(rs.getString("employee_id"));
        deduction.setPayPeriodEnd(rs.getDate("pay_period_end").toLocalDate());
        deduction.setDeductionType(rs.getString("deduction_type"));
        deduction.setAmount(rs.getBigDecimal("amount"));
        deduction.setEmployerMatch(rs.getBigDecimal("employer_match"));
        deduction.setCategory(rs.getString("category"));
        return deduction;
    };

    public List<DeductionBreakdown> findByEmployeeIdAndPayPeriod(String employeeId, LocalDate payPeriodEnd) {
        String sql = "SELECT * FROM deduction_breakdown " +
                "WHERE employee_id = ? AND pay_period_end = ?";

        return jdbcTemplate.query(
                sql,
                new Object[]{employeeId, payPeriodEnd},
                deductionRowMapper
        );
    }

    public List<DeductionBreakdown> findByEmployeeIdAndMonth(String employeeId, YearMonth yearMonth) {
        // Calculate the start and end of the month
        LocalDate startOfMonth = yearMonth.atDay(1);
        LocalDate endOfMonth = yearMonth.atEndOfMonth();

        String sql = "SELECT * FROM deduction_breakdown " +
                "WHERE employee_id = ? " +
                "AND pay_period_end BETWEEN ? AND ?";

        return jdbcTemplate.query(
                sql,
                new Object[]{employeeId, startOfMonth, endOfMonth},
                deductionRowMapper
        );
    }
}

