package com.residentops.repository;
import com.residentops.model.Complaint;
import com.residentops.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    List<Complaint> findByFiledBy(User user);
    List<Complaint> findAllByOrderByCreatedAtDesc();
}
