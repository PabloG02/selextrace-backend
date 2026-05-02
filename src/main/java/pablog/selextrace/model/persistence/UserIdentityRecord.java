package pablog.selextrace.model.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import pablog.selextrace.model.auth.IdentityProvider;

import java.time.Instant;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "provider", discriminatorType = DiscriminatorType.STRING, length = 32)
@Table(
        name = "user_identities",
        indexes = {@Index(name = "idx_identity_user_id", columnList = "user_id")}
)
public abstract class UserIdentityRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_identity_user")
    )
    private AppUserRecord user;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public abstract IdentityProvider getProvider();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AppUserRecord getUser() {
        return user;
    }

    public void setUser(AppUserRecord user) {
        this.user = user;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}