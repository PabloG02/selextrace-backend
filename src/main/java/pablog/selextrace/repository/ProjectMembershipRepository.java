package pablog.selextrace.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pablog.selextrace.model.persistence.ProjectMembershipRecord;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectMembershipRepository extends JpaRepository<ProjectMembershipRecord, Long> {

    @EntityGraph(attributePaths = "user")
    List<ProjectMembershipRecord> findAllByProject_Id(Long projectId);

    @EntityGraph(attributePaths = "project")
    List<ProjectMembershipRecord> findAllByUser_Id(String userId);

    Optional<ProjectMembershipRecord> findByProject_IdAndUser_Id(Long projectId, String userId);

    void deleteByProject_IdAndUser_Id(Long projectId, String userId);

    void deleteByProject_Id(Long projectId);
}
