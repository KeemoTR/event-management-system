package eventsystem.util;

import eventsystem.model.Rating;
import eventsystem.service.RatingService;

public class RatingServiceTest {

    public static void main(String[] args) {

        RatingService ratingService = new RatingService();

        int studentId = 3;
        int eventId = 1;

        try {
            System.out.println("=== Rating Service Test Started ===");

            // 1) Add rating
            Rating rating = ratingService.addRating(
                    studentId,
                    eventId,
                    5,
                    "Great event!"
            );

            System.out.println("Rating added successfully");
            System.out.println("Rating ID: " + rating.getId());
            System.out.println("Student ID: " + rating.getStudentId());
            System.out.println("Event ID: " + rating.getEventId());
            System.out.println("Rating: " + rating.getRating());
            System.out.println("Comment: " + rating.getComment());

            // 2) Get rating by student and event
            Rating foundRating = ratingService.getRatingByStudentAndEvent(studentId, eventId);

            if (foundRating != null) {
                System.out.println("Rating found successfully");
                System.out.println("Found Rating: " + foundRating.getRating());
            } else {
                System.out.println("Rating not found");
            }

            // 3) Average rating
            double average = ratingService.getAverageRatingForEvent(eventId);
            System.out.println("Average rating for event " + eventId + ": " + average);

            // 4) Update rating
            Rating updatedRating = ratingService.updateRating(
                    studentId,
                    rating.getId(),
                    4,
                    "Good event, but can be better."
            );

            System.out.println("Rating updated successfully");
            System.out.println("Updated Rating: " + updatedRating.getRating());
            System.out.println("Updated Comment: " + updatedRating.getComment());

            // 5) Delete rating
            ratingService.deleteRating(studentId, rating.getId());
            System.out.println("Rating deleted successfully");

            System.out.println("=== Rating Service Test Finished ===");

        } catch (Exception e) {
            System.out.println("Test failed");
            e.printStackTrace();
        }
    }
}