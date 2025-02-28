package com.halilsahin.stockautomation.util;

import com.halilsahin.stockautomation.entity.Transaction;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class TransactionExcelExporter {
    private XSSFWorkbook workbook;
    private XSSFSheet sheet;
    private List<Transaction> transactions;

    public TransactionExcelExporter(List<Transaction> transactions) {
        this.transactions = transactions;
        workbook = new XSSFWorkbook();
    }

    private void writeHeaderRow() {
        sheet = workbook.createSheet("İşlemler");
        
        // Kolon genişliklerini ayarla
        sheet.setColumnWidth(0, 3000); // ID
        sheet.setColumnWidth(1, 5000); // İşlem Tipi
        sheet.setColumnWidth(2, 15000); // Açıklama
        sheet.setColumnWidth(3, 5000); // Tutar
        sheet.setColumnWidth(4, 8000); // Tarih
        sheet.setColumnWidth(5, 8000); // Müşteri
        sheet.setColumnWidth(6, 8000); // Ürün
        
        Row row = sheet.createRow(0);

        CellStyle style = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setBold(true);
        font.setFontHeight(14);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Cell cell = row.createCell(0);
        cell.setCellValue("ID");
        cell.setCellStyle(style);

        cell = row.createCell(1);
        cell.setCellValue("İşlem Tipi");
        cell.setCellStyle(style);

        cell = row.createCell(2);
        cell.setCellValue("Açıklama");
        cell.setCellStyle(style);

        cell = row.createCell(3);
        cell.setCellValue("Tutar (TL)");
        cell.setCellStyle(style);

        cell = row.createCell(4);
        cell.setCellValue("Tarih");
        cell.setCellStyle(style);

        cell = row.createCell(5);
        cell.setCellValue("Müşteri");
        cell.setCellStyle(style);

        cell = row.createCell(6);
        cell.setCellValue("Ürün");
        cell.setCellStyle(style);
    }

    private void writeDataRows() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        
        int rowCount = 1;
        CellStyle style = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setFontHeight(12);
        style.setFont(font);

        for (Transaction transaction : transactions) {
            Row row = sheet.createRow(rowCount++);

            Cell cell = row.createCell(0);
            cell.setCellValue(transaction.getId());
            cell.setCellStyle(style);

            cell = row.createCell(1);
            // İşlem Tipi
            String typeName = "";
            switch (transaction.getType().toString()) {
                case "SALE": typeName = "SATIŞ"; break;
                case "STOCK_IN": typeName = "STOK GİRİŞİ"; break;
                case "STOCK_OUT": typeName = "STOK ÇIKIŞI"; break;
                case "DEBT_IN": typeName = "BORÇ ALMA"; break;
                case "DEBT_OUT": typeName = "BORÇ VERME"; break;
                case "DEBT_PAYMENT": typeName = "BORÇ ÖDEMESİ"; break;
                case "DEBT_COLLECTION": typeName = "ALACAK TAHSİLATI"; break;
                case "CUSTOMER_ADD": typeName = "MÜŞTERİ KAYDI"; break;
                default: typeName = transaction.getType().toString();
            }
            cell.setCellValue(typeName);
            cell.setCellStyle(style);

            cell = row.createCell(2);
            cell.setCellValue(transaction.getDescription());
            cell.setCellStyle(style);

            cell = row.createCell(3);
            cell.setCellValue(transaction.getAmount().doubleValue());
            cell.setCellStyle(style);

            cell = row.createCell(4);
            String date = transaction.getDate() != null ? 
                transaction.getDate().format(formatter) : "-";
            cell.setCellValue(date);
            cell.setCellStyle(style);

            cell = row.createCell(5);
            String customerName = transaction.getCustomer() != null ? 
                transaction.getCustomer().getFirstName() + " " + transaction.getCustomer().getLastName() : "-";
            cell.setCellValue(customerName);
            cell.setCellStyle(style);

            cell = row.createCell(6);
            String productName = transaction.getProduct() != null ? 
                transaction.getProduct().getName() : "-";
            cell.setCellValue(productName);
            cell.setCellStyle(style);
        }
    }

    private void writeIstatistikRows() {
        // Yeni sayfa oluştur
        sheet = workbook.createSheet("İstatistikler");
        sheet.setColumnWidth(0, 8000);
        sheet.setColumnWidth(1, 5000);
        
        // Başlık satırı
        Row headerRow = sheet.createRow(0);
        CellStyle headerStyle = workbook.createCellStyle();
        XSSFFont headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeight(14);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        
        Cell headerCell = headerRow.createCell(0);
        headerCell.setCellValue("İstatistik");
        headerCell.setCellStyle(headerStyle);
        
        headerCell = headerRow.createCell(1);
        headerCell.setCellValue("Değer");
        headerCell.setCellStyle(headerStyle);
        
        // İstatistik satırları
        int rowNum = 1;
        CellStyle dataStyle = workbook.createCellStyle();
        XSSFFont dataFont = workbook.createFont();
        dataFont.setFontHeight(12);
        dataStyle.setFont(dataFont);
        
        // İşlem tiplerine göre sayım
        long saleCount = transactions.stream().filter(t -> t.getType().toString().equals("SALE")).count();
        long stockInCount = transactions.stream().filter(t -> t.getType().toString().equals("STOCK_IN")).count();
        long stockOutCount = transactions.stream().filter(t -> t.getType().toString().equals("STOCK_OUT")).count();
        long debtInCount = transactions.stream().filter(t -> t.getType().toString().equals("DEBT_IN")).count();
        long debtOutCount = transactions.stream().filter(t -> t.getType().toString().equals("DEBT_OUT")).count();
        
        // Toplam tutarları hesapla
        double totalSaleAmount = transactions.stream()
            .filter(t -> t.getType().toString().equals("SALE"))
            .mapToDouble(t -> t.getAmount().doubleValue())
            .sum();
        
        double totalStockInAmount = transactions.stream()
            .filter(t -> t.getType().toString().equals("STOCK_IN"))
            .mapToDouble(t -> t.getAmount().doubleValue())
            .sum();
        
        // İstatistikleri ekle
        addStatRow(rowNum++, "Toplam İşlem Sayısı", transactions.size(), dataStyle);
        addStatRow(rowNum++, "Satış İşlem Sayısı", saleCount, dataStyle);
        addStatRow(rowNum++, "Stok Giriş İşlem Sayısı", stockInCount, dataStyle);
        addStatRow(rowNum++, "Stok Çıkış İşlem Sayısı", stockOutCount, dataStyle);
        addStatRow(rowNum++, "Borç Alma İşlem Sayısı", debtInCount, dataStyle);
        addStatRow(rowNum++, "Borç Verme İşlem Sayısı", debtOutCount, dataStyle);
        
        rowNum++;
        addStatRow(rowNum++, "Toplam Satış Tutarı", String.format("%.2f TL", totalSaleAmount), dataStyle);
        addStatRow(rowNum++, "Toplam Stok Giriş Tutarı", String.format("%.2f TL", totalStockInAmount), dataStyle);
    }
    
    private void addStatRow(int rowNum, String label, Object value, CellStyle style) {
        Row row = sheet.createRow(rowNum);
        
        Cell cell = row.createCell(0);
        cell.setCellValue(label);
        cell.setCellStyle(style);
        
        cell = row.createCell(1);
        if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else {
            cell.setCellValue(value.toString());
        }
        cell.setCellStyle(style);
    }

    public void export(HttpServletResponse response) throws IOException {
        writeHeaderRow();
        writeDataRows();
        writeIstatistikRows();

        ServletOutputStream outputStream = response.getOutputStream();
        workbook.write(outputStream);
        workbook.close();

        outputStream.close();
    }
} 