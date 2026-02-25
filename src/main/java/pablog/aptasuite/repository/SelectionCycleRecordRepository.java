package pablog.aptasuite.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pablog.aptasuite.model.persistence.SelectionCycleRecordId;
import pablog.aptasuite.model.persistence.SelectionCycleRecord;

import java.util.List;

@Repository
public interface SelectionCycleRecordRepository extends JpaRepository<SelectionCycleRecord, SelectionCycleRecordId> {

    List<SelectionCycleRecord> findByIdExperimentId(String experimentId);

    void deleteByIdExperimentId(String experimentId);
}
