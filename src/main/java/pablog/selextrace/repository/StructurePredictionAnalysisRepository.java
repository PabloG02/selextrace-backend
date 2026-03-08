package pablog.selextrace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pablog.selextrace.model.StructurePredictionAnalysis;

@Repository
public interface StructurePredictionAnalysisRepository extends JpaRepository<StructurePredictionAnalysis, String> {
}