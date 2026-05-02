package com.residentops.service;

import com.residentops.model.Poll;
import com.residentops.model.User;
import com.residentops.model.Vote;
import com.residentops.model.enums.PollStatus;
import com.residentops.model.enums.VoteChoice;
import com.residentops.repository.PollRepository;
import com.residentops.repository.UserRepository;
import com.residentops.repository.VoteRepository;
import com.residentops.singleton.AuditLogger;
import com.residentops.singleton.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class VotingService {

    private final PollRepository pollRepository;
    private final VoteRepository voteRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditLogger auditLogger;

    public VotingService(PollRepository pollRepository,
                         VoteRepository voteRepository,
                         UserRepository userRepository,
                         NotificationService notificationService,
                         AuditLogger auditLogger) {
        this.pollRepository = pollRepository;
        this.voteRepository = voteRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.auditLogger = auditLogger;
    }

    @Transactional
    public Poll createPoll(Long adminId, String question, String description,
                           int quorum, LocalDateTime deadline) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));
        Poll poll = new Poll();
        poll.setQuestion(question);
        poll.setDescription(description);
        poll.setCreatedBy(admin);
        poll.setQuorum(quorum);
        poll.setDeadline(deadline);
        Poll saved = pollRepository.save(poll);

        notificationService.notifyAll("POLL_CREATED", "New poll: " + question, saved.getId());
        auditLogger.log("POLL_CREATED", "Poll", saved.getId(), adminId,
                "Quorum: " + quorum + ", Deadline: " + deadline);
        return saved;
    }

    @Transactional
    public Vote castVote(Long pollId, Long userId, VoteChoice choice) {
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new RuntimeException("Poll not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (poll.getStatus() != PollStatus.OPEN) {
            throw new RuntimeException("Poll is not open for voting.");
        }
        if (poll.isExpired()) {
            closePoll(pollId, 3L); // auto-close
            throw new RuntimeException("Poll deadline has passed.");
        }
        if (voteRepository.existsByPollAndVotedBy(poll, user)) {
            throw new RuntimeException("You have already voted in this poll.");
        }

        Vote vote = new Vote();
        vote.setPoll(poll);
        vote.setVotedBy(user);
        vote.setChoice(choice);
        Vote saved = voteRepository.save(vote);

        auditLogger.log("VOTE_CAST", "Vote", saved.getId(), userId,
                "Poll #" + pollId + " → " + choice);
        return saved;
    }

    @Transactional
    public Poll closePoll(Long pollId, Long adminId) {
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new RuntimeException("Poll not found"));

        long totalVotes = voteRepository.countByPoll(poll);
        long yesVotes = voteRepository.countByPollAndChoice(poll, VoteChoice.YES);
        long noVotes = voteRepository.countByPollAndChoice(poll, VoteChoice.NO);

        // GRASP Information Expert — quorum check belongs where the data is
        if (totalVotes < poll.getQuorum()) {
            poll.setStatus(PollStatus.INCONCLUSIVE);
            poll.setResult("Inconclusive — quorum not met (" + totalVotes + "/" + poll.getQuorum() + ")");
        } else {
            poll.setStatus(PollStatus.CLOSED);
            String outcome = yesVotes > noVotes ? "APPROVED" : "REJECTED";
            poll.setResult(outcome + " | YES: " + yesVotes + ", NO: " + noVotes
                    + ", Total: " + totalVotes);
        }

        Poll saved = pollRepository.save(poll);
        notificationService.notifyAll("POLL_CLOSED", "Poll closed: " + poll.getQuestion(), pollId);
        auditLogger.log("POLL_CLOSED", "Poll", pollId, adminId, saved.getResult());
        return saved;
    }

    public List<Poll> getAll() {
        return pollRepository.findAllByOrderByCreatedAtDesc();
    }

    public Optional<Poll> getById(Long id) {
        return pollRepository.findById(id);
    }

    public Map<String, Long> getPollResults(Long pollId) {
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new RuntimeException("Poll not found"));
        Map<String, Long> results = new LinkedHashMap<>();
        results.put("YES",     voteRepository.countByPollAndChoice(poll, VoteChoice.YES));
        results.put("NO",      voteRepository.countByPollAndChoice(poll, VoteChoice.NO));
        results.put("ABSTAIN", voteRepository.countByPollAndChoice(poll, VoteChoice.ABSTAIN));
        results.put("TOTAL",   voteRepository.countByPoll(poll));
        results.put("QUORUM",  (long) poll.getQuorum());
        return results;
    }
}
