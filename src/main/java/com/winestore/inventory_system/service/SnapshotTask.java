package com.winestore.inventory_system.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SnapshotTask {

    @Autowired
    private InventoryService inventoryService;

    // Cron expression for "Every day at Midnight"
    @Scheduled(cron = "0 0 0 * * *")
    public void performMidnightSnapshot() {
        System.out.println(">>> EXCISE AUDIT: Capturing Midnight Inventory Snapshot...");
        inventoryService.captureAllSnapshots();
    }
}