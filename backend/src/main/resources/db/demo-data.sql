-- Demo sales data (Q1-Q4 2025)
INSERT INTO sales (id, order_date, region, product, category, amount, quantity, status) VALUES
(1, '2025-01-05', 'North', 'Laptop', 'Electronics', 4500.00, 3, 'completed'),
(2, '2025-01-12', 'South', 'Phone', 'Electronics', 3200.00, 4, 'completed'),
(3, '2025-02-03', 'East', 'Laptop', 'Electronics', 6000.00, 4, 'completed'),
(4, '2025-02-18', 'North', 'Chair', 'Furniture', 800.00, 8, 'completed'),
(5, '2025-03-07', 'West', 'Phone', 'Electronics', 2400.00, 3, 'pending'),
(6, '2025-03-22', 'South', 'Desk', 'Furniture', 1500.00, 3, 'completed'),
(7, '2025-04-10', 'East', 'Monitor', 'Electronics', 3600.00, 6, 'completed'),
(8, '2025-04-25', 'North', 'Tablet', 'Electronics', 2200.00, 4, 'completed'),
(9, '2025-05-14', 'West', 'Laptop', 'Electronics', 7500.00, 5, 'completed'),
(10, '2025-05-28', 'South', 'Chair', 'Furniture', 1200.00, 12, 'completed'),
(11, '2025-06-09', 'East', 'Phone', 'Electronics', 4800.00, 6, 'cancelled'),
(12, '2025-06-24', 'North', 'Desk', 'Furniture', 2000.00, 4, 'completed'),
(13, '2025-07-11', 'West', 'Monitor', 'Electronics', 3000.00, 5, 'completed'),
(14, '2025-07-27', 'South', 'Tablet', 'Electronics', 2750.00, 5, 'completed'),
(15, '2025-08-15', 'East', 'Chair', 'Furniture', 900.00, 9, 'completed'),
(16, '2025-08-30', 'North', 'Phone', 'Electronics', 5600.00, 7, 'completed'),
(17, '2025-09-12', 'West', 'Desk', 'Furniture', 1800.00, 3, 'pending'),
(18, '2025-09-26', 'South', 'Laptop', 'Electronics', 9000.00, 6, 'completed'),
(19, '2025-10-18', 'East', 'Tablet', 'Electronics', 3300.00, 6, 'completed'),
(20, '2025-10-31', 'North', 'Monitor', 'Electronics', 4200.00, 7, 'completed'),
(21, '2025-11-14', 'West', 'Phone', 'Electronics', 4000.00, 5, 'completed'),
(22, '2025-11-28', 'South', 'Chair', 'Furniture', 1500.00, 15, 'completed'),
(23, '2025-12-09', 'East', 'Laptop', 'Electronics', 10500.00, 7, 'completed'),
(24, '2025-12-22', 'North', 'Tablet', 'Electronics', 3900.00, 6, 'completed');

-- Demo user data
INSERT INTO users (id, register_date, city, age, is_active, last_login) VALUES
(1, '2025-01-10', 'Beijing', 25, TRUE, '2025-12-20'),
(2, '2025-02-15', 'Shanghai', 32, TRUE, '2025-12-18'),
(3, '2025-03-05', 'Guangzhou', 28, FALSE, '2025-10-02'),
(4, '2025-04-20', 'Beijing', 41, TRUE, '2025-12-22'),
(5, '2025-05-11', 'Shenzhen', 23, TRUE, '2025-12-21'),
(6, '2025-06-01', 'Shanghai', 35, TRUE, '2025-12-19'),
(7, '2025-07-16', 'Chengdu', 29, FALSE, '2025-09-15'),
(8, '2025-08-08', 'Beijing', 38, TRUE, '2025-12-23'),
(9, '2025-09-25', 'Hangzhou', 26, TRUE, '2025-12-17'),
(10, '2025-10-30', 'Shanghai', 31, FALSE, '2025-11-05');
