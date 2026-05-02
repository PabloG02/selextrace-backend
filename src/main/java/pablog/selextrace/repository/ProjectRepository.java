package pablog.selextrace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pablog.selextrace.model.persistence.ProjectRecord;

@Repository
public interface ProjectRepository extends JpaRepository<ProjectRecord, String> {
}
