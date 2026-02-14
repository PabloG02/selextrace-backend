package pablog.aptasuite.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import pablog.aptasuite.model.ClusterAnalysis;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClusterAnalysisRepository extends MongoRepository<ClusterAnalysis, String> {

    List<ClusterAnalysis> findByExperimentId(String experimentId);

    Optional<ClusterAnalysis> findByIdAndExperimentId(String id, String experimentId);
}
