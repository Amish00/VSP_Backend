package com.final_year.v2.repository;

import com.final_year.v2.model.Subscription;
import com.final_year.v2.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findBySubscriberAndSubscribedTo(User subscriber, User subscribedTo);

    long countBySubscribedTo(User subscribedTo);

    Page<Subscription> findBySubscriberOrderBySubscribedAtDesc(User subscriber, Pageable pageable);

    @Modifying
    @Query("DELETE FROM Subscription s WHERE s.subscriber = :subscriber AND s.subscribedTo = :subscribedTo")
    void deleteBySubscriberAndSubscribedTo(@Param("subscriber") User subscriber, @Param("subscribedTo") User subscribedTo);
}