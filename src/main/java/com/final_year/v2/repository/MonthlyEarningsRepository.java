package com.final_year.v2.repository;

import com.final_year.v2.model.MonthlyEarnings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface MonthlyEarningsRepository extends JpaRepository<MonthlyEarnings, Long> {
    Optional<MonthlyEarnings> findByCreatorIdAndMonthYear(Long creatorId, String monthYear);
    boolean existsByMonthYear(String monthYear);

    @Query("SELECT SUM(m.earningsAmount) FROM MonthlyEarnings m WHERE m.creatorId = :creatorId")
    Optional<BigDecimal> sumEarningsByCreatorId(@Param("creatorId") Long creatorId);

    List<MonthlyEarnings> findAllByCreatorIdOrderByMonthYearDesc(Long creatorId);

    
}