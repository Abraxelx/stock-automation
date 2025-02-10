package com.halilsahin.stockautomation.controller;

import com.halilsahin.stockautomation.constants.Constants;
import com.halilsahin.stockautomation.entity.Product;
import com.halilsahin.stockautomation.entity.SaleItem;
import com.halilsahin.stockautomation.service.SaleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/sales")
public class SalesController {
    private final SaleService saleService;
    private List<SaleItem> saleItems = new ArrayList<>();
    private double total = 0;

    @Autowired
    public SalesController(SaleService saleService) {
        this.saleService = saleService;
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
            total = saleItems.stream().mapToDouble(SaleItem::getSubtotal).sum();
            
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
            total = saleItems.stream().mapToDouble(SaleItem::getSubtotal).sum();
            
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

        total -= saleItem.getSubtotal(); // Toplam tutarı güncelle
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
            saleService.completeSale(saleItems, total, discountRate, finalTotal);
            saleItems.clear();
            total = 0;
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
        total = 0;
        return "redirect:/sales";
    }
}

