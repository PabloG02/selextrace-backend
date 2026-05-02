package pablog.selextrace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pablog.selextrace.model.persistence.AppUserRecord;

import java.util.Optional;

@Repository
public interface AppUserRepository extends JpaRepository<AppUserRecord, String> {

    boolean existsByEmail(String email);

    Optional<AppUserRecord> findByEmail(String email);
}
