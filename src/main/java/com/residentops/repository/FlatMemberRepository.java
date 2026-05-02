package com.residentops.repository;
import com.residentops.model.FlatMember;
import com.residentops.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FlatMemberRepository extends JpaRepository<FlatMember, Long> {
    List<FlatMember> findByResident(User resident);
    long countByResident(User resident);
}
