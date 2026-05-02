package com.residentops.repository;
import com.residentops.model.User;
import com.residentops.model.VendorConcern;
import com.residentops.model.enums.VendorConcernStatus;
import com.residentops.model.enums.VendorConcernType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface VendorConcernRepository extends JpaRepository<VendorConcern, Long> {
    List<VendorConcern> findByVendor(User vendor);
    List<VendorConcern> findByStatus(VendorConcernStatus status);
    List<VendorConcern> findAllByOrderByCreatedAtDesc();
    List<VendorConcern> findByVendorAndConcernTypeAndStatus(User vendor, VendorConcernType type, VendorConcernStatus status);
}
