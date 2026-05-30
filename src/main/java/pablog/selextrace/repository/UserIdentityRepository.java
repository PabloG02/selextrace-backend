package pablog.selextrace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pablog.selextrace.model.persistence.UserIdentityRecord;

@Repository
public interface UserIdentityRepository extends JpaRepository<UserIdentityRecord, Long> {
}
