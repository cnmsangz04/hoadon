package vn.hoadon.controller;

import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/invoices/init")
public class InvoiceController {

    @PostMapping
    public List<Map<String, Object>> getInvoices() {
        List<Map<String, Object>> invoices = new ArrayList<>();

        Map<String, Object> inv1 = new HashMap<>();
        inv1.put("id", 1);
        inv1.put("name", "Hóa đơn A");
        inv1.put("amount", 100000);
        inv1.put("date", "2025-11-13");

        Map<String, Object> inv2 = new HashMap<>();
        inv2.put("id", 2);
        inv2.put("name", "Hóa đơn B");
        inv2.put("amount", 250000);
        inv2.put("date", "2025-11-14");

        invoices.add(inv1);
        invoices.add(inv2);

        return invoices;
    }
}
