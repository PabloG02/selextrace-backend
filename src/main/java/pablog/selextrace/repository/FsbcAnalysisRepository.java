package pablog.selextrace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pablog.selextrace.model.FsbcAnalysis;

import java.util.List;
import java.util.Optional;

@Repository
public interface FsbcAnalysisRepository extends JpaRepository<FsbcAnalysis, String> {

    List<FsbcAnalysis> findByExperimentId(String experimentId);

    Optional<FsbcAnalysis> findByIdAndExperimentId(String id, String experimentId);

    long deleteByIdAndExperimentId(String id, String experimentId);

    long deleteByExperimentId(String experimentId);
}
