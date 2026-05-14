package pablog.selextrace.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import pablog.selextrace.model.persistence.ExperimentRecord;

import java.util.Optional;

@Repository
public interface ExperimentRecordRepository extends JpaRepository<ExperimentRecord, Long> {

	@EntityGraph(attributePaths = {
			"metadataRecord",
			"selectionCycleRecords",
			"aptamerRecords"
	})
	Optional<ExperimentRecord> findById(Long id);

	@Modifying
	@Query(value = "DELETE FROM experiment_records WHERE id = :id", nativeQuery = true)
	void bulkDeleteById(Long id);
}
