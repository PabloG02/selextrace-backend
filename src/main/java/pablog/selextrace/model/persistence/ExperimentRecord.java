package pablog.selextrace.model.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "experiment_records")
public class ExperimentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false)
    private Long id;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "created_by_user_id",
        nullable = false,
        updatable = false,
        foreignKey = @ForeignKey(name = "fk_experiment_created_by_user")
    )
    private AppUserRecord createdByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "project_id",
        foreignKey = @ForeignKey(name = "fk_experiment_project")
    )
    private ProjectRecord project;

    // --- General information --- //

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Integer aptamerSize;

    @Column(length = 1024)
    private String fivePrimePrimer;

    @Column(length = 1024)
    private String threePrimePrimer;

    // --- Sequence Import Statistics --- //

    @Column(nullable = false)
    private long totalProcessedReads;

    @Column(nullable = false)
    private long totalAcceptedReads;

    @Column(nullable = false)
    private long contigAssemblyFailure;

    @Column(nullable = false)
    private long invalidAlphabet;

    @Column(nullable = false)
    private long fivePrimeError;

    @Column(nullable = false)
    private long threePrimeError;

    @Column(nullable = false)
    private long invalidCycle;

    @Column(nullable = false)
    private long totalPrimerOverlaps;

    @OneToOne(
        mappedBy = "experiment",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private ExperimentMetadataRecord metadataRecord;

    @OneToMany(
        mappedBy = "experiment",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private Set<SelectionCycleRecord> selectionCycleRecords = new LinkedHashSet<>();

    @OneToMany(
        mappedBy = "experiment",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private Set<AptamerRecord> aptamerRecords = new LinkedHashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getAptamerSize() {
        return aptamerSize;
    }

    public void setAptamerSize(Integer aptamerSize) {
        this.aptamerSize = aptamerSize;
    }

    public String getFivePrimePrimer() {
        return fivePrimePrimer;
    }

    public void setFivePrimePrimer(String fivePrimePrimer) {
        this.fivePrimePrimer = fivePrimePrimer;
    }

    public String getThreePrimePrimer() {
        return threePrimePrimer;
    }

    public void setThreePrimePrimer(String threePrimePrimer) {
        this.threePrimePrimer = threePrimePrimer;
    }

    public long getTotalProcessedReads() {
        return totalProcessedReads;
    }

    public void setTotalProcessedReads(long totalProcessedReads) {
        this.totalProcessedReads = totalProcessedReads;
    }

    public long getTotalAcceptedReads() {
        return totalAcceptedReads;
    }

    public void setTotalAcceptedReads(long totalAcceptedReads) {
        this.totalAcceptedReads = totalAcceptedReads;
    }

    public long getContigAssemblyFailure() {
        return contigAssemblyFailure;
    }

    public void setContigAssemblyFailure(long contigAssemblyFailure) {
        this.contigAssemblyFailure = contigAssemblyFailure;
    }

    public long getInvalidAlphabet() {
        return invalidAlphabet;
    }

    public void setInvalidAlphabet(long invalidAlphabet) {
        this.invalidAlphabet = invalidAlphabet;
    }

    public long getFivePrimeError() {
        return fivePrimeError;
    }

    public void setFivePrimeError(long fivePrimeError) {
        this.fivePrimeError = fivePrimeError;
    }

    public long getThreePrimeError() {
        return threePrimeError;
    }

    public void setThreePrimeError(long threePrimeError) {
        this.threePrimeError = threePrimeError;
    }

    public long getInvalidCycle() {
        return invalidCycle;
    }

    public void setInvalidCycle(long invalidCycle) {
        this.invalidCycle = invalidCycle;
    }

    public long getTotalPrimerOverlaps() {
        return totalPrimerOverlaps;
    }

    public void setTotalPrimerOverlaps(long totalPrimerOverlaps) {
        this.totalPrimerOverlaps = totalPrimerOverlaps;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public AppUserRecord getCreatedByUser() {
        return createdByUser;
    }

    public void setCreatedByUser(AppUserRecord createdByUser) {
        this.createdByUser = createdByUser;
    }

    public ProjectRecord getProject() {
        return project;
    }

    public void setProject(ProjectRecord project) {
        this.project = project;
    }

    public ExperimentMetadataRecord getMetadataRecord() {
        return metadataRecord;
    }

    public void setMetadataRecord(ExperimentMetadataRecord metadataRecord) {
        if (metadataRecord == null) {
            if (this.metadataRecord != null) {
                this.metadataRecord.setExperiment(null);
            }
        } else {
            metadataRecord.setExperiment(this);
        }
        this.metadataRecord = metadataRecord;
    }

    public Set<SelectionCycleRecord> getSelectionCycleRecords() {
        return selectionCycleRecords;
    }

    public void addSelectionCycle(SelectionCycleRecord cycle) {
        cycle.setExperiment(this);
        selectionCycleRecords.add(cycle);
    }

    public void setAptamerRecords(Set<AptamerRecord> aptamerRecords) {
        this.aptamerRecords.clear();
        if (aptamerRecords != null) {
            for (AptamerRecord aptamerRecord : aptamerRecords) {
                addAptamerRecord(aptamerRecord);
            }
        }
    }

    public void addAptamerRecord(AptamerRecord aptamerRecord) {
        aptamerRecord.setExperiment(this);
        aptamerRecords.add(aptamerRecord);
    }
}
