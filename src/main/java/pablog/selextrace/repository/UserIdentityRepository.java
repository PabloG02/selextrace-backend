package pablog.selextrace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pablog.selextrace.model.auth.IdentityProvider;
import pablog.selextrace.model.persistence.UserIdentityRecord;

import java.util.Optional;

@Repository
public interface UserIdentityRepository extends JpaRepository<UserIdentityRecord, Long> {

    Optional<UserIdentityRecord> findByUser_EmailAndProvider(String email, IdentityProvider provider);
}
