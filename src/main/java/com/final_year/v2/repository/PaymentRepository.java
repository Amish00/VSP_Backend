package com.final_year.v2.repository;

import com.final_year.v2.dto.PaymentProjection;
import com.final_year.v2.model.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByTransactionId(String transactionId);
    Optional<Payment> findByPidx(String pidx);

    @Query("SELECT p.id as id, u.username as username, p.gateway as paymentMethod, " +
            "p.transactionId as transactionId, p.amount as amount, p.planId as planId, " +
            "p.billingCycle as billingCycle, p.status as status, p.createdAt as transactionDate " +
            "FROM Payment p JOIN User u ON p.userId = u.id ORDER BY p.createdAt DESC")
    Page<PaymentProjection> findAllWithUsername(Pageable pageable);
}