package com.residentops.repository;
import com.residentops.model.Poll;
import com.residentops.model.enums.PollStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PollRepository extends JpaRepository<Poll, Long> {
    List<Poll> findByStatus(PollStatus status);
    List<Poll> findAllByOrderByCreatedAtDesc();
}
