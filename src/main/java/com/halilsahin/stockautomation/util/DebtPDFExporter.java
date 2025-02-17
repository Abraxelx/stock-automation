package com.halilsahin.stockautomation.util;

import com.halilsahin.stockautomation.entity.Debt;
import com.halilsahin.stockautomation.entity.Installment;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DebtPDFExporter {
    private List<Debt> debts;
    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
    private static final Font HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
    private static final Font DATA_FONT = FontFactory.getFont(FontFactory.HELVETICA, 12);
    private static final Font INSTALLMENT_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10);

    public DebtPDFExporter(List<Debt> debts) {
        this.debts = debts;
    }

    private void writeTableHeader(PdfPTable table) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        cell.setPadding(5);

        cell.setPhrase(new Phrase("Müşteri", HEADER_FONT));
        table.addCell(cell);

        cell.setPhrase(new Phrase("Borç Tipi", HEADER_FONT));
        table.addCell(cell);

        cell.setPhrase(new Phrase("Yön", HEADER_FONT));
        table.addCell(cell);

        cell.setPhrase(new Phrase("Tutar", HEADER_FONT));
        table.addCell(cell);

        cell.setPhrase(new Phrase("Vade Tarihi", HEADER_FONT));
        table.addCell(cell);

        cell.setPhrase(new Phrase("Durum", HEADER_FONT));
        table.addCell(cell);

        cell.setPhrase(new Phrase("Ödeme Tarihi", HEADER_FONT));
        table.addCell(cell);

        cell.setPhrase(new Phrase("Taksitler", HEADER_FONT));
        table.addCell(cell);
    }

    private void writeTableData(PdfPTable table) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        for (Debt debt : debts) {
            // Müşteri bilgisi
            String customerName = debt.getCustomer() != null ? 
                debt.getCustomer().getFirstName() + " " + debt.getCustomer().getLastName() : "Müşteri Silinmiş";
            table.addCell(new PdfPCell(new Phrase(customerName, DATA_FONT)));

            // Borç tipi
            String debtType = debt.getDebtType() != null ? 
                debt.getDebtType().getDisplayName() : "-";
            table.addCell(new PdfPCell(new Phrase(debtType, DATA_FONT)));

            // Borç yönü
            String direction = debt.getDirection() != null ? 
                debt.getDirection().getDisplayName() : "-";
            table.addCell(new PdfPCell(new Phrase(direction, DATA_FONT)));

            // Tutar
            table.addCell(new PdfPCell(new Phrase(String.format("%.2f TL", debt.getAmount()), DATA_FONT)));

            // Vade tarihi
            String dueDate = debt.getDueDate() != null ? 
                debt.getDueDate().format(formatter) : "-";
            table.addCell(new PdfPCell(new Phrase(dueDate, DATA_FONT)));

            // Durum
            PdfPCell statusCell = new PdfPCell(new Phrase(debt.isPaid() ? "Ödendi" : "Ödenmedi", DATA_FONT));
            statusCell.setBackgroundColor(debt.isPaid() ? new BaseColor(144, 238, 144) : new BaseColor(255, 255, 224));
            table.addCell(statusCell);

            // Ödeme tarihi
            String paymentDate = debt.getPaymentDate() != null ? 
                debt.getPaymentDate().format(formatter) : "-";
            table.addCell(new PdfPCell(new Phrase(paymentDate, DATA_FONT)));

            // Taksit bilgileri
            PdfPCell installmentCell = new PdfPCell();
            if (debt.getInstallments() != null && !debt.getInstallments().isEmpty()) {
                StringBuilder installmentDetails = new StringBuilder();
                for (Installment inst : debt.getInstallments()) {
                    installmentDetails.append(String.format("Taksit %d: %.2f TL\n%s\n%s\n\n",
                        debt.getInstallments().indexOf(inst) + 1,
                        inst.getAmount(),
                        inst.getDueDate().format(formatter),
                        inst.isPaid() ? "Ödendi" : "Ödenmedi"
                    ));
                }
                installmentCell.setPhrase(new Phrase(installmentDetails.toString(), INSTALLMENT_FONT));
            } else {
                installmentCell.setPhrase(new Phrase("Taksit yok", DATA_FONT));
            }
            table.addCell(installmentCell);
        }
    }

    public void export(HttpServletResponse response) throws DocumentException, IOException {
        Document document = new Document(PageSize.A4.rotate()); // Yatay sayfa
        PdfWriter.getInstance(document, response.getOutputStream());

        document.open();

        // Başlık
        Paragraph title = new Paragraph("Borç Listesi", TITLE_FONT);
        title.setAlignment(Paragraph.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);

        // Tablo
        PdfPTable table = new PdfPTable(8); // 8 sütun
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        table.setWidths(new float[]{3, 2, 2, 2, 2, 2, 2, 4}); // Sütun genişlikleri

        writeTableHeader(table);
        writeTableData(table);

        document.add(table);
        document.close();
    }
} 