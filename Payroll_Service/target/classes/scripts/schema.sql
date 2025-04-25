-- Create payroll_transactions table
CREATE TABLE IF NOT EXISTS payroll_transactions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    employee_id VARCHAR(50) NOT NULL,
    pay_period_start DATE NOT NULL,
    pay_period_end DATE NOT NULL,
    gross_pay DECIMAL(10,2) NOT NULL,
    net_pay DECIMAL(10,2) NOT NULL,
    base_salary DECIMAL(10,2) NOT NULL,
    bonus DECIMAL(10,2) DEFAULT 0.00,
    overtime DECIMAL(10,2) DEFAULT 0.00,
    tax_withheld DECIMAL(10,2) NOT NULL,
    total_deductions DECIMAL(10,2) NOT NULL,
    currency VARCHAR(10) DEFAULT 'USD',
    load_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);



-- Create deduction_breakdown table
CREATE TABLE IF NOT EXISTS deduction_breakdown (
    id INT AUTO_INCREMENT PRIMARY KEY,
    employee_id VARCHAR(50) NOT NULL,
    pay_period_end DATE NOT NULL,
    deduction_type VARCHAR(100) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    employer_match DECIMAL(10,2) DEFAULT 0.00,
    category VARCHAR(50)
);


-- Create explanation_logs table
CREATE TABLE IF NOT EXISTS explanation_logs (
    explanation_id INT AUTO_INCREMENT PRIMARY KEY,
    employee_id VARCHAR(50) NOT NULL,
    intent VARCHAR(255),
    pay_period VARCHAR,
    explanation_text TEXT NOT NULL,
    generated_by VARCHAR(50) DEFAULT 'system',
    language VARCHAR(10) DEFAULT 'en-US',
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

DELIMITER //

DROP PROCEDURE IF EXISTS manage_indexes //

CREATE PROCEDURE manage_indexes()
BEGIN
    -- Declare handler for SQL errors (1091 = index doesn't exist)
    DECLARE CONTINUE HANDLER FOR 1091 BEGIN END;

    -- Handle payroll_transactions.employee_id index
    BEGIN
        DECLARE CONTINUE HANDLER FOR 1091 BEGIN END;
        DROP INDEX idx_payroll_employee_id ON payroll_transactions;
    END;
    CREATE INDEX idx_payroll_employee_id ON payroll_transactions(employee_id);

    -- Handle payroll_transactions.pay_period_end index
    BEGIN
        DECLARE CONTINUE HANDLER FOR 1091 BEGIN END;
        DROP INDEX idx_payroll_pay_period_end ON payroll_transactions;
    END;
    CREATE INDEX idx_payroll_pay_period_end ON payroll_transactions(pay_period_end);

    -- Handle deduction_breakdown.employee_id index
    BEGIN
        DECLARE CONTINUE HANDLER FOR 1091 BEGIN END;
        DROP INDEX idx_deduction_employee_id ON deduction_breakdown;
    END;
    CREATE INDEX idx_deduction_employee_id ON deduction_breakdown(employee_id);

    -- Handle deduction_breakdown.pay_period_end index
    BEGIN
        DECLARE CONTINUE HANDLER FOR 1091 BEGIN END;
        DROP INDEX idx_deduction_pay_period ON deduction_breakdown;
    END;
    CREATE INDEX idx_deduction_pay_period ON deduction_breakdown(pay_period_end);
END //

DELIMITER ;

-- Execute the procedure
CALL manage_indexes();

-- Clean up
DROP PROCEDURE IF EXISTS manage_indexes;