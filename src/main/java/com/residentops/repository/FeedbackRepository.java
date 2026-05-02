package com.residentops.repository;
import com.residentops.model.Feedback;
import com.residentops.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    List<Feedback> findByGivenBy(User user);
    @Query("SELECT AVG(f.rating) FROM Feedback f JOIN f.request r WHERE r.assignedTo = :vendor")
    Double getAverageRatingForVendor(User vendor);
}
