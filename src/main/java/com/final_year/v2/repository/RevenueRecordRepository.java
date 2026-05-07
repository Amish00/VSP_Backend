package com.final_year.v2.repository;

import com.final_year.v2.model.RevenueRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface RevenueRecordRepository extends JpaRepository<RevenueRecord, Long> {
    Page<RevenueRecord> findAllByOrderByTransactionDateDesc(Pageable pageable);
    List<RevenueRecord> findAllByTransactionDateBetween(LocalDateTime start, LocalDateTime end);
}