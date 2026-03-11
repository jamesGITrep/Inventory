package com.inventory.system.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.inventory.system.entity.PurchaseRecord;

@Repository
public interface PurchaseRecordRepository extends JpaRepository<PurchaseRecord, Integer> {
}