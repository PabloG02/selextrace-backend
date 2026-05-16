package pablog.selextrace.repository;

import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pablog.selextrace.model.persistence.AptamerRecord;
import pablog.selextrace.model.persistence.AptamerRecordId;

import java.util.stream.Stream;

@Repository
public interface AptamerRecordRepository extends JpaRepository<AptamerRecord, AptamerRecordId> {

    @QueryHints(@QueryHint(name = org.hibernate.jpa.HibernateHints.HINT_FETCH_SIZE, value = "10000"))
    @Query("SELECT a FROM AptamerRecord a WHERE a.experiment.id = :experimentId ORDER BY a.id.aptamerId")
    Stream<AptamerRecord> streamByExperimentId(@Param("experimentId") Long experimentId);
}
