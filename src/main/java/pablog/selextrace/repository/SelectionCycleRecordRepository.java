package pablog.selextrace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pablog.selextrace.model.persistence.SelectionCycleRecordId;
import pablog.selextrace.model.persistence.SelectionCycleRecord;

import java.util.List;

@Repository
public interface SelectionCycleRecordRepository extends JpaRepository<SelectionCycleRecord, SelectionCycleRecordId> {

    List<SelectionCycleRecord> findByIdExperimentId(Long experimentId);

    void deleteByIdExperimentId(Long experimentId);
}
