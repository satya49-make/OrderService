-- Insert additional seed data for OrderService (idempotent)
-- Adds 5 products, 5 payments and 5 orders (orders reference payments and products via subqueries)

START TRANSACTION;

-- Insert products if they don't already exist (checks product_name)
INSERT INTO products (product_name, description, price, available_quantity, status, created_at, updated_at)
SELECT 'Headphones', 'Over-ear noise-cancelling headphones', 129.99, 60, 'AVAILABLE', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE product_name = 'Headphones');

INSERT INTO products (product_name, description, price, available_quantity, status, created_at, updated_at)
SELECT 'Webcam', '1080p USB webcam with microphone', 49.99, 120, 'AVAILABLE', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE product_name = 'Webcam');

INSERT INTO products (product_name, description, price, available_quantity, status, created_at, updated_at)
SELECT 'Portable SSD', '1TB portable SSD, USB-C', 159.99, 40, 'AVAILABLE', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE product_name = 'Portable SSD');

INSERT INTO products (product_name, description, price, available_quantity, status, created_at, updated_at)
SELECT 'Smartphone Stand', 'Adjustable smartphone desk stand', 12.99, 300, 'AVAILABLE', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE product_name = 'Smartphone Stand');

INSERT INTO products (product_name, description, price, available_quantity, status, created_at, updated_at)
SELECT 'Bluetooth Speaker', 'Portable Bluetooth speaker with deep bass', 59.99, 150, 'AVAILABLE', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE product_name = 'Bluetooth Speaker');

-- Insert payments if transaction_id doesn't already exist
INSERT INTO payments (amount, status, payment_method, transaction_id, description, created_at, updated_at)
SELECT 129.99, 'SUCCESS', 'CREDIT_CARD', 'tx-1001-INSERT', 'Payment for Headphones order', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM payments WHERE transaction_id = 'tx-1001-INSERT');

INSERT INTO payments (amount, status, payment_method, transaction_id, description, created_at, updated_at)
SELECT 49.99, 'SUCCESS', 'DEBIT_CARD', 'tx-1002-INSERT', 'Payment for Webcam order', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM payments WHERE transaction_id = 'tx-1002-INSERT');

INSERT INTO payments (amount, status, payment_method, transaction_id, description, created_at, updated_at)
SELECT 159.99, 'PENDING', 'UPI', 'tx-1003-INSERT', 'Payment for Portable SSD order', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM payments WHERE transaction_id = 'tx-1003-INSERT');

INSERT INTO payments (amount, status, payment_method, transaction_id, description, created_at, updated_at)
SELECT 12.99, 'FAILED', 'WALLET', 'tx-1004-INSERT', 'Payment for Smartphone Stand order', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM payments WHERE transaction_id = 'tx-1004-INSERT');

INSERT INTO payments (amount, status, payment_method, transaction_id, description, created_at, updated_at)
SELECT 59.99, 'SUCCESS', 'NET_BANKING', 'tx-1005-INSERT', 'Payment for Bluetooth Speaker order', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM payments WHERE transaction_id = 'tx-1005-INSERT');

-- Insert orders only if there isn't already an order tied to the payment_id
INSERT INTO orders (product_id, quantity, total_price, status, customer_name, customer_email, shipping_address, payment_id, created_at, updated_at)
SELECT
  (SELECT product_id FROM products WHERE product_name = 'Headphones' LIMIT 1),
  1,
  (SELECT price FROM products WHERE product_name = 'Headphones' LIMIT 1) * 1,
  'CONFIRMED',
  'Charlie Brown',
  'charlie.brown@example.com',
  '100 Main St, Springfield',
  (SELECT payment_id FROM payments WHERE transaction_id = 'tx-1001-INSERT' LIMIT 1),
  NOW(), NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM orders o WHERE o.payment_id = (SELECT payment_id FROM payments WHERE transaction_id = 'tx-1001-INSERT' LIMIT 1)
);

INSERT INTO orders (product_id, quantity, total_price, status, customer_name, customer_email, shipping_address, payment_id, created_at, updated_at)
SELECT
  (SELECT product_id FROM products WHERE product_name = 'Webcam' LIMIT 1),
  2,
  (SELECT price FROM products WHERE product_name = 'Webcam' LIMIT 1) * 2,
  'CONFIRMED',
  'Diana Prince',
  'diana.prince@example.com',
  '200 Market Ave, Gotham',
  (SELECT payment_id FROM payments WHERE transaction_id = 'tx-1002-INSERT' LIMIT 1),
  NOW(), NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM orders o WHERE o.payment_id = (SELECT payment_id FROM payments WHERE transaction_id = 'tx-1002-INSERT' LIMIT 1)
);

INSERT INTO orders (product_id, quantity, total_price, status, customer_name, customer_email, shipping_address, payment_id, created_at, updated_at)
SELECT
  (SELECT product_id FROM products WHERE product_name = 'Portable SSD' LIMIT 1),
  1,
  (SELECT price FROM products WHERE product_name = 'Portable SSD' LIMIT 1) * 1,
  'PENDING',
  'Ethan Hunt',
  'ethan.hunt@example.com',
  '55 Mission Rd, Unknown',
  (SELECT payment_id FROM payments WHERE transaction_id = 'tx-1003-INSERT' LIMIT 1),
  NOW(), NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM orders o WHERE o.payment_id = (SELECT payment_id FROM payments WHERE transaction_id = 'tx-1003-INSERT' LIMIT 1)
);

INSERT INTO orders (product_id, quantity, total_price, status, customer_name, customer_email, shipping_address, payment_id, created_at, updated_at)
SELECT
  (SELECT product_id FROM products WHERE product_name = 'Smartphone Stand' LIMIT 1),
  3,
  (SELECT price FROM products WHERE product_name = 'Smartphone Stand' LIMIT 1) * 3,
  'PAYMENT_FAILED',
  'Fiona Apple',
  'fiona.apple@example.com',
  '78 Music Ln, Cityville',
  (SELECT payment_id FROM payments WHERE transaction_id = 'tx-1004-INSERT' LIMIT 1),
  NOW(), NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM orders o WHERE o.payment_id = (SELECT payment_id FROM payments WHERE transaction_id = 'tx-1004-INSERT' LIMIT 1)
);

INSERT INTO orders (product_id, quantity, total_price, status, customer_name, customer_email, shipping_address, payment_id, created_at, updated_at)
SELECT
  (SELECT product_id FROM products WHERE product_name = 'Bluetooth Speaker' LIMIT 1),
  1,
  (SELECT price FROM products WHERE product_name = 'Bluetooth Speaker' LIMIT 1) * 1,
  'CONFIRMED',
  'George Bailey',
  'george.bailey@example.com',
  '12 Bedford St, Smalltown',
  (SELECT payment_id FROM payments WHERE transaction_id = 'tx-1005-INSERT' LIMIT 1),
  NOW(), NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM orders o WHERE o.payment_id = (SELECT payment_id FROM payments WHERE transaction_id = 'tx-1005-INSERT' LIMIT 1)
);

COMMIT;

-- Return counts for verification
SELECT 'Inserted rows' as note;
SELECT COUNT(*) AS products_total FROM products;
SELECT COUNT(*) AS payments_total FROM payments;
SELECT COUNT(*) AS orders_total FROM orders;

