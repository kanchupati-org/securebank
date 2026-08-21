INSERT INTO users (id, email, name, password_hash, role)
VALUES
    (5, 'pavan-secure@example.com', 'Pavan Secure', 'test-password-hash', 'CUSTOMER'),
    (7, 'user7@example.com', 'User Seven', 'test-password-hash', 'CUSTOMER')
ON CONFLICT (id) DO UPDATE
SET email = EXCLUDED.email,
    name = EXCLUDED.name,
    password_hash = EXCLUDED.password_hash,
    role = EXCLUDED.role;

INSERT INTO accounts (id, account_number, balance, user_id)
VALUES
    (1, 'SB100000000000000001', 10000.00, 5),
    (2, 'SB100000000000000002', 25000.00, 7)
ON CONFLICT (id) DO UPDATE
SET account_number = EXCLUDED.account_number,
    balance = EXCLUDED.balance,
    user_id = EXCLUDED.user_id;
