package pablog.aptasuite.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pablog.aptasuite.model.persistence.ExperimentRecord;

import java.util.Optional;

@Repository
public interface ExperimentRecordRepository extends JpaRepository<ExperimentRecord, String> {

	@EntityGraph(attributePaths = {
			"metadataRecord",
			"selectionCycleRecords",
			"aptamerRecords"
	})
	Optional<ExperimentRecord> findById(String id);
}
