package com.example.publicdatabackend.service;

import com.example.publicdatabackend.domain.restaurant.Restaurant;
import com.example.publicdatabackend.domain.statistics.CostsStatistics;
import com.example.publicdatabackend.domain.statistics.SeasonsStatistics;
import com.example.publicdatabackend.domain.users.Users;
import com.example.publicdatabackend.dto.RestaurantDto;
import com.example.publicdatabackend.exception.SeasonException;
import com.example.publicdatabackend.exception.UsersException;
import com.example.publicdatabackend.repository.*;
import com.example.publicdatabackend.utils.ErrorResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RestaurantService {
    private final UsersRepository usersRepository;
    private final RestaurantRepository restaurantRepository;
    private final KakaoReviewsRepository kakaoReviewsRepository;
    private final ReviewsRepository reviewsRepository;
    private final WishListRestaurantRepository wishListRestaurantRepository;
    private final CostsStatisticsRepository costsStatisticsRepository;
    private final SeasonsRepository seasonsRepository;

    /**
     * @param userId
     * @param pageable
     * @return Page<RestaurantDto>
     * @Description 매출수 Service Method
     */
    public Page<RestaurantDto> getRestaurantExecAmountsDescDTO(Long userId, Pageable pageable) {
        validateUser(userId);

        Page<Restaurant> restaurantPage = restaurantRepository.findAllByExecAmountsDesc(pageable);

        return buildRestaurantDto(restaurantPage, userId);
    }

    /**
     * @param userId
     * @param pageable
     * @return Page<RestaurantDto>
     * @Description 방문횟수 Service Method
     */
    public Page<RestaurantDto> getRestaurantNumberOfVisitDescDTO(Long userId, Pageable pageable) {
        validateUser(userId);

        Page<Restaurant> restaurantPage = restaurantRepository.findAllByNumberOfVisitDesc(pageable);

        return buildRestaurantDto(restaurantPage, userId);
    }

    /**
     * @param userId
     * @param price
     * @param pageable
     * @return Page<RestaurantDto>
     * @Description 가격별 Service Method
     */
    public Page<RestaurantDto> getRestaurantPriceDTO(Long userId, Long price, Pageable pageable) {
        validateUser(userId);

        Page<CostsStatistics> costsStatisticsPage = findCostsStatisticsByPrice(price, pageable);
        Page<Restaurant> restaurantPage = getRestaurantFromCostsStatistics(costsStatisticsPage, pageable);

        return buildRestaurantDto(restaurantPage, userId);
    }

    /**
     * @param userId
     * @param season
     * @param pageable
     * @return Page<RestaurantDto>
     * @Description 계절별 Service Method
     */
    public Page<RestaurantDto> getRestaurantSeasonDTO(Long userId, String season, Pageable pageable) {
        validateUser(userId);
        validateSeason(season);

        Page<SeasonsStatistics> seasonsStatistics = findSeasonsStatisticsBySeason(season, pageable);
        Page<Restaurant> restaurantPage = getRestaurantFromSeasonsStatistics(seasonsStatistics, pageable);

        return buildRestaurantDto(restaurantPage, userId);
    }

    // --> 예외 처리 구간
    private Users validateUser(Long userId) {
        return usersRepository.findById(userId)
                .orElseThrow(() -> new UsersException(ErrorResult.USER_ID_NOT_FOUND));
    }

    private void validateSeason(String season) {
        if (!season.equals("spring") && !season.equals("summer") && !season.equals("fall") && !season.equals("winter"))
            throw new SeasonException(ErrorResult.NOT_ALLOWED_SEASON_TYPE);
    }
    // <-- 예외 처리 구간

    private Page<RestaurantDto> buildRestaurantDto(Page<Restaurant> restaurantPage, Long userId) {
        return restaurantPage.map(restaurant -> {
            Long kakaoReviewsNum = kakaoReviewsRepository.findKakaoReviewsNumByRestaurant(restaurant);
            Long reviewsNum = reviewsRepository.findReviewsNumByRestaurant(restaurant);

            Boolean wishListRestaurant = wishListRestaurantRepository
                    .findWishListRestaurantByUserIdAndRestaurantId(userId, restaurant.getId())
                    .isPresent();

            return RestaurantDto.builder()
                    .restaurantId(restaurant.getId())
                    .placeName(restaurant.getPlaceName())
                    .reviewsNum(kakaoReviewsNum + reviewsNum)
                    .rating(restaurant.getRating())
                    .wishListRestaurant(wishListRestaurant)
                    .currentOpeningHours(restaurant.getCurrentOpeningHours())
                    .photoUrl(restaurant.getPhotoUrl())
                    .build();
        });
    }

    // --> Statistics 반환 구간
    private Page<CostsStatistics> findCostsStatisticsByPrice(Long price, Pageable pageable) {
        if (price <= 10000) {
            return costsStatisticsRepository.findByLower10000(pageable);
        } else if (price <= 15000) {
            return costsStatisticsRepository.findByLower15000(pageable);
        } else if (price <= 20000) {
            return costsStatisticsRepository.findByLower20000(pageable);
        } else {
            return costsStatisticsRepository.findByUpper20000(pageable);
        }
    }

    private Page<SeasonsStatistics> findSeasonsStatisticsBySeason(String season, Pageable pageable) {
        if (season.equals("spring")) {
            return seasonsRepository.findBySpringDesc(pageable);
        } else if (season.equals("summer")) {
            return seasonsRepository.findBySummerDesc(pageable);
        } else if (season.equals("fall")) {
            return seasonsRepository.findByFallDesc(pageable);
        } else if (season.equals("winter")) {
            return seasonsRepository.findByWinterDesc(pageable);
        } else {
            throw new SeasonException(ErrorResult.UNKNOWN_EXCEPTION);
        }
    }
    // <-- Statistics 반환 구간


    // --> Statistics -> Restaurant 변환 구간
    private Page<Restaurant> getRestaurantFromCostsStatistics(Page<CostsStatistics> costsStatisticsPage, Pageable pageable) {
        List<Long> restaurantIds = costsStatisticsPage.getContent().stream()
                .map(CostsStatistics::getRestaurantId)
                .collect(Collectors.toList());

        List<Restaurant> restaurants = restaurantRepository.findAllById(restaurantIds);
        return new PageImpl<>(restaurants, pageable, restaurants.size());
    }

    private Page<Restaurant> getRestaurantFromSeasonsStatistics(Page<SeasonsStatistics> seasonsStatistics, Pageable pageable) {
        List<Long> restaurantIds = seasonsStatistics.getContent().stream()
                .map(SeasonsStatistics::getRestaurantId)
                .collect(Collectors.toList());

        List<Restaurant> restaurants = restaurantRepository.findAllById(restaurantIds);
        return new PageImpl<>(restaurants, pageable, restaurants.size());
    }
    // <-- Statistics -> Restaurant 변환 구간
}