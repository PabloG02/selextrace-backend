package pablog.selextrace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pablog.selextrace.model.FsbcAnalysis;

import java.util.List;
import java.util.Optional;

@Repository
public interface FsbcAnalysisRepository extends JpaRepository<FsbcAnalysis, Long> {

    List<FsbcAnalysis> findByExperimentId(Long experimentId);

    Optional<FsbcAnalysis> findByIdAndExperimentId(Long id, Long experimentId);

    long deleteByIdAndExperimentId(Long id, Long experimentId);

    long deleteByExperimentId(Long experimentId);
}
