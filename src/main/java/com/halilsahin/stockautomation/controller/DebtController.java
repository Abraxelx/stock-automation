package com.halilsahin.stockautomation.controller;

import com.halilsahin.stockautomation.constants.Constants;
import com.halilsahin.stockautomation.entity.*;
import com.halilsahin.stockautomation.enums.DebtDirection;
import com.halilsahin.stockautomation.enums.DebtType;
import com.halilsahin.stockautomation.enums.PaymentMethod;
import com.halilsahin.stockautomation.repository.DebtRepository;
import com.halilsahin.stockautomation.repository.TransactionRepository;
import com.halilsahin.stockautomation.service.*;
import com.halilsahin.stockautomation.util.DebtExcelExporter;
import com.halilsahin.stockautomation.util.DebtPDFExporter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import jakarta.servlet.http.HttpServletResponse;
import com.itextpdf.text.DocumentException;

@Controller
@RequestMapping("/debts")
public class DebtController {

    private final DebtService debtService;
    private final ProductService productService;
    private final CustomerService customerService;
    private final DocumentService documentService;
    private final TransactionRepository transactionRepository;
    private final DebtRepository debtRepository;
    private final TransactionService transactionService;

    @Autowired
    public DebtController(DebtService debtService, ProductService productService, CustomerService customerService, DocumentService documentService, TransactionRepository transactionRepository, DebtRepository debtRepository, TransactionService transactionService) {
        this.debtService = debtService;
        this.productService = productService;
        this.customerService = customerService;
        this.documentService = documentService;
        this.transactionRepository = transactionRepository;
        this.debtRepository = debtRepository;
        this.transactionService = transactionService;
    }

    // Borç Girişi
    @PostMapping("/add")
    public String addDebt(@RequestParam Long customerId,
                         @RequestParam double amount,
                         @RequestParam String dueDate,
                         @RequestParam DebtType debtType,
                         @RequestParam DebtDirection direction,
                         @RequestParam(required = false) MultipartFile documentFile,
                         @RequestParam(required = false) Integer installments,
                         Model model) {
        
        Customer customer = customerService.findCustomerById(customerId);
        if (customer == null) {
            model.addAttribute("error", "Müşteri bulunamadı");
            return Constants.DEBTS;
        }

        Debt debt = new Debt();
        debt.setCustomer(customer);
        debt.setAmount(amount);
        debt.setDueDate(LocalDateTime.parse(dueDate));
        debt.setDebtType(debtType);
        debt.setDirection(direction);
        debt.setPaid(false);
        debt.setCreatedAt(LocalDateTime.now());

        // Taksit işlemleri
        if (installments != null && installments > 1) {
            List<Installment> installmentList = new ArrayList<>();
            double installmentAmount = amount / installments;
            LocalDateTime installmentDueDate = debt.getDueDate();
            
            for (int i = 0; i < installments; i++) {
                Installment inst = new Installment();
                inst.setAmount(installmentAmount);
                inst.setDueDate(installmentDueDate.plusMonths(i));
                inst.setPaid(false);
                inst.setDebt(debt);
                installmentList.add(inst);
            }
            debt.setInstallments(installmentList);
        }

        // Belge işlemleri
        if (documentFile != null && !documentFile.isEmpty()) {
            try {
                Document document = new Document();
                String filePath = documentService.saveDocument(documentFile);
                document.setFileName(documentFile.getOriginalFilename());
                document.setFilePath(filePath);
                document.setFileType(documentFile.getContentType());
                document.setFileSize(documentFile.getSize() / 1024);
                document.setContentType(documentFile.getContentType());
                document.setDebt(debt);
                debt.getDocuments().add(document);
            } catch (IOException e) {
                model.addAttribute("error", "Belge kaydedilemedi: " + e.getMessage());
                return Constants.DEBTS;
            }
        }

        debtService.addDebt(debt);
        return "redirect:/debts";
    }

    // Ürün borcu için ayrı endpoint
    @PostMapping("/add-product-debt")
    public String addProductDebt(@RequestParam Long customerId,
                               @RequestParam String dueDate,
                               @RequestParam DebtDirection direction,
                               @RequestParam List<Long> productIds,
                               @RequestParam List<Integer> quantities,
                               @RequestParam List<BigDecimal> prices,
                               @RequestParam(required = false) Integer installments,
                               @RequestParam(required = false) MultipartFile documentFile,
                               Model model) {

        Customer customer = customerService.findCustomerById(customerId);
        if (customer == null) {
            model.addAttribute("error", "Müşteri bulunamadı");
            return Constants.DEBTS;
        }

        // Yeni borç kaydı oluştur
        Debt debt = new Debt();
        debt.setCustomer(customer);
        debt.setDueDate(LocalDateTime.parse(dueDate));
        debt.setDebtType(DebtType.PRODUCT); // Ürün borcu
        debt.setDirection(direction);
        debt.setPaid(false);
        debt.setCreatedAt(LocalDateTime.now());

        // Ürünleri ekle
        for (int i = 0; i < productIds.size(); i++) {
            Product product = productService.findById(productIds.get(i));
            if (product != null) {
                debt.addProduct(product, quantities.get(i), prices.get(i));
            }
        }

        // Taksit ve belge işlemleri aynı şekilde devam eder...
        // ...

        debtService.addDebt(debt);
        return "redirect:/debts";
    }

    // Borçları listeleme
    @GetMapping
    public String getAllDebts(
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String direction,
            Model model) {
        // Önce istatistikleri al
        Map<String, Object> stats = debtService.getDebtStatistics();
        if (stats == null) {
            stats = new HashMap<>();
            stats.put("totalDebtAmount", 0.0);
            stats.put("paidDebtAmount", 0.0);
            stats.put("unpaidDebtAmount", 0.0);
            stats.put("overdueDebts", new ArrayList<>());
            stats.put("upcomingDebts", new ArrayList<>());
        }

        List<Debt> debts;
        if (sortBy != null && !sortBy.isEmpty()) {
            debts = debtService.getAllDebtsSorted(sortBy, direction);
        } else {
            debts = debtService.getAllDebts();
        }

        model.addAttribute("debts", debts);
        model.addAttribute("customers", customerService.getAllCustomers());
        model.addAttribute("products", productService.findAll());
        model.addAttribute("stats", stats);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("direction", direction);
        return "debts";
    }

    // Borç Filtreleme
    @GetMapping("/filter")
    public String filterDebts(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String direction,
            @RequestParam(required = false) String debtType,
            @RequestParam(required = false) Boolean paid,
            @RequestParam(required = false) String dateRange,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false, defaultValue = "dueDate") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortDirection,
            Model model) {
        
        // İstatistikleri al
        Map<String, Object> stats = debtService.getDebtStatistics();
        
        // Filtreleme servis metodunu çağır
        List<Debt> filteredDebts = debtService.filterDebts(
            customerId, direction, debtType, paid, 
            dateRange, startDate, endDate, 
            sortBy, sortDirection
        );
        
        // Model'e verileri ekle
        model.addAttribute("debts", filteredDebts);
        model.addAttribute("customers", customerService.getAllCustomers());
        model.addAttribute("products", productService.findAll());
        model.addAttribute("stats", stats);
        
        // Filtreleme parametrelerini geri gönder
        model.addAttribute("customerId", customerId);
        model.addAttribute("direction", direction);
        model.addAttribute("debtType", debtType);
        model.addAttribute("paid", paid);
        model.addAttribute("dateRange", dateRange);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDirection", sortDirection);
        
        return "debts";
    }

    // Müşteriye göre borç arama
    @GetMapping("/search")
    public String searchDebts(@RequestParam String customerName, Model model) {
        model.addAttribute(Constants.DEBTS, debtService.findDebtsByCustomerName(customerName));
        model.addAttribute(Constants.PRODUCTS, productService.findAll());  // Ürünleri de model'e ekleyelim
        return Constants.DEBTS;
    }

    // Ödeme işlemi
    @PostMapping("/pay/{id}")
    public String payDebt(@PathVariable Long id, 
                     @RequestParam PaymentMethod paymentMethod,
                     @RequestParam BigDecimal amount) {
        debtService.payDebt(id, paymentMethod, amount);
        return "redirect:/debts";
    }

    // ÖDEME EKRANI: payment.html (Ödeme yapmak için ayrı ekran)
    @GetMapping("/payment/{id}")
    public String showPaymentForm(@PathVariable Long id, Model model) {
        Debt debt = debtService.findDebtById(id);
        model.addAttribute("debt", debt);
        model.addAttribute("paymentMethods", PaymentMethod.values());
        return "payments";
    }

    // Taksit ödeme işlemi
    @PostMapping("/pay-installment/{id}")
    public String payInstallment(@PathVariable Long id, Model model) {
        Installment installment = debtService.findInstallmentById(id);
        if (installment != null && !installment.isPaid()) {
            installment.setPaid(true);
            debtService.saveInstallment(installment);

            // Transaction kaydı
            Transaction transaction = Transaction.createDebtTransaction(
                null, // customer null for now
                BigDecimal.valueOf(-installment.getAmount()), // negative for DEBT_OUT
                BigDecimal.ZERO, // previous balance
                BigDecimal.ZERO  // new balance
            );
            transaction.setDescription("Taksit Ödendi: " + installment.getAmount() + " TL");
            transactionService.save(transaction);
        }

        assert installment != null;
        return "redirect:/debts/payment/" + installment.getDebt().getId();
    }

    @GetMapping("/document/{id}")
    public ResponseEntity<Resource> viewDocument(@PathVariable Long id) {
        Document document = documentService.findById(id);
        if (document == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            String filePath = "src/main/resources/static" + document.getFilePath();
            Path path = Paths.get(filePath);
            Resource resource = new UrlResource(path.toUri());

            if (resource.exists() && resource.isReadable()) {
                String contentType = Files.probeContentType(path);
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + document.getFileName() + "\"")
                        .body(resource);
            }
            return ResponseEntity.notFound().build();
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/document/upload/{debtId}")
    public String uploadDocuments(@PathVariable Long debtId, 
                                @RequestParam("files") MultipartFile[] files) {
        Debt debt = debtService.findDebtById(debtId);
        if (debt != null) {
            for (MultipartFile file : files) {
                try {
                    Document document = new Document();
                    String filePath = documentService.saveDocument(file);
                    document.setFileName(file.getOriginalFilename());
                    document.setFilePath(filePath);
                    document.setFileType(file.getContentType());
                    document.setFileSize(file.getSize() / 1024); // KB cinsinden
                    document.setContentType(file.getContentType());
                    document.setDebt(debt);
                    debt.getDocuments().add(document);
                } catch (IOException e) {
                    // Hata işleme
                }
            }
            debtService.addDebt(debt);
        }
        return "redirect:/debts/payment/" + debtId;
    }

    @GetMapping("/document/download/{id}")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long id) {
        Document document = documentService.findById(id);
        if (document == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            Path path = Paths.get("src/main/resources/static" + document.getFilePath());
            Resource resource = new UrlResource(path.toUri());

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(document.getContentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"" + document.getFileName() + "\"")
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/document/view/{id}")
    public String viewDocumentPage(@PathVariable Long id, Model model) {
        Document document = documentService.findById(id);
        if (document == null) {
            return "redirect:/debts";
        }
        model.addAttribute("document", document);
        return "document-viewer";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Debt debt = debtService.findDebtById(id);
        if (debt == null) {
            return "redirect:/debts";
        }
        
        model.addAttribute("debt", debt);
        model.addAttribute("customers", customerService.getAllCustomers());
        model.addAttribute("products", productService.findAll());
        return "edit-debt";
    }

    @PostMapping("/edit/{id}")
    public String updateDebt(@PathVariable Long id,
                            @RequestParam Long customerId,
                            @RequestParam DebtDirection direction,
                            @RequestParam double amount,
                            @RequestParam(required = false) Long productId,
                            @RequestParam String dueDate,
                            @RequestParam DebtType debtType,
                            @RequestParam(required = false) MultipartFile[] files,
                            Model model) {
        Debt debt = debtService.findDebtById(id);
        if (debt == null) {
            return "redirect:/debts";
        }

        // Temel bilgileri güncelle
        debt.setCustomer(customerService.findCustomerById(customerId));
        debt.setDirection(direction);
        debt.setAmount(amount);
        debt.setDueDate(LocalDateTime.parse(dueDate));
        debt.setDebtType(debtType);

        // Yeni belgeler ekle
        if (files != null && files.length > 0) {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    try {
                        Document document = new Document();
                        String filePath = documentService.saveDocument(file);
                        document.setFileName(file.getOriginalFilename());
                        document.setFilePath(filePath);
                        document.setFileType(file.getContentType());
                        document.setFileSize(file.getSize() / 1024);
                        document.setContentType(file.getContentType());
                        document.setDebt(debt);
                        debt.getDocuments().add(document);
                    } catch (IOException e) {
                        model.addAttribute("error", "Belge yüklenirken hata oluştu: " + e.getMessage());
                    }
                }
            }
        }

        debtService.addDebt(debt); // Güncelleme için aynı metodu kullanabiliriz
        return "redirect:/debts";
    }

    @PostMapping("/document/delete/{docId}")
    public String deleteDocument(@PathVariable Long docId) {
        Document document = documentService.findById(docId);
        if (document != null) {
            Long debtId = document.getDebt().getId();
            documentService.deleteDocument(docId);
            return "redirect:/debts/edit/" + debtId;
        }
        return "redirect:/debts";
    }

    @GetMapping("/export/pdf")
    public void exportToPDF(HttpServletResponse response) throws IOException, DocumentException {
        response.setContentType("application/pdf");
        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=borçlar_" + 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm")) + ".pdf";
        response.setHeader(headerKey, headerValue);

        List<Debt> debts = debtService.getAllDebts();
        DebtPDFExporter exporter = new DebtPDFExporter(debts);
        exporter.export(response);
    }

    @GetMapping("/export/excel")
    public void exportToExcel(HttpServletResponse response) throws IOException {
        response.setContentType("application/octet-stream");
        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=borçlar_" + 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm")) + ".xlsx";
        response.setHeader(headerKey, headerValue);

        List<Debt> debts = debtService.getAllDebts();
        DebtExcelExporter exporter = new DebtExcelExporter(debts);
        exporter.export(response);
    }

    // Borç detay sayfası
    @GetMapping("/detail/{id}")
    public String getDebtDetail(@PathVariable Long id, Model model) {
        Debt debt = debtService.findDebtById(id);
        if (debt == null) {
            return "redirect:/transactions";
        }
        
        model.addAttribute("debt", debt);
        return "debt-detail"; // debt-detail.html oluşturmamız gerekecek
    }

}
