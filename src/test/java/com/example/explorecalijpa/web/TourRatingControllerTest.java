package com.example.explorecalijpa.web;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.example.explorecalijpa.business.TourRatingService;
import com.example.explorecalijpa.model.Tour;
import com.example.explorecalijpa.model.TourRating;

import jakarta.validation.ConstraintViolationException;

/**
 * Integration tests for TourRatingController.
 * Includes authentication (Basic Auth) and negative tests for security lab.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
public class TourRatingControllerTest {

  private static final int TOUR_ID = 999;
  private static final int CUSTOMER_ID = 1000;
  private static final int SCORE = 3;
  private static final String COMMENT = "comment";
  private static final String TOUR_RATINGS_URL = "/tours/" + TOUR_ID + "/ratings";

  @Autowired
  private TestRestTemplate template;

  private TestRestTemplate userRestTemplate;
  private TestRestTemplate adminRestTemplate;

  @MockBean
  private TourRatingService serviceMock;

  @Mock
  private TourRating tourRatingMock;

  @Mock
  private Tour tourMock;

  private RatingDto ratingDto = new RatingDto(SCORE, COMMENT, CUSTOMER_ID);

  @BeforeEach
  void setUp() {
    // Derive authenticated rest templates for both roles
    this.userRestTemplate = template.withBasicAuth("user", "password");
    this.adminRestTemplate = template.withBasicAuth("admin", "admin123");
  }

  /** Positive path tests with admin privileges **/
  @Test
  void testCreateTourRating() {
    adminRestTemplate.postForEntity(TOUR_RATINGS_URL, ratingDto, RatingDto.class);
    verify(this.serviceMock).createNew(TOUR_ID, CUSTOMER_ID, SCORE, COMMENT);
  }

  @Test
  void testDelete() {
    adminRestTemplate.delete(TOUR_RATINGS_URL + "/" + CUSTOMER_ID);
    verify(this.serviceMock).delete(TOUR_ID, CUSTOMER_ID);
  }

  @Test
  void testUpdateWithPut() {
    adminRestTemplate.put(TOUR_RATINGS_URL, ratingDto);
    verify(this.serviceMock).update(TOUR_ID, CUSTOMER_ID, SCORE, COMMENT);
  }

  @Test
  void testUpdateWithPatch() {
    when(serviceMock.updateSome(anyInt(), anyInt(), any(), any())).thenReturn(tourRatingMock);
    adminRestTemplate.patchForObject(TOUR_RATINGS_URL, ratingDto, String.class);
    verify(this.serviceMock).updateSome(anyInt(), anyInt(), any(), any());
  }

  @Test
  void testCreateManyTourRatings() {
    Integer[] customers = { 123 };
    adminRestTemplate.postForObject(
        TOUR_RATINGS_URL + "/batch?score=" + SCORE, customers, String.class);
    verify(serviceMock).rateMany(anyInt(), anyInt(), anyList());
  }

  /** Read operations (any authenticated user) **/
  @Test
  void testGetAllRatingsForTour() {
    when(serviceMock.lookupRatings(anyInt())).thenReturn(List.of(tourRatingMock));
    ResponseEntity<String> res = userRestTemplate.getForEntity(TOUR_RATINGS_URL, String.class);

    assertThat(res.getStatusCode(), is(HttpStatus.OK));
    verify(serviceMock).lookupRatings(anyInt());
  }

  @Test
  void testGetAverage() {
    when(serviceMock.lookupRatings(anyInt())).thenReturn(List.of(tourRatingMock));
    ResponseEntity<String> res = userRestTemplate.getForEntity(
        TOUR_RATINGS_URL + "/average", String.class);

    assertThat(res.getStatusCode(), is(HttpStatus.OK));
    verify(serviceMock).getAverageScore(TOUR_ID);
  }

  /** Negative path tests **/

  @Test
  void testUserCannotCreateTourRating() {
    // USER role should not have permission to POST ratings (expect 403)
    ResponseEntity<String> res = userRestTemplate.postForEntity(TOUR_RATINGS_URL, ratingDto, String.class);
    assertThat(res.getStatusCode(), is(HttpStatus.FORBIDDEN));
  }

  @Test
  public void test404() {
    when(serviceMock.lookupRatings(anyInt())).thenThrow(new NoSuchElementException());
    ResponseEntity<String> res = userRestTemplate.getForEntity(TOUR_RATINGS_URL, String.class);
    assertThat(res.getStatusCode(), is(HttpStatus.NOT_FOUND));
  }

  @Test
  public void test400() {
    when(serviceMock.lookupRatings(anyInt())).thenThrow(new ConstraintViolationException(null));
    ResponseEntity<String> res = userRestTemplate.getForEntity(TOUR_RATINGS_URL, String.class);
    assertThat(res.getStatusCode(), is(HttpStatus.BAD_REQUEST));
  }
}
