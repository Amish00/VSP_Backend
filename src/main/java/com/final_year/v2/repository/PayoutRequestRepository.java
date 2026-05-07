package com.final_year.v2.repository;

import com.final_year.v2.model.PayoutRequest;
import com.final_year.v2.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PayoutRequestRepository extends JpaRepository<PayoutRequest, Long> {
    List<PayoutRequest> findByCreatorAndStatusOrderByRequestedAtDesc(User creator, String status);
    List<PayoutRequest> findByStatusOrderByRequestedAtDesc(String status);
    @Query("SELECT SUM(p.amount) FROM PayoutRequest p WHERE p.creator.id = :creatorId AND p.status = :status")
    Optional<BigDecimal> sumAmountByCreatorIdAndStatus(@Param("creatorId") Long creatorId, @Param("status") String status);
}