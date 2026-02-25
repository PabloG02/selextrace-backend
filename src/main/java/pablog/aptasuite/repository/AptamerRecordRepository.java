package pablog.aptasuite.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pablog.aptasuite.model.persistence.AptamerRecord;
import pablog.aptasuite.model.persistence.AptamerRecordId;

import java.util.List;

@Repository
public interface AptamerRecordRepository extends JpaRepository<AptamerRecord, AptamerRecordId> {

    List<AptamerRecord> findByIdExperimentId(String experimentId);

    void deleteByIdExperimentId(String experimentId);
}
