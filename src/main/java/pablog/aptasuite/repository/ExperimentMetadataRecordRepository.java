package pablog.aptasuite.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pablog.aptasuite.model.persistence.ExperimentMetadataRecord;

@Repository
public interface ExperimentMetadataRecordRepository extends JpaRepository<ExperimentMetadataRecord, String> {
}
