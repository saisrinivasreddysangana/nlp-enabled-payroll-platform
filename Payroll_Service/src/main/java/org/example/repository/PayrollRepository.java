package org.example.repository;

import org.example.model.PayrollTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Repository
public class PayrollRepository {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public PayrollRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<PayrollTransaction> payrollRowMapper = (rs, rowNum) -> {
        PayrollTransaction transaction = new PayrollTransaction();
        transaction.setEmployeeId(rs.getString("employee_id"));
        transaction.setPayPeriodStart(rs.getDate("pay_period_start").toLocalDate());
        transaction.setPayPeriodEnd(rs.getDate("pay_period_end").toLocalDate());
        transaction.setGrossPay(rs.getBigDecimal("gross_pay"));
        transaction.setNetPay(rs.getBigDecimal("net_pay"));
        transaction.setBaseSalary(rs.getBigDecimal("base_salary"));
        transaction.setBonus(rs.getBigDecimal("bonus"));
        transaction.setOvertime(rs.getBigDecimal("overtime"));
        transaction.setTaxWithheld(rs.getBigDecimal("tax_withheld"));
        transaction.setTotalDeductions(rs.getBigDecimal("total_deductions"));
        transaction.setCurrency(rs.getString("currency"));
        transaction.setLoadDate(rs.getTimestamp("load_date").toLocalDateTime());
        return transaction;
    };

    public List<PayrollTransaction> findByEmployeeIdAndPayPeriod(String employeeId, LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT * FROM payroll_transactions " +
                "WHERE employee_id = ? " +
                "AND pay_period_end BETWEEN ? AND ? " +
                "ORDER BY pay_period_end DESC";

        return jdbcTemplate.query(
                sql,
                new Object[]{employeeId, startDate, endDate},
                payrollRowMapper
        );
    }

    public PayrollTransaction findLatestPayrollByEmployeeId(String employeeId) {
        String sql = "SELECT * FROM payroll_transactions " +
                "WHERE employee_id = ? " +
                "ORDER BY pay_period_end DESC " +
                "LIMIT 1";

        List<PayrollTransaction> results = jdbcTemplate.query(
                sql,
                new Object[]{employeeId},
                payrollRowMapper
        );

        return results.isEmpty() ? null : results.get(0);
    }

    public PayrollTransaction findPayrollByEmployeeIdAndMonth(String employeeId, YearMonth yearMonth) {
        LocalDate startOfMonth = yearMonth.atDay(1);
        LocalDate endOfMonth = yearMonth.atEndOfMonth();

        String sql = "SELECT * FROM payroll_transactions " +
                "WHERE employee_id = ? " +
                "AND pay_period_end BETWEEN ? AND ? " +
                "ORDER BY pay_period_end DESC " +
                "LIMIT 1";

        List<PayrollTransaction> results = jdbcTemplate.query(
                sql,
                new Object[]{employeeId, startOfMonth, endOfMonth},
                payrollRowMapper
        );

        return results.isEmpty() ? null : results.get(0);
    }

    public BigDecimal[] getYearToDateEarnings(String employeeId, int year, YearMonth targetMonth) {
        LocalDate startOfYear = YearMonth.of(year, 1).atDay(1);
        LocalDate endOfMonth = targetMonth.atEndOfMonth();

        String sql = "SELECT COALESCE(SUM(gross_pay), 0) as total_gross, COALESCE(SUM(net_pay), 0) as total_net " +
                "FROM payroll_transactions " +
                "WHERE employee_id = ? " +
                "AND pay_period_end BETWEEN ? AND ?";

        return jdbcTemplate.query(
                sql,
                new Object[]{employeeId, startOfYear, endOfMonth},
                rs -> {
                    if (rs.next()) {
                        return new BigDecimal[] {
                                rs.getBigDecimal("total_gross"),
                                rs.getBigDecimal("total_net")
                        };
                    }
                    return new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO };
                }
        );
    }
}