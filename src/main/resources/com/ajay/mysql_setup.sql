-- Create the database
CREATE DATABASE grocery_shop_management;
USE grocery_shop_management;

-- Table: admins
CREATE TABLE admins (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(64) NOT NULL -- SHA-256 hash is 64 characters
);

-- Sample admin
INSERT INTO admins (username, password)
VALUES ('admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9');


-- Customer types table
CREATE TABLE IF NOT EXISTS customer_types (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Units table
CREATE TABLE IF NOT EXISTS units (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    symbol VARCHAR(10) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Categories table
CREATE TABLE IF NOT EXISTS categories (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Products table
CREATE TABLE IF NOT EXISTS products (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    barcode VARCHAR(50) UNIQUE,
    category_id INT NOT NULL,
    unit_id INT NOT NULL,
    purchase_price DECIMAL(10,2) NOT NULL,
    selling_price DECIMAL(10,2) NOT NULL,
    min_selling_price DECIMAL(10,2) NOT NULL,
    stock_quantity DECIMAL(10,3) NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES categories(id),
    FOREIGN KEY (unit_id) REFERENCES units(id)
);

-- Transactions table (replaces bills from previous version)
CREATE TABLE IF NOT EXISTS transactions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    customer_type_id INT NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    discount DECIMAL(10,2) DEFAULT 0,
    tax_amount DECIMAL(10,2) DEFAULT 0,
    final_amount DECIMAL(10,2) NOT NULL,
    payment_method VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_type_id) REFERENCES customer_types(id)
);

describe transactions;

ALTER TABLE transactions ADD COLUMN transaction_type VARCHAR(20) DEFAULT 'SALE';


-- Transaction items table
CREATE TABLE IF NOT EXISTS transaction_items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    transaction_id INT NOT NULL,
    product_id INT NOT NULL,
    quantity DECIMAL(10,3) NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    total_price DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id)
);

-- Insert default data
INSERT INTO customer_types (name) VALUES 
('Retail'), ('Wholesale'), ('Distributor'), ('Online');

INSERT INTO units (name, symbol) VALUES 
('Kilogram', 'kg'), ('Gram', 'g'), ('Liter', 'L'), 
('Piece', 'pc'), ('Pack', 'pk'), ('Dozen', 'dz');

INSERT INTO categories (name) VALUES 
('Groceries'), ('Electronics'), ('Dairy'), 
('Beverages'), ('Snacks'), ('Household');

select * from customer_types;

CREATE TABLE IF NOT EXISTS price_analysis (
    id INT AUTO_INCREMENT PRIMARY KEY,
    product_id INT NOT NULL,
    price_type ENUM('Standard', 'Promotional', 'Clearance', 'Seasonal') NOT NULL,
    notes TEXT,
    effective_date DATE NOT NULL,
    expiry_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS inventory_movements (
    id INT AUTO_INCREMENT PRIMARY KEY,
    product_id INT NOT NULL,
    movement_type ENUM('Purchase', 'Sale', 'Return', 'Adjustment', 'Wastage') NOT NULL,
    quantity DECIMAL(10,3) NOT NULL,
    reference_id INT, -- Can reference transactions or purchases
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE IF NOT EXISTS suppliers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    contact_person VARCHAR(100),
    phone VARCHAR(20),
    email VARCHAR(100),
    address TEXT,
    tax_id VARCHAR(50),
    payment_terms VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS purchase_orders (
    id INT AUTO_INCREMENT PRIMARY KEY,
    supplier_id INT NOT NULL,
    order_date DATE NOT NULL,
    expected_delivery_date DATE,
    status ENUM('Draft', 'Ordered', 'Received', 'Cancelled') DEFAULT 'Draft',
    total_amount DECIMAL(10,2) NOT NULL,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id)
);

CREATE TABLE IF NOT EXISTS purchase_order_items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    purchase_order_id INT NOT NULL,
    product_id INT NOT NULL,
    quantity DECIMAL(10,3) NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    total_price DECIMAL(10,2) NOT NULL,
    received_quantity DECIMAL(10,3) DEFAULT 0,
    FOREIGN KEY (purchase_order_id) REFERENCES purchase_orders(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE IF NOT EXISTS customers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(100),
    address TEXT,
    customer_type_id INT,
    tax_id VARCHAR(50),
    loyalty_points INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_type_id) REFERENCES customer_types(id)
);

DELIMITER //
CREATE PROCEDURE update_product_stock(
    IN p_product_id INT,
    IN p_quantity DECIMAL(10,3),
    IN p_movement_type VARCHAR(20)
)
BEGIN
    DECLARE current_stock DECIMAL(10,3);
    
    -- Get current stock
    SELECT stock_quantity INTO current_stock FROM products WHERE id = p_product_id;
    
    -- Update stock based on movement type
    IF p_movement_type = 'Purchase' OR p_movement_type = 'Return' THEN
        UPDATE products 
        SET stock_quantity = current_stock + p_quantity
        WHERE id = p_product_id;
    ELSEIF p_movement_type = 'Sale' OR p_movement_type = 'Wastage' THEN
        UPDATE products 
        SET stock_quantity = current_stock - p_quantity
        WHERE id = p_product_id;
    END IF;
    
    -- Record the movement
    INSERT INTO inventory_movements (product_id, movement_type, quantity)
    VALUES (p_product_id, p_movement_type, p_quantity);
END //
DELIMITER ;

DELIMITER //
CREATE PROCEDURE generate_sales_report(
    IN p_start_date DATE,
    IN p_end_date DATE,
    IN p_customer_type_id INT
)
BEGIN
    SELECT 
        t.id AS transaction_id,
        t.transaction_date,
        ct.name AS customer_type,
        COUNT(ti.id) AS items_count,
        t.total_amount,
        t.discount,
        t.tax_amount,
        t.final_amount,
        t.payment_method
    FROM 
        transactions t
    JOIN 
        customer_types ct ON t.customer_type_id = ct.id
    LEFT JOIN 
        transaction_items ti ON t.id = ti.transaction_id
    WHERE 
        t.transaction_date BETWEEN p_start_date AND p_end_date
        AND (p_customer_type_id IS NULL OR t.customer_type_id = p_customer_type_id)
    GROUP BY 
        t.id
    ORDER BY 
        t.transaction_date DESC;
END //
DELIMITER ;

CREATE VIEW low_stock_products AS
SELECT 
    p.id,
    p.name,
    c.name AS category,
    u.name AS unit,
    p.stock_quantity,
    p.min_selling_price
FROM 
    products p
JOIN 
    categories c ON p.category_id = c.id
JOIN 
    units u ON p.unit_id = u.id
WHERE 
    p.stock_quantity < 10; -- Adjust threshold as needed
    
    CREATE VIEW daily_sales_summary AS
SELECT 
    DATE(t.transaction_date) AS sale_date,
    COUNT(DISTINCT t.id) AS transactions_count,
    SUM(t.final_amount) AS total_sales,
    AVG(t.final_amount) AS average_sale,
    MAX(t.final_amount) AS highest_sale,
    MIN(t.final_amount) AS lowest_sale,
    COUNT(DISTINCT ti.product_id) AS unique_products_sold,
    SUM(ti.quantity) AS total_items_sold
FROM 
    transactions t
JOIN 
    transaction_items ti ON t.id = ti.transaction_id
WHERE 
    t.transaction_type = 'SALE'
GROUP BY 
    DATE(t.transaction_date)
ORDER BY 
    sale_date DESC;
    
    DELIMITER //
CREATE TRIGGER after_transaction_item_insert
AFTER INSERT ON transaction_items
FOR EACH ROW
BEGIN
    CALL update_product_stock(NEW.product_id, NEW.quantity, 'Sale');
END //
DELIMITER ;

DELIMITER //
CREATE TRIGGER after_purchase_order_item_update
AFTER UPDATE ON purchase_order_items
FOR EACH ROW
BEGIN
    DECLARE quantity_diff DECIMAL(10,3);
    
    IF NEW.received_quantity > OLD.received_quantity THEN
        SET quantity_diff = NEW.received_quantity - OLD.received_quantity;
        CALL update_product_stock(NEW.product_id, quantity_diff, 'Purchase');
    END IF;
END //
DELIMITER ;

-- Insert sample suppliers
INSERT INTO suppliers (name, contact_person, phone, email) VALUES 
('ABC Wholesalers', 'John Doe', '9876543210', 'john@abc.com'),
('XYZ Distributors', 'Jane Smith', '8765432109', 'jane@xyz.com');

-- Insert sample products
INSERT INTO products (name, category_id, unit_id, purchase_price, selling_price, min_selling_price) VALUES
('Premium Rice 5kg', 1, 1, 200.00, 250.00, 230.00),
('Fresh Milk 1L', 3, 3, 50.00, 60.00, 55.00),
('Chocolate Cookies', 5, 4, 20.00, 30.00, 25.00);

-- Insert sample transactions
INSERT INTO transactions (customer_type_id, total_amount, final_amount, payment_method) VALUES
(1, 250.00, 250.00, 'Cash'),
(2, 120.00, 120.00, 'Card');

-- Insert transaction items
INSERT INTO transaction_items (transaction_id, product_id, quantity, unit_price, total_price) VALUES
(1, 1, 1, 250.00, 250.00),
(2, 2, 2, 60.00, 120.00);

CREATE TABLE product_variations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    product_id INT NOT NULL,
    variation_name VARCHAR(255),
    price DECIMAL(10,2),
    quantity INT,
    FOREIGN KEY (product_id) REFERENCES products(id)
);

SELECT * FROM products pv WHERE pv.stock_quantity > 0;

DESCRIBE product_variations;

SELECT COUNT(*) 
FROM product_variations pv 
JOIN products p ON pv.product_id = p.id 
WHERE pv.quantity <= p.stock_quantity;



ALTER TABLE product_variations 
ADD COLUMN quantity_in_stock INT;

-- Drop the existing table if it exists
DROP TABLE IF EXISTS product_variations;

-- Create the corrected product_variations table
CREATE TABLE product_variations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    product_id INT NOT NULL,
    variation_name VARCHAR(255),
    price DECIMAL(10,2),
    quantity_in_stock INT,  -- Changed from 'quantity' to 'quantity_in_stock' for clarity
    min_stock_level INT DEFAULT 5,  -- Added minimum stock level
    FOREIGN KEY (product_id) REFERENCES products(id)
);

desc transactions;

ALTER TABLE products ADD FOREIGN KEY (customer_type_id) REFERENCES customer_types(id);

-- Then modify the column to have a default
ALTER TABLE units MODIFY COLUMN symbol VARCHAR(10) DEFAULT '';

-- First check if transaction_id column exists
SELECT COUNT(*) FROM information_schema.columns 
WHERE table_name = 'transactions' AND column_name = 'transaction_id';


ALTER TABLE products 
MODIFY COLUMN stock_quantity DECIMAL(10,3) NOT NULL DEFAULT 0,
MODIFY COLUMN purchase_price DECIMAL(10,2) NOT NULL DEFAULT 0,
MODIFY COLUMN selling_price DECIMAL(10,2) NOT NULL DEFAULT 0,
MODIFY COLUMN min_selling_price DECIMAL(10,2) NOT NULL DEFAULT 0;

ALTER TABLE price_analysis 
MODIFY COLUMN effective_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;

select * from products;

-- Add customer_type_id column to products table if it doesn't exist
ALTER TABLE products 
ADD COLUMN customer_type_id INT,
ADD FOREIGN KEY (customer_type_id) REFERENCES customer_types(id);

-- Update existing products with default customer type (Retail)
UPDATE products SET customer_type_id = 1 WHERE customer_type_id IS NULL;

-- Make sure price_analysis table has the correct structure
ALTER TABLE price_analysis 
MODIFY COLUMN price_type VARCHAR(20) NOT NULL DEFAULT 'Standard',
MODIFY COLUMN effective_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Add a search_key column to store normalized product names/IDs for searching
ALTER TABLE products ADD COLUMN search_key VARCHAR(255);

-- Create an index for faster searching
CREATE INDEX idx_products_search_key ON products(search_key);

-- Update all existing products with normalized search keys
SET SQL_SAFE_UPDATES = 0;

UPDATE products
SET search_key = LOWER(REGEXP_REPLACE(name, '[^a-zA-Z0-9]', ''));

SET SQL_SAFE_UPDATES = 1;  -- Optional: turn it back on



-- For MySQL versions without REGEXP_REPLACE, use:
SET SQL_SAFE_UPDATES = 0;

UPDATE products
SET search_key = LOWER(REPLACE(REPLACE(REPLACE(REPLACE(name, '-', ''), ' ', ''), '_', ''), '.', ''));

SET SQL_SAFE_UPDATES = 1;  -- Optional: turn it back on

desc products;

CREATE TABLE inventory_items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    current_stock DOUBLE NOT NULL,
    alert_threshold DOUBLE NOT NULL,
    category_id INT,
    unit_id INT,
    FOREIGN KEY (category_id) REFERENCES categories(id),
    FOREIGN KEY (unit_id) REFERENCES units(id)
);

CREATE TABLE IF NOT EXISTS alert_config (
    email TEXT,
    phone TEXT,
    email_threshold REAL DEFAULT 10.0,
    sms_threshold REAL DEFAULT 3.0
);

CREATE TABLE udhar_khata (
    id INT AUTO_INCREMENT PRIMARY KEY,
    customer_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    product_name VARCHAR(100) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    paid_amount DECIMAL(10,2) NOT NULL,
    date VARCHAR(20) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Price history table
CREATE TABLE price_history (
    id INT AUTO_INCREMENT PRIMARY KEY,
    product_name VARCHAR(100) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    price_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);


-- Inventory history table
CREATE TABLE inventory_history (
    id INT AUTO_INCREMENT PRIMARY KEY,
    product_name VARCHAR(100) NOT NULL,
    stock_quantity INT NOT NULL,
    last_updated DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Inventory table (for current stock)
CREATE TABLE inventory (
    id INT AUTO_INCREMENT PRIMARY KEY,
    product_name VARCHAR(100) NOT NULL,
    stock_quantity INT NOT NULL,
    last_updated DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);


-- Sample data for testing
INSERT INTO price_history (product_name, price) VALUES 
('Rice', 45.50), ('Rice', 46.00), ('Rice', 45.75),
('Wheat', 30.25), ('Wheat', 31.00), ('Wheat', 30.50);

INSERT INTO inventory_history (product_name, stock_quantity) VALUES 
('Rice', 100), ('Rice', 95), ('Rice', 85),
('Wheat', 150), ('Wheat', 140), ('Wheat', 130);

INSERT INTO inventory (product_name, stock_quantity) VALUES 
('Rice', 85), ('Wheat', 130), ('Sugar', 45), ('Oil', 60);

-- Insert sample data if tables are empty
INSERT INTO price_history (product_name, price) 
SELECT 'Rice', 45.50 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM price_history LIMIT 1);
INSERT INTO price_history (product_name, price) 
SELECT 'Rice', 46.00 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM price_history LIMIT 1);
INSERT INTO price_history (product_name, price) 
SELECT 'Wheat', 30.25 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM price_history LIMIT 1);

INSERT INTO inventory (product_name, stock_quantity) 
SELECT 'Rice', 85 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM inventory LIMIT 1);
INSERT INTO inventory (product_name, stock_quantity) 
SELECT 'Wheat', 130 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM inventory LIMIT 1);

SELECT @@sql_mode;

ALTER TABLE price_history
ADD COLUMN product_id INT;

ALTER TABLE price_history MODIFY product_name VARCHAR(255) DEFAULT 'Unknown';

INSERT INTO price_history (product_id, price, price_date)
VALUES
(1, 120.5, NOW() - INTERVAL 1 DAY),
(1, 121.0, NOW() - INTERVAL 2 DAY),
(1, 119.8, NOW() - INTERVAL 3 DAY);

-- Clean up the udhar_khata table structure
ALTER TABLE udhar_khata 
DROP COLUMN  amount,
DROP COLUMN  product_name,
DROP COLUMN date;

-- Make sure we have the right columns
ALTER TABLE udhar_khata
MODIFY COLUMN total_credit DECIMAL(10,2) NOT NULL DEFAULT 0,
MODIFY COLUMN paid_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
MODIFY COLUMN last_transaction_date DATE;

-- Clear existing confusing data
TRUNCATE TABLE udhar_khata;

-- Insert proper sample data
INSERT INTO udhar_khata (customer_name, phone, total_credit, paid_amount, last_transaction_date) 
VALUES
('Rajesh Kumar', '9876543210', 500.00, 350.00, '2025-05-10'),
('Priya Sharma', '8765432109', 1000.00, 800.00, '2025-05-12'),
('Amit Verma', '7654321098', 300.00, 200.00, '2025-05-05'),
('Sneha Gupta', '6543210987', 750.00, 500.00, '2025-05-11'),
('Vikram Singh', '5432109876', 400.00, 300.00, '2025-05-08'),
('Neha Patel', '4321098765', 600.00, 400.00, '2025-05-09'),
('Rahul Mehta', '3210987654', 450.00, 300.00, '2025-05-07'),
('Anjali Desai', '2109876543', 350.00, 200.00, '2025-05-06'),
('Sanjay Joshi', '1098765432', 800.00, 500.00, '2025-05-04'),
('Pooja Reddy', '0987654321', 900.00, 600.00, '2025-05-03'),
('Test Customer', '1111111111', 200.00, 200.00, '2025-05-01'); -- This one won't show (fully paid)

ALTER TABLE udhar_khata
ADD COLUMN product_name VARCHAR(100);

ALTER TABLE udhar_khata
ADD COLUMN amount DECIMAL(10,2);

ALTER TABLE udhar_khata
ADD COLUMN date DATE;


-- Bills table
CREATE TABLE IF NOT EXISTS bills (
    id INT AUTO_INCREMENT PRIMARY KEY,
    total DECIMAL(10,2) NOT NULL,
    profit DECIMAL(10,2) NOT NULL,
    purchase_total DECIMAL(10,2) NOT NULL,
    bill_date TIMESTAMP NOT NULL
);

-- Bill items table
CREATE TABLE IF NOT EXISTS bill_items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    bill_id INT NOT NULL,
    product_name VARCHAR(100) NOT NULL,
    quantity DECIMAL(10,3) NOT NULL,
    unit VARCHAR(50) NOT NULL,
    selling_price DECIMAL(10,2) NOT NULL,
    purchase_price DECIMAL(10,2) NOT NULL,
    customer_type VARCHAR(20) NOT NULL,
    FOREIGN KEY (bill_id) REFERENCES bills(id)
);


SELECT * FROM udhar_khata;

desc udhar_khata;

SELECT COALESCE(payment_method, 'Unknown') as payment_method, 
       SUM(total_amount) as amount 
FROM transactions 
WHERE transaction_date >= DATE_SUB(CURDATE(), INTERVAL 7 DAY) 
GROUP BY COALESCE(payment_method, 'Unknown')