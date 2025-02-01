package com.halilsahin.stockautomation.controller;

import com.halilsahin.stockautomation.constants.Constants;
import com.halilsahin.stockautomation.entity.*;
import com.halilsahin.stockautomation.enums.DebtType;
import com.halilsahin.stockautomation.enums.TransactionType;
import com.halilsahin.stockautomation.repository.DebtRepository;
import com.halilsahin.stockautomation.repository.TransactionRepository;
import com.halilsahin.stockautomation.service.CustomerService;
import com.halilsahin.stockautomation.service.DebtService;
import com.halilsahin.stockautomation.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/debts")
public class DebtController {

    private final DebtService debtService;
    private final ProductService productService;
    private final CustomerService customerService;
    private final TransactionRepository transactionRepository;
    private final DebtRepository debtRepository;

    @Autowired
    public DebtController(DebtService debtService, ProductService productService, CustomerService customerService, TransactionRepository transactionRepository, DebtRepository debtRepository) {
        this.debtService = debtService;
        this.productService = productService;
        this.customerService = customerService;
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
                          Model model) {
        Customer debtor = customerService.findCustomerById(debtorId);
        Customer creditor = customerService.findCustomerById(creditorId);
        Product product = (productId != null) ? productService.findById(productId) : null;


        Debt debt = new Debt();
        debt.setAmount(amount);
        debt.setDueDate(LocalDateTime.parse(dueDate));
        debt.setDebtType(debtType);  // Borç türünü ayarla
        debt.setDebtor(debtor);
        debt.setCreditor(creditor);
        debt.setProduct(product);
        debt.setPaid(false);

        // Belge ekleme
        if (documentFile != null && !documentFile.isEmpty()) {
            Document document = new Document();
            document.setDebt(debt);
            // Dosya kaydetme işlemi
            document.setFileName(documentFile.getOriginalFilename());
            // Örneğin, dosya resources/images altında saklanabilir.
            String filePath = "images/" + documentFile.getOriginalFilename();
            document.setFilePath(filePath);
            // Burada dosyayı kaydedebilirsin, örneğin resources altına.
            debt.setDocument(document);
        }
        debtService.addDebt(debt);



        // Transaction kaydı
        transactionRepository.save(new Transaction(
                LocalDateTime.now(),
                debtor.getFirstName() + " " + debtor.getLastName() + " " + amount + " " + "BORÇ GİRİŞİ YAPTI",
                "DEBT",
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
                    "ÖDEME YAPILDI: " + debtById.getAmount(),
                    "DEBT",
                    TransactionType.DEBT_OUT
            ));
        }

        model.addAttribute(Constants.DEBTS, debtService.getAllDebts());
        model.addAttribute("customers", customerService.getAllCustomers());
        model.addAttribute(Constants.PRODUCTS, productService.findAll());  // Ürünleri de model'e ekleyelim
        return Constants.DEBTS;
    }
}
