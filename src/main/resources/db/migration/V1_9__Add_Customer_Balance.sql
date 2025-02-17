-- Önce balance sütununu nullable olarak ekle
ALTER TABLE customer ADD COLUMN IF NOT EXISTS balance numeric(38,2);

-- Mevcut kayıtların balance değerini 0 olarak güncelle
UPDATE customer SET balance = 0 WHERE balance IS NULL;

-- Sonra balance sütununu NOT NULL yap
ALTER TABLE customer ALTER COLUMN balance SET NOT NULL; 