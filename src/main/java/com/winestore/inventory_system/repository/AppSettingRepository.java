package com.winestore.inventory_system.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.winestore.inventory_system.model.AppSetting;

@Repository
public interface AppSettingRepository extends JpaRepository<AppSetting, String> {
}
