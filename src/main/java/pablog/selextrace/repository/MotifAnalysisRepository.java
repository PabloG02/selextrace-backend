package pablog.selextrace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pablog.selextrace.model.MotifAnalysis;

import java.util.List;
import java.util.Optional;

@Repository
public interface MotifAnalysisRepository extends JpaRepository<MotifAnalysis, Long> {

    List<MotifAnalysis> findByExperimentId(Long experimentId);

    Optional<MotifAnalysis> findByIdAndExperimentId(Long id, Long experimentId);

    long deleteByIdAndExperimentId(Long id, Long experimentId);

    long deleteByExperimentId(Long experimentId);
}