package com.Ecommerce.product_service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "stock_releases",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "operation_key")
        }
)
public class StockRelease {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String operationKey;

    public StockRelease() {
    }

    public StockRelease(String operationKey) {
        this.operationKey = operationKey;
    }
}
