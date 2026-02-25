package pablog.aptasuite.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pablog.aptasuite.model.ClusterAnalysis;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClusterAnalysisRepository extends JpaRepository<ClusterAnalysis, String> {

    List<ClusterAnalysis> findByExperimentId(String experimentId);

    Optional<ClusterAnalysis> findByIdAndExperimentId(String id, String experimentId);
}
