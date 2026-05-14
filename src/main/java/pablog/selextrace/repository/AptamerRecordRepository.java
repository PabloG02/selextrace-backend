package pablog.selextrace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pablog.selextrace.model.persistence.AptamerRecord;
import pablog.selextrace.model.persistence.AptamerRecordId;

import java.util.List;

@Repository
public interface AptamerRecordRepository extends JpaRepository<AptamerRecord, AptamerRecordId> {

    List<AptamerRecord> findByIdExperimentId(Long experimentId);

    void deleteByIdExperimentId(Long experimentId);
}
