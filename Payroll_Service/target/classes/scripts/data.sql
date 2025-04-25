-- Sample data for employee1 for March and April 2025
-- March 2025 data
INSERT INTO payroll_transactions
(employee_id, pay_period_start, pay_period_end, gross_pay, net_pay, base_salary, bonus, overtime, tax_withheld, total_deductions, currency, load_date)
VALUES
('emp001', '2025-03-01', '2025-03-31', 5200.00, 3700.00, 5000.00, 200.00, 0.00, 1000.00, 500.00, 'USD', '2025-04-02 10:00:00');

-- April 2025 data with decreased net pay
INSERT INTO payroll_transactions
(employee_id, pay_period_start, pay_period_end, gross_pay, net_pay, base_salary, bonus, overtime, tax_withheld, total_deductions, currency, load_date)
VALUES
('emp001', '2025-04-01', '2025-04-30', 5000.00, 3450.00, 5000.00, 0.00, 0.00, 965.00, 585.00, 'USD', '2025-05-02 10:00:00');

-- March 2025 deductions
INSERT INTO deduction_breakdown
(employee_id, pay_period_end, deduction_type, amount, employer_match, category)
VALUES
('emp001', '2025-03-31', 'healthcare', 300.00, 0.00, 'health'),
('emp001', '2025-03-31', '401k', 200.00, 200.00, 'retirement');

-- April 2025 deductions with increased healthcare deduction
INSERT INTO deduction_breakdown
(employee_id, pay_period_end, deduction_type, amount, employer_match, category)
VALUES
('emp001', '2025-04-30', 'healthcare', 385.00, 0.00, 'health'),
('emp001', '2025-04-30', '401k', 200.00, 200.00, 'retirement');

-- Sample for employee2
-- March 2025 data
INSERT INTO payroll_transactions
(employee_id, pay_period_start, pay_period_end, gross_pay, net_pay, base_salary, bonus, overtime, tax_withheld, total_deductions, currency, load_date)
VALUES
('emp002', '2025-03-01', '2025-03-31', 6000.00, 4200.00, 5500.00, 0.00, 500.00, 1200.00, 600.00, 'USD', '2025-04-02 10:00:00');

-- April 2025 data with increased net pay due to bonus
INSERT INTO payroll_transactions
(employee_id, pay_period_start, pay_period_end, gross_pay, net_pay, base_salary, bonus, overtime, tax_withheld, total_deductions, currency, load_date)
VALUES
('emp002', '2025-04-01', '2025-04-30', 7000.00, 4950.00, 5500.00, 1500.00, 0.00, 1450.00, 600.00, 'USD', '2025-05-02 10:00:00');

-- March 2025 deductions
INSERT INTO deduction_breakdown
(employee_id, pay_period_end, deduction_type, amount, employer_match, category)
VALUES
('emp002', '2025-03-31', 'healthcare', 400.00, 0.00, 'health'),
('emp002', '2025-03-31', '401k', 200.00, 200.00, 'retirement');

-- April 2025 deductions (unchanged)
INSERT INTO deduction_breakdown
(employee_id, pay_period_end, deduction_type, amount, employer_match, category)
VALUES
('emp002', '2025-04-30', 'healthcare', 400.00, 0.00, 'health'),
('emp002', '2025-04-30', '401k', 200.00, 200.00, 'retirement');