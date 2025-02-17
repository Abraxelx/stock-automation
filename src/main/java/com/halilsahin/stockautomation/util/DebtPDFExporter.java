package com.halilsahin.stockautomation.util;

import com.halilsahin.stockautomation.entity.Debt;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DebtPDFExporter {
    private List<Debt> debts;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public DebtPDFExporter(List<Debt> debts) {
        this.debts = debts;
    }

    private void writeTableHeader(PdfPTable table) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        cell.setPadding(5);

        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD);
        font.setColor(BaseColor.BLACK);

        cell.setPhrase(new Phrase("ID", font));
        table.addCell(cell);
        cell.setPhrase(new Phrase("Borçlu", font));
        table.addCell(cell);
        cell.setPhrase(new Phrase("Alacaklı", font));
        table.addCell(cell);
        cell.setPhrase(new Phrase("Tutar", font));
        table.addCell(cell);
        cell.setPhrase(new Phrase("Vade Tarihi", font));
        table.addCell(cell);
        cell.setPhrase(new Phrase("Durum", font));
        table.addCell(cell);
        cell.setPhrase(new Phrase("Ödeme Yöntemi", font));
        table.addCell(cell);
    }

    private void writeTableData(PdfPTable table) {
        for (Debt debt : debts) {
            table.addCell(String.valueOf(debt.getId()));
            table.addCell(debt.getCustomer().getFirstName() + " " + debt.getCustomer().getLastName());
            table.addCell(debt.getDirection().getDisplayName());
            table.addCell(String.format("%.2f TL", debt.getAmount()));
            table.addCell(debt.getDueDate().format(DATE_FORMATTER));
            table.addCell(debt.isPaid() ? "Ödendi" : "Ödenmedi");
            table.addCell(debt.getPaymentMethod() != null ? debt.getPaymentMethod().getDisplayName() : "-");
        }
    }

    public void export(HttpServletResponse response) throws IOException, DocumentException {
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, response.getOutputStream());

        document.open();
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD);
        font.setSize(18);

        Paragraph title = new Paragraph("Borç Listesi", font);
        title.setAlignment(Paragraph.ALIGN_CENTER);
        document.add(title);

        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100f);
        table.setSpacingBefore(10);

        writeTableHeader(table);
        writeTableData(table);

        document.add(table);
        document.close();
    }
} 