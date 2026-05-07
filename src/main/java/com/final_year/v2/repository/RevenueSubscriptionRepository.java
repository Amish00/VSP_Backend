package com.final_year.v2.repository;

import com.final_year.v2.model.RevenueSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface RevenueSubscriptionRepository extends JpaRepository<RevenueSubscription, Long> {
    @Query("SELECT rs FROM RevenueSubscription rs WHERE rs.startDate <= :endDate AND rs.endDate >= :startDate")
    List<RevenueSubscription> findActiveBetween(@Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate);
}