package com.residentops.repository;
import com.residentops.model.FlatUpdateRequest;
import com.residentops.model.User;
import com.residentops.model.enums.FlatUpdateStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FlatUpdateRequestRepository extends JpaRepository<FlatUpdateRequest, Long> {
    List<FlatUpdateRequest> findByResident(User resident);
    List<FlatUpdateRequest> findByStatus(FlatUpdateStatus status);
    List<FlatUpdateRequest> findAllByOrderByCreatedAtDesc();
}
