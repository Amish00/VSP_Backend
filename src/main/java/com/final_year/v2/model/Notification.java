package com.final_year.v2.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)  // the creator who receives notification
    private User user;

    @Column(nullable = false)
    private String title;      // e.g., "New Subscriber!"

    @Column(nullable = false, length = 500)
    private String message;    // e.g., "John Doe subscribed to your channel."

    @Column(nullable = false)
    private String type;       // "SUBSCRIPTION", "COMMENT", "LIKE", "VIDEO_APPROVED", etc.

    private String relatedId;  // optional: subscription ID, video ID, etc.

    @Column(nullable = false)
    private boolean isRead = false;

    @CreationTimestamp
    private LocalDateTime createdAt;
}