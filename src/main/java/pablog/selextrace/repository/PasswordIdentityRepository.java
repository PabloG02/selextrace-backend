package pablog.selextrace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pablog.selextrace.model.persistence.AppUserRecord;
import pablog.selextrace.model.persistence.PasswordIdentityRecord;

import java.util.Optional;

public interface PasswordIdentityRepository extends JpaRepository<PasswordIdentityRecord, Long> {

    Optional<PasswordIdentityRecord> findByUser(AppUserRecord user);

    Optional<PasswordIdentityRecord> findByUser_Email(String email);
}
