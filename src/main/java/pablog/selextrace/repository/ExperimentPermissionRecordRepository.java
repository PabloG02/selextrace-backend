package pablog.selextrace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pablog.selextrace.model.persistence.ExperimentPermissionRecord;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExperimentPermissionRecordRepository extends JpaRepository<ExperimentPermissionRecord, Long> {

    List<ExperimentPermissionRecord> findAllByExperiment_Id(Long experimentId);

    List<ExperimentPermissionRecord> findAllByUser_Id(String userId);

    Optional<ExperimentPermissionRecord> findByExperiment_IdAndUser_Id(Long experimentId, String userId);

    void deleteByExperiment_IdAndUser_Id(Long experimentId, String userId);

    void deleteAllByExperiment_Id(Long experimentId);
}
