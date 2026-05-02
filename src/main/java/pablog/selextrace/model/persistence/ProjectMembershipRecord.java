package pablog.selextrace.model.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.CreationTimestamp;
import pablog.selextrace.model.auth.ResourceAccessLevel;

import java.time.Instant;

@Entity
@Table(
        name = "project_memberships",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_project_membership_project_user", columnNames = {"project_id", "user_id"})
        }
)
public class ProjectMembershipRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "project_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_project_membership_project")
    )
    private ProjectRecord project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_project_membership_user")
    )
    private AppUserRecord user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ResourceAccessLevel accessLevel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "granted_by_user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_project_membership_granted_by_user")
    )
    private AppUserRecord grantedByUser;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public ProjectRecord getProject() {
        return project;
    }

    public void setProject(ProjectRecord project) {
        this.project = project;
    }

    public AppUserRecord getUser() {
        return user;
    }

    public void setUser(AppUserRecord user) {
        this.user = user;
    }

    public ResourceAccessLevel getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(ResourceAccessLevel accessLevel) {
        this.accessLevel = accessLevel;
    }

    public AppUserRecord getGrantedByUser() {
        return grantedByUser;
    }

    public void setGrantedByUser(AppUserRecord grantedByUser) {
        this.grantedByUser = grantedByUser;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
