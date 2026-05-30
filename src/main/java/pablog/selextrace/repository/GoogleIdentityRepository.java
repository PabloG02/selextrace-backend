package pablog.selextrace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pablog.selextrace.model.persistence.GoogleIdentityRecord;

import java.util.Optional;

public interface GoogleIdentityRepository extends JpaRepository<GoogleIdentityRecord, Long> {

    Optional<GoogleIdentityRecord> findByProviderSubject(String providerSubject);

    boolean existsByUser_Id(String userId);
}
