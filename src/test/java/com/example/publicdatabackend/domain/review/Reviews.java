package com.example.publicdatabackend.domain.review;

import com.example.publicdatabackend.domain.Restaurant;
import com.example.publicdatabackend.domain.users.Users;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class Reviews {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String authorName; // 작성자 이름
    private Double rating; // 별점
    private String relativeTimeDescription; // 작성 시간
    private String photoUrl;

    @Column(columnDefinition = "TEXT")
    private String text; // 리뷰 내용

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id")
    private Restaurant restaurant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private Users user;
}