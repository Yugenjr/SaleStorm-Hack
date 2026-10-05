package com.salestorm.repository;

import com.salestorm.domain.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryRepository extends JpaRepository<Inventory, String> {
    
    // The critical atomic conditional update ensuring no overselling
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Inventory i SET i.availableQuantity = i.availableQuantity - :quantity, " +
           "i.reservedQuantity = i.reservedQuantity + :quantity, " +
           "i.version = i.version + 1 " +
           "WHERE i.productId = :productId AND i.availableQuantity >= :quantity")
    int decrementInventoryConditionally(@Param("productId") String productId, @Param("quantity") int quantity);
    
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Inventory i SET i.availableQuantity = i.availableQuantity + :quantity, " +
           "i.reservedQuantity = i.reservedQuantity - :quantity, " +
           "i.version = i.version + 1 " +
           "WHERE i.productId = :productId")
    int restoreInventory(@Param("productId") String productId, @Param("quantity") int quantity);
}
