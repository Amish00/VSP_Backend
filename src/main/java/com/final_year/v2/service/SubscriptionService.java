package com.final_year.v2.service;

import com.final_year.v2.dto.SubscriberCountResponse;
import com.final_year.v2.dto.SubscriptionResponse;
import com.final_year.v2.model.Subscription;
import com.final_year.v2.model.User;
import com.final_year.v2.repository.SubscriptionRepository;
import com.final_year.v2.repository.UserRepository;
import com.final_year.v2.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService; // if you use it

    @Transactional
    public void subscribe(UserDetailsImpl currentUser, Long creatorId) {
        User subscriber = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new RuntimeException("Creator not found"));

        if (subscriber.getId().equals(creator.getId())) {
            throw new RuntimeException("Cannot subscribe to yourself");
        }

        if (subscriptionRepository.findBySubscriberAndSubscribedTo(subscriber, creator).isPresent()) {
            throw new RuntimeException("Already subscribed");
        }

        Subscription subscription = Subscription.builder()
                .subscriber(subscriber)
                .subscribedTo(creator)
                .build();
        subscriptionRepository.save(subscription);
    }

    @Transactional
    public void unsubscribe(UserDetailsImpl currentUser, Long creatorId) {
        User subscriber = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new RuntimeException("Creator not found"));

        subscriptionRepository.deleteBySubscriberAndSubscribedTo(subscriber, creator);
    }

    // Updated method: now accepts currentUser (can be null for unauthenticated requests)
    public SubscriberCountResponse getSubscriberInfo(Long creatorId, UserDetailsImpl currentUser) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new RuntimeException("Creator not found"));
        long count = subscriptionRepository.countBySubscribedTo(creator);

        boolean isSubscribed = false;
        if (currentUser != null) {
            User subscriber = userRepository.findById(currentUser.getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            isSubscribed = subscriptionRepository.findBySubscriberAndSubscribedTo(subscriber, creator).isPresent();
        }
        return new SubscriberCountResponse(count, isSubscribed);
    }

    public Page<SubscriptionResponse> getSubscribedChannels(UserDetailsImpl currentUser, Pageable pageable) {
        User subscriber = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return subscriptionRepository.findBySubscriberOrderBySubscribedAtDesc(subscriber, pageable)
                .map(this::mapToResponse);
    }

    private SubscriptionResponse mapToResponse(Subscription subscription) {
        User creator = subscription.getSubscribedTo();
        return SubscriptionResponse.builder()
                .id(subscription.getId())
                .subscribedToId(creator.getId())
                .username(creator.getUsername())
                .profilePicture(creator.getProfilePicture())
                .subscribedAt(subscription.getSubscribedAt())
                .build();
    }
}