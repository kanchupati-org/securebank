INSERT INTO users (id, email, name, password_hash, role)
VALUES
    (5, 'pavan-secure@example.com', 'Pavan Secure', 'test-password-hash', 'CUSTOMER'),
    (7, 'user7@example.com', 'User Seven', 'test-password-hash', 'CUSTOMER');

INSERT INTO accounts (id, account_number, balance, user_id)
VALUES
    (1, 'SB100000000000000001', 10000.00, 5),
    (2, 'SB100000000000000002', 25000.00, 7);