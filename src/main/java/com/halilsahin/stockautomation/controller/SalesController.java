package com.halilsahin.stockautomation.controller;

import com.halilsahin.stockautomation.constants.Constants;
import com.halilsahin.stockautomation.entity.Product;
import com.halilsahin.stockautomation.entity.Sale;
import com.halilsahin.stockautomation.entity.SaleItem;
import com.halilsahin.stockautomation.service.SaleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/sales")
public class SalesController {
    private final SaleService saleService;
    private List<SaleItem> saleItems = new ArrayList<>();
    private BigDecimal total = BigDecimal.ZERO;

    @Autowired
    public SalesController(SaleService saleService) {
        this.saleService = saleService;
    }

    @GetMapping("/detail/{id}")
    public String getSaleDetail(@PathVariable Long id, Model model) {
        Sale sale = saleService.findById(id);
        if (sale == null) {
            return "redirect:/transactions";
        }
        model.addAttribute("sale", sale);
        return "sale-detail";
    }

    // Satış Sayfasını Görüntüleme
    @GetMapping
    public String showSalesPage(Model model) {
        model.addAttribute(Constants.SALE_ITEMS, saleItems);
        model.addAttribute(Constants.TOTAL, total);
        return Constants.SALES;
    }

    // Sepete Ürün Ekleme
    @PostMapping
    public String addItemToSale(@RequestParam String barcode, @RequestParam int quantity, RedirectAttributes redirectAttributes) {
        try {
            SaleItem saleItem = saleService.addItemToSale(barcode, quantity, saleItems);
            
            // Eğer yeni ürün eklendiyse listeye ekle
            if (!saleItems.contains(saleItem)) {
                saleItems.add(saleItem);
            }
            
            // Toplam tutarı yeniden hesapla
            total = saleItems.stream()
                .map(SaleItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/sales";
        }

        return "redirect:/sales";
    }

    @PostMapping("/updateQuantity")
    public String updateQuantity(@RequestParam String barcode, @RequestParam int quantity, RedirectAttributes redirectAttributes) {
        try {
            saleService.updateItemQuantity(barcode, quantity, saleItems);
            
            // Toplam tutarı yeniden hesapla
            total = saleItems.stream()
                .map(SaleItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/sales";
        }

        return "redirect:/sales";
    }

    @GetMapping("/item/{barcode}")
    public String removeItemFromSale(@PathVariable String barcode, Model model) {
        // Listeden ilgili ürünü bul ve çıkar
        SaleItem saleItem = saleItems.stream()
                .filter(item -> item.getProduct().getBarcode().equals(barcode))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Ürün bulunamadı: " + barcode));

        total = total.subtract(saleItem.getSubtotal()); // Toplam tutarı güncelle
        saleItems.remove(saleItem); // Ürünü listeden çıkar

        // Görünümü güncelle
        model.addAttribute(Constants.SALE_ITEMS, saleItems);
        model.addAttribute(Constants.TOTAL, total);
        return "redirect:/sales";
    }

    // Satışı Tamamlama
    @PostMapping("/complete")
    public String completeSale(
            @RequestParam(defaultValue = "0") double discountRate,
            @RequestParam double finalTotal,
            RedirectAttributes redirectAttributes) {
        try {
            saleService.completeSale(saleItems, total.doubleValue(), discountRate, finalTotal);
            saleItems.clear();
            total = BigDecimal.ZERO;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Satış tamamlanamadı: " + e.getMessage());
            return "redirect:/sales";
        }

        return "redirect:/sales";
    }

    // Sepeti Temizleme
    @GetMapping("/reset")
    public String resetSale() {
        saleItems.clear();
        total = BigDecimal.ZERO;
        return "redirect:/sales";
    }
}

