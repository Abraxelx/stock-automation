-- Önce registration_date sütununu nullable olarak ekle
ALTER TABLE customer ADD COLUMN IF NOT EXISTS registration_date timestamp;

-- Mevcut kayıtların registration_date değerini şu anki tarih olarak güncelle
UPDATE customer SET registration_date = CURRENT_TIMESTAMP WHERE registration_date IS NULL;

-- Sonra registration_date sütununu NOT NULL yap
ALTER TABLE customer ALTER COLUMN registration_date SET NOT NULL; 