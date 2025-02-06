package com.halilsahin.stockautomation.util;

import com.halilsahin.stockautomation.entity.Debt;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DebtExcelExporter {
    private List<Debt> debts;
    private XSSFWorkbook workbook;
    private Sheet sheet;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public DebtExcelExporter(List<Debt> debts) {
        this.debts = debts;
        workbook = new XSSFWorkbook();
    }

    private void writeHeaderLine() {
        sheet = workbook.createSheet("Borçlar");
        Row row = sheet.createRow(0);
        CellStyle style = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setBold(true);
        font.setFontHeight(14);
        style.setFont(font);

        createCell(row, 0, "ID", style);
        createCell(row, 1, "Borçlu", style);
        createCell(row, 2, "Alacaklı", style);
        createCell(row, 3, "Tutar", style);
        createCell(row, 4, "Vade Tarihi", style);
        createCell(row, 5, "Durum", style);
        createCell(row, 6, "Ödeme Yöntemi", style);
    }

    private void createCell(Row row, int columnCount, Object value, CellStyle style) {
        Cell cell = row.createCell(columnCount);
        if (value instanceof Integer) {
            cell.setCellValue((Integer) value);
        } else if (value instanceof Long) {
            cell.setCellValue((Long) value);
        } else if (value instanceof Double) {
            cell.setCellValue((Double) value);
        } else {
            cell.setCellValue((String) value);
        }
        cell.setCellStyle(style);
    }

    private void writeDataLines() {
        int rowCount = 1;
        CellStyle style = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setFontHeight(12);
        style.setFont(font);

        for (Debt debt : debts) {
            Row row = sheet.createRow(rowCount++);
            int columnCount = 0;

            createCell(row, columnCount++, debt.getId(), style);
            createCell(row, columnCount++, debt.getDebtor().getFirstName() + " " + debt.getDebtor().getLastName(), style);
            createCell(row, columnCount++, debt.getCreditor().getFirstName() + " " + debt.getCreditor().getLastName(), style);
            createCell(row, columnCount++, String.format("%.2f TL", debt.getAmount()), style);
            createCell(row, columnCount++, debt.getDueDate().format(DATE_FORMATTER), style);
            createCell(row, columnCount++, debt.isPaid() ? "Ödendi" : "Ödenmedi", style);
            createCell(row, columnCount++, debt.getPaymentMethod() != null ? debt.getPaymentMethod().getDisplayName() : "-", style);
        }
    }

    public void export(HttpServletResponse response) throws IOException {
        writeHeaderLine();
        writeDataLines();

        workbook.write(response.getOutputStream());
        workbook.close();
    }
} 