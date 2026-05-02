package com.residentops.repository;
import com.residentops.model.Poll;
import com.residentops.model.User;
import com.residentops.model.Vote;
import com.residentops.model.enums.VoteChoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface VoteRepository extends JpaRepository<Vote, Long> {
    List<Vote> findByPoll(Poll poll);
    Optional<Vote> findByPollAndVotedBy(Poll poll, User user);
    long countByPoll(Poll poll);
    long countByPollAndChoice(Poll poll, VoteChoice choice);
    boolean existsByPollAndVotedBy(Poll poll, User user);
}
