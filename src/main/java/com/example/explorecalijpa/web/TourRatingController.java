package com.example.explorecalijpa.web;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.example.explorecalijpa.business.TourRatingService;
import com.example.explorecalijpa.config.FeatureFlagService;
import com.example.explorecalijpa.model.TourRating;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * Tour Rating Controller
 *
 * Handles CRUD operations for tour ratings with Feature Flag support.
 * When features.tour-ratings=false, all endpoints return 404 Not Found.
 */
@RestController
@Slf4j
@Tag(name = "Tour Rating", description = "The Rating for a Tour API")
@RequestMapping(path = "/tours/{tourId}/ratings")
public class TourRatingController {

  private final TourRatingService tourRatingService;
  private final FeatureFlagService featureFlagService;

  public TourRatingController(TourRatingService tourRatingService,
      FeatureFlagService featureFlagService) {
    this.tourRatingService = tourRatingService;
    this.featureFlagService = featureFlagService;
  }

  /** Guard method to check if tour ratings are enabled */
  private void checkRatingsEnabled() {
    if (!featureFlagService.isEnabled("tour-ratings")) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tour ratings feature disabled");
    }
  }

  /**
   * Create a Tour Rating.
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create a Tour Rating")
  public RatingDto createTourRating(@PathVariable(value = "tourId") int tourId,
      @RequestBody @Valid RatingDto ratingDto) {
    checkRatingsEnabled();
    log.info("POST /tours/{}/ratings", tourId);
    TourRating rating = tourRatingService.createNew(
        tourId, ratingDto.getCustomerId(), ratingDto.getScore(), ratingDto.getComment());
    return new RatingDto(rating);
  }

  /**
   * Lookup all Ratings for a Tour.
   */
  @GetMapping
  @Operation(summary = "Lookup All Ratings for a Tour")
  public List<RatingDto> getAllRatingsForTour(@PathVariable(value = "tourId") int tourId) {
    checkRatingsEnabled();
    log.info("GET /tours/{}/ratings", tourId);
    List<TourRating> tourRatings = tourRatingService.lookupRatings(tourId);
    return tourRatings.stream().map(RatingDto::new).toList();
  }

  /**
   * Calculate the average Score of a Tour.
   */
  @GetMapping("/average")
  @Operation(summary = "Get the Average Score for a Tour")
  public Map<String, Double> getAverage(@PathVariable(value = "tourId") int tourId) {
    checkRatingsEnabled();
    log.info("GET /tours/{}/ratings/average", tourId);
    return Map.of("average", tourRatingService.getAverageScore(tourId));
  }

  /**
   * Update score and comment of a Tour Rating.
   */
  @PutMapping
  @Operation(summary = "Modify All Tour Rating Attributes")
  public RatingDto updateWithPut(@PathVariable(value = "tourId") int tourId,
      @RequestBody @Valid RatingDto ratingDto) {
    checkRatingsEnabled();
    log.info("PUT /tours/{}/ratings", tourId);
    return new RatingDto(tourRatingService.update(
        tourId, ratingDto.getCustomerId(), ratingDto.getScore(), ratingDto.getComment()));
  }

  /**
   * Update score or comment of a Tour Rating (PATCH).
   */
  @PatchMapping
  @Operation(summary = "Modify Some Tour Rating Attributes")
  public RatingDto updateWithPatch(@PathVariable(value = "tourId") int tourId,
      @RequestBody @Valid RatingDto ratingDto) {
    checkRatingsEnabled();
    log.info("PATCH /tours/{}/ratings", tourId);
    return new RatingDto(tourRatingService.updateSome(
        tourId,
        ratingDto.getCustomerId(),
        Optional.ofNullable(ratingDto.getScore()),
        Optional.ofNullable(ratingDto.getComment())));
  }

  /**
   * Delete a Rating of a Tour made by a Customer.
   */
  @DeleteMapping("/{customerId}")
  @Operation(summary = "Delete a Customer's Rating of a Tour")
  public void delete(@PathVariable(value = "tourId") int tourId,
      @PathVariable(value = "customerId") int customerId) {
    checkRatingsEnabled();
    log.info("DELETE /tours/{}/ratings/{}", tourId, customerId);
    tourRatingService.delete(tourId, customerId);
  }

  /**
   * Create Several Tour Ratings for one tour, score, and several customers.
   */
  @PostMapping("/batch")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Give Many Tours Same Score")
  public void createManyTourRatings(@PathVariable(value = "tourId") int tourId,
      @RequestParam(value = "score") int score,
      @RequestBody List<Integer> customers) {
    checkRatingsEnabled();
    log.info("POST /tours/{}/ratings/batch", tourId);
    tourRatingService.rateMany(tourId, score, customers);
  }
}
