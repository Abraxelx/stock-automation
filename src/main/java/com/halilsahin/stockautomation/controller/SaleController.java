package com.halilsahin.stockautomation.controller;

import com.halilsahin.stockautomation.entity.Sale;
import com.halilsahin.stockautomation.service.SaleService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class SaleController {

    private final SaleService saleService;

    public SaleController(SaleService saleService) {
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
} 