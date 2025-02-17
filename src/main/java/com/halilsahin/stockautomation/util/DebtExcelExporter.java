package com.halilsahin.stockautomation.util;

import com.halilsahin.stockautomation.entity.Debt;
import com.halilsahin.stockautomation.entity.Installment;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DebtExcelExporter {
    private List<Debt> debts;
    private XSSFWorkbook workbook;
    private Sheet sheet;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public DebtExcelExporter(List<Debt> debts) {
        this.debts = debts;
        workbook = new XSSFWorkbook();
    }

    private void writeHeaderLine() {
        sheet = workbook.createSheet("Borçlar");
        Row row = sheet.createRow(0);
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        createCell(row, 0, "Müşteri", style);
        createCell(row, 1, "Borç Tipi", style);
        createCell(row, 2, "Yön", style);
        createCell(row, 3, "Tutar", style);
        createCell(row, 4, "Vade Tarihi", style);
        createCell(row, 5, "Durum", style);
        createCell(row, 6, "Ödeme Tarihi", style);
        createCell(row, 7, "Ödeme Yöntemi", style);
        createCell(row, 8, "Taksit Sayısı", style);
        createCell(row, 9, "Taksit Detayı", style);
    }

    private void createCell(Row row, int columnCount, Object value, CellStyle style) {
        Cell cell = row.createCell(columnCount);
        if (value instanceof String) {
            cell.setCellValue((String) value);
        } else if (value instanceof Double) {
            cell.setCellValue((Double) value);
        }
        cell.setCellStyle(style);
    }

    private void writeDataLines() {
        int rowCount = 1;
        CellStyle style = workbook.createCellStyle();
        style.setWrapText(true);

        CellStyle dateStyle = workbook.createCellStyle();
        dateStyle.setDataFormat(workbook.createDataFormat().getFormat("dd/MM/yyyy HH:mm"));

        CellStyle amountStyle = workbook.createCellStyle();
        amountStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00\" TL\""));

        for (Debt debt : debts) {
            Row row = sheet.createRow(rowCount++);

            // Müşteri bilgisi
            String customerName = debt.getCustomer() != null ? 
                debt.getCustomer().getFirstName() + " " + debt.getCustomer().getLastName() : "Müşteri Silinmiş";
            createCell(row, 0, customerName, style);

            // Borç tipi
            String debtType = debt.getDebtType() != null ? 
                debt.getDebtType().getDisplayName() : "-";
            createCell(row, 1, debtType, style);

            // Borç yönü
            String direction = debt.getDirection() != null ? 
                debt.getDirection().getDisplayName() : "-";
            createCell(row, 2, direction, style);

            // Tutar
            Cell amountCell = row.createCell(3);
            amountCell.setCellValue(debt.getAmount());
            amountCell.setCellStyle(amountStyle);

            // Vade tarihi
            if (debt.getDueDate() != null) {
                Cell dueDateCell = row.createCell(4);
                dueDateCell.setCellValue(java.util.Date.from(debt.getDueDate().atZone(java.time.ZoneId.systemDefault()).toInstant()));
                dueDateCell.setCellStyle(dateStyle);
            } else {
                createCell(row, 4, "-", style);
            }

            // Durum
            createCell(row, 5, debt.isPaid() ? "Ödendi" : "Ödenmedi", style);

            // Ödeme tarihi
            if (debt.getPaymentDate() != null) {
                Cell paymentDateCell = row.createCell(6);
                paymentDateCell.setCellValue(java.util.Date.from(debt.getPaymentDate().atZone(java.time.ZoneId.systemDefault()).toInstant()));
                paymentDateCell.setCellStyle(dateStyle);
            } else {
                createCell(row, 6, "-", style);
            }

            // Ödeme yöntemi
            createCell(row, 7, debt.getPaymentMethod() != null ? debt.getPaymentMethod().getDisplayName() : "-", style);

            // Taksit sayısı
            int installmentCount = debt.getInstallments() != null ? debt.getInstallments().size() : 0;
            createCell(row, 8, String.valueOf(installmentCount), style);

            // Taksit detayı
            if (installmentCount > 0) {
                StringBuilder installmentDetails = new StringBuilder();
                for (Installment inst : debt.getInstallments()) {
                    installmentDetails.append(String.format("Taksit %d: %.2f TL - %s - %s\n",
                        debt.getInstallments().indexOf(inst) + 1,
                        inst.getAmount(),
                        inst.getDueDate().format(formatter),
                        inst.isPaid() ? "Ödendi" : "Ödenmedi"
                    ));
                }
                createCell(row, 9, installmentDetails.toString(), style);
            } else {
                createCell(row, 9, "Taksit yok", style);
            }
        }

        // Sütun genişliklerini otomatik ayarla
        for (int i = 0; i < 10; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    public void export(HttpServletResponse response) throws IOException {
        writeHeaderLine();
        writeDataLines();

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        workbook.write(response.getOutputStream());
        workbook.close();
    }
} 