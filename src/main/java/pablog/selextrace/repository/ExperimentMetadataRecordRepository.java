package pablog.selextrace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pablog.selextrace.model.persistence.ExperimentMetadataRecord;

@Repository
public interface ExperimentMetadataRecordRepository extends JpaRepository<ExperimentMetadataRecord, String> {
}
