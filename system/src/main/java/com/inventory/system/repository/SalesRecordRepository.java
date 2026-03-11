package com.inventory.system.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.inventory.system.entity.SalesRecord;

@Repository
public interface SalesRecordRepository extends JpaRepository<SalesRecord, Integer> {
}