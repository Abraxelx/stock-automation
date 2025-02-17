-- Önce mevcut check constraint'i kaldır
ALTER TABLE transactions DROP CONSTRAINT IF EXISTS transactions_type_check;

-- Yeni değerleri içeren check constraint ekle
ALTER TABLE transactions ADD CONSTRAINT transactions_type_check 
CHECK (type IN ('SALE', 'STOCK_IN', 'STOCK_OUT', 'DEBT_IN', 'DEBT_OUT', 'CUSTOMER_ADD')); 