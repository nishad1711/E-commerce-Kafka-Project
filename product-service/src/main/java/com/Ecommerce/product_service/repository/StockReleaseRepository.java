package com.Ecommerce.product_service.repository;

import com.Ecommerce.product_service.entity.StockRelease;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

public interface StockReleaseRepository extends JpaRepository<StockRelease, Long> {

    @Modifying
    @Query(value = """
            INSERT INTO stock_releases (operation_key)
            VALUES (:operationKey)
            ON CONFLICT (operation_key) DO NOTHING
            """, nativeQuery = true)
    int recordIfAbsent(@Param("operationKey") String operationKey);
}