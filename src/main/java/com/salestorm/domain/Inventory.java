package com.salestorm.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
@Entity
public class Inventory {
    @Id
    private String productId;
    
    private int availableQuantity;
    private int reservedQuantity;
    
    @Version
    private Long version;

    public Inventory() {}

    public Inventory(String productId, int availableQuantity, int reservedQuantity, Long version) {
        this.productId = productId;
        this.availableQuantity = availableQuantity;
        this.reservedQuantity = reservedQuantity;
        this.version = version;
    }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public int getAvailableQuantity() { return availableQuantity; }
    public void setAvailableQuantity(int availableQuantity) { this.availableQuantity = availableQuantity; }
    public int getReservedQuantity() { return reservedQuantity; }
    public void setReservedQuantity(int reservedQuantity) { this.reservedQuantity = reservedQuantity; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
