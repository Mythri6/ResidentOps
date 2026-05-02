package com.residentops.repository;

import com.residentops.model.MaintenanceRequest;
import com.residentops.model.User;
import com.residentops.model.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MaintenanceRequestRepository extends JpaRepository<MaintenanceRequest, Long> {
    List<MaintenanceRequest> findByRaisedBy(User user);
    List<MaintenanceRequest> findByAssignedTo(User vendor);
    List<MaintenanceRequest> findByStatus(RequestStatus status);

    // Find all SLA-breached requests that haven't been escalated yet
    @Query("SELECT r FROM MaintenanceRequest r WHERE r.slaDeadline < :now " +
           "AND r.status NOT IN ('RESOLVED','CLOSED','ESCALATED','REJECTED')")
    List<MaintenanceRequest> findSLABreached(LocalDateTime now);

    List<MaintenanceRequest> findAllByOrderByCreatedAtDesc();
}
