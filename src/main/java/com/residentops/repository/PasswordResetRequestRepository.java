package com.residentops.repository;
import com.residentops.model.PasswordResetRequest;
import com.residentops.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PasswordResetRequestRepository extends JpaRepository<PasswordResetRequest, Long> {
    List<PasswordResetRequest> findByUserAndStatus(User user, String status);
    List<PasswordResetRequest> findByStatusOrderByCreatedAtDesc(String status);
    List<PasswordResetRequest> findAllByOrderByCreatedAtDesc();
}
