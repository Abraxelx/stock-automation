package com.halilsahin.stockautomation.controller;

import com.halilsahin.stockautomation.constants.Constants;
import com.halilsahin.stockautomation.entity.*;
import com.halilsahin.stockautomation.enums.DebtType;
import com.halilsahin.stockautomation.enums.TransactionType;
import com.halilsahin.stockautomation.repository.DebtRepository;
import com.halilsahin.stockautomation.repository.TransactionRepository;
import com.halilsahin.stockautomation.service.*;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/debts")
public class DebtController {

    private final DebtService debtService;
    private final ProductService productService;
    private final CustomerService customerService;
    private final DocumentService documentService;
    private final TransactionRepository transactionRepository;
    private final DebtRepository debtRepository;

    @Autowired
    public DebtController(DebtService debtService, ProductService productService, CustomerService customerService, DocumentService documentService, TransactionRepository transactionRepository, DebtRepository debtRepository) {
        this.debtService = debtService;
        this.productService = productService;
        this.customerService = customerService;
        this.documentService = documentService;
        this.transactionRepository = transactionRepository;
        this.debtRepository = debtRepository;
    }

    // Borç Girişi
    @PostMapping("/add")
    public String addDebt(@RequestParam Long debtorId,
                          @RequestParam Long creditorId,
                          @RequestParam double amount,
                          @RequestParam (required = false) Long productId,
                          @RequestParam String dueDate,
                          @RequestParam DebtType debtType,
                          @RequestParam(required = false) MultipartFile documentFile,
                          @RequestParam(required = false) Integer installments,
                          Model model) {
        Customer debtor = customerService.findCustomerById(debtorId);
        Customer creditor = customerService.findCustomerById(creditorId);
        if (debtor == null || creditor == null) {
            model.addAttribute("error", "Borçlu veya alacaklı müşteri bulunamadı.");
            return Constants.DEBTS;
        }
        Product product = (productId != null) ? productService.findById(productId) : null;


        Debt debt = new Debt();
        debt.setAmount(amount);
        debt.setDueDate(LocalDateTime.parse(dueDate));
        debt.setDebtType(debtType);  // Borç türünü ayarla
        debt.setDebtor(debtor);
        debt.setCreditor(creditor);
        debt.setProduct(product);
        debt.setPaid(false);

        // Taksit oluşturma: Eğer taksit sayısı girildiyse (1'den büyük), borcu eşit parçalara bölüyoruz.
        if (installments != null && installments > 1) {
            List<Installment> installmentList = new ArrayList<>();
            double installmentAmount = amount / installments;
            LocalDateTime installmentDueDate = debt.getDueDate();
            for (int i = 0; i < installments; i++) {
                Installment inst = new Installment();
                inst.setAmount(installmentAmount);
                // Her taksiti, başlangıç vadesine i ay ekleyerek ayarlıyoruz (örneğin aylık taksitler)
                inst.setDueDate(installmentDueDate.plusMonths(i));
                inst.setPaid(false);
                inst.setDebt(debt);
                inst.setDueDate(installmentDueDate.plusMonths(installments - 1));
                installmentList.add(inst);
            }
            debt.setInstallments(installmentList);
        }

        // Belge ekleme
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
                e.printStackTrace();
                model.addAttribute("error", "Belge kaydedilemedi: " + e.getMessage());
                return Constants.DEBTS;
            }
        }
        debtService.addDebt(debt);



        // Transaction kaydı
        transactionRepository.save(new Transaction(
                LocalDateTime.now(),
                debtor.getFirstName() + " " + debtor.getLastName() + " " + amount + " " + "BORÇ GİRİŞİ YAPTI",
                debt.getAmount(),
                TransactionType.DEBT_IN
        ));

        model.addAttribute(Constants.DEBTS, debtService.getAllDebts());
        model.addAttribute("customers", customerService.getAllCustomers());
        model.addAttribute(Constants.PRODUCTS, productService.findAll());// Ürünleri de model'e ekleyelim
        return Constants.DEBTS;
    }

    // Borçları listeleme
    @GetMapping
    public String getAllDebts(Model model) {
        model.addAttribute(Constants.DEBTS, debtService.getAllDebts());
        model.addAttribute("customers", customerService.getAllCustomers());
        model.addAttribute(Constants.PRODUCTS, productService.findAll());  // Ürünleri de model'e ekleyelim
        return Constants.DEBTS;
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
    public String payDebt(@PathVariable Long id, Model model) {
        Debt debtById = debtService.findDebtById(id);
        if (!debtById.isPaid()) {
            debtById.setPaid(true);
            debtById.setPaymentDate(LocalDateTime.now());
            debtRepository.save(debtById);
            // Transaction kaydı (Ödeme işlemi)
            transactionRepository.save(new Transaction(
                    LocalDateTime.now(),
                    "BORÇ ÖDEME YAPILDI: ",
                    debtById.getAmount(),
                    TransactionType.DEBT_OUT
            ));
        }

        model.addAttribute(Constants.DEBTS, debtService.getAllDebts());
        model.addAttribute("customers", customerService.getAllCustomers());
        model.addAttribute(Constants.PRODUCTS, productService.findAll());  // Ürünleri de model'e ekleyelim
        return Constants.DEBTS;
    }

    // ÖDEME EKRANI: payment.html (Ödeme yapmak için ayrı ekran)
    @GetMapping("/payment/{id}")
    public String paymentScreen(@PathVariable Long id, Model model) {
        Debt debt = debtService.findDebtById(id);
        if (debt == null) {
            model.addAttribute("error", "Borç bulunamadı.");
            return Constants.DEBTS;
        }
        model.addAttribute("debt", debt); // Borç bilgilerini gönder
        // Borç taksitli mi kontrol et
        if (debt.getInstallments() != null && !debt.getInstallments().isEmpty()) {
            model.addAttribute("installments", debt.getInstallments()); // Taksitleri gönder
        }
        return "payments"; // Ödeme sayfasına yönlendir
    }

    // Taksit ödeme işlemi
    @PostMapping("/pay-installment/{id}")
    public String payInstallment(@PathVariable Long id, Model model) {
        Installment installment = debtService.findInstallmentById(id);
        if (installment != null && !installment.isPaid()) {
            installment.setPaid(true);
            debtService.saveInstallment(installment);

            // Transaction kaydı
            transactionRepository.save(new Transaction(
                    LocalDateTime.now(),
                    "Taksit Ödendi: " + installment.getAmount() + " TL",
                    installment.getAmount(),
                    TransactionType.DEBT_OUT
            ));
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
                            @RequestParam Long debtorId,
                            @RequestParam Long creditorId,
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
        debt.setDebtor(customerService.findCustomerById(debtorId));
        debt.setCreditor(customerService.findCustomerById(creditorId));
        debt.setAmount(amount);
        debt.setProduct(productId != null ? productService.findById(productId) : null);
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

}
