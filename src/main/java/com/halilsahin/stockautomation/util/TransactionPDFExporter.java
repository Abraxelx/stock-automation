package com.halilsahin.stockautomation.util;

import com.halilsahin.stockautomation.entity.Transaction;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class TransactionPDFExporter {
    private List<Transaction> transactions;
    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
    private static final Font HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
    private static final Font DATA_FONT = FontFactory.getFont(FontFactory.HELVETICA, 12);

    public TransactionPDFExporter(List<Transaction> transactions) {
        this.transactions = transactions;
    }

    private void writeTableHeader(PdfPTable table) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        cell.setPadding(5);

        cell.setPhrase(new Phrase("ID", HEADER_FONT));
        table.addCell(cell);

        cell.setPhrase(new Phrase("İşlem Tipi", HEADER_FONT));
        table.addCell(cell);

        cell.setPhrase(new Phrase("Açıklama", HEADER_FONT));
        table.addCell(cell);

        cell.setPhrase(new Phrase("Tutar (TL)", HEADER_FONT));
        table.addCell(cell);

        cell.setPhrase(new Phrase("Tarih", HEADER_FONT));
        table.addCell(cell);

        cell.setPhrase(new Phrase("Müşteri", HEADER_FONT));
        table.addCell(cell);

        cell.setPhrase(new Phrase("Ürün", HEADER_FONT));
        table.addCell(cell);
    }

    private void writeTableData(PdfPTable table) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        for (Transaction transaction : transactions) {
            table.addCell(new PdfPCell(new Phrase(transaction.getId().toString(), DATA_FONT)));
            
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
            table.addCell(new PdfPCell(new Phrase(typeName, DATA_FONT)));
            
            // Kısaltılmış açıklama (100 karakterle sınırlı)
            String description = transaction.getDescription();
            if (description != null && description.length() > 100) {
                description = description.substring(0, 97) + "...";
            }
            table.addCell(new PdfPCell(new Phrase(description, DATA_FONT)));
            
            // Tutar
            table.addCell(new PdfPCell(new Phrase(String.format("%.2f", transaction.getAmount()), DATA_FONT)));
            
            // Tarih
            String date = transaction.getDate() != null ? 
                transaction.getDate().format(formatter) : "-";
            table.addCell(new PdfPCell(new Phrase(date, DATA_FONT)));
            
            // Müşteri
            String customerName = transaction.getCustomer() != null ? 
                transaction.getCustomer().getFirstName() + " " + transaction.getCustomer().getLastName() : "-";
            table.addCell(new PdfPCell(new Phrase(customerName, DATA_FONT)));
            
            // Ürün
            String productName = transaction.getProduct() != null ? 
                transaction.getProduct().getName() : "-";
            table.addCell(new PdfPCell(new Phrase(productName, DATA_FONT)));
        }
    }

    public void export(HttpServletResponse response) throws DocumentException, IOException {
        Document document = new Document(PageSize.A4.rotate()); // Yatay sayfa
        PdfWriter.getInstance(document, response.getOutputStream());

        document.open();

        // Başlık
        Paragraph title = new Paragraph("İşlem Raporu", TITLE_FONT);
        title.setAlignment(Paragraph.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);

        // Rapor özeti
        Paragraph summary = new Paragraph();
        summary.add(new Phrase("Toplam İşlem Sayısı: " + transactions.size() + "\n", DATA_FONT));
        
        // İşlem tiplerine göre sayım
        long saleCount = transactions.stream().filter(t -> t.getType().toString().equals("SALE")).count();
        long stockInCount = transactions.stream().filter(t -> t.getType().toString().equals("STOCK_IN")).count();
        long stockOutCount = transactions.stream().filter(t -> t.getType().toString().equals("STOCK_OUT")).count();
        
        summary.add(new Phrase("Satış İşlemleri: " + saleCount + "\n", DATA_FONT));
        summary.add(new Phrase("Stok Giriş İşlemleri: " + stockInCount + "\n", DATA_FONT));
        summary.add(new Phrase("Stok Çıkış İşlemleri: " + stockOutCount + "\n", DATA_FONT));
        
        summary.setSpacingAfter(15);
        document.add(summary);

        // Tablo
        PdfPTable table = new PdfPTable(7); // 7 sütun
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        table.setWidths(new float[]{1, 2, 6, 2, 3, 3, 3}); // Sütun genişlikleri

        writeTableHeader(table);
        writeTableData(table);

        document.add(table);
        document.close();
    }
} 