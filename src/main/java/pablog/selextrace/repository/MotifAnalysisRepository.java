package pablog.selextrace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pablog.selextrace.model.MotifAnalysis;

import java.util.List;
import java.util.Optional;

@Repository
public interface MotifAnalysisRepository extends JpaRepository<MotifAnalysis, String> {

    List<MotifAnalysis> findByExperimentId(String experimentId);

    Optional<MotifAnalysis> findByIdAndExperimentId(String id, String experimentId);
}