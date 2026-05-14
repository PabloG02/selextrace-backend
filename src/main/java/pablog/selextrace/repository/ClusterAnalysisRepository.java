package pablog.selextrace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pablog.selextrace.model.ClusterAnalysis;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClusterAnalysisRepository extends JpaRepository<ClusterAnalysis, Long> {

    List<ClusterAnalysis> findByExperimentId(Long experimentId);

    Optional<ClusterAnalysis> findByIdAndExperimentId(Long id, Long experimentId);

    long deleteByIdAndExperimentId(Long id, Long experimentId);

    long deleteByExperimentId(Long experimentId);
}
