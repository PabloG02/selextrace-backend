package pablog.aptasuite.model.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "experiment_records")
public class ExperimentRecord {

    @Id
    @Column(nullable = false, updatable = false, length = 36)
    private String id;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

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

    @OneToOne
    @JoinColumn(name = "id", referencedColumnName = "experimentId", insertable = false, updatable = false)
    private ExperimentMetadataRecord metadataRecord;

    @OneToMany
    @JoinColumn(name = "experimentId", referencedColumnName = "id", insertable = false, updatable = false)
    private Set<SelectionCycleRecord> selectionCycleRecords = new LinkedHashSet<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Set<AptamerRecord> aptamerRecords = new LinkedHashSet<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
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

    public ExperimentMetadataRecord getMetadataRecord() {
        return metadataRecord;
    }

    public void setMetadataRecord(ExperimentMetadataRecord metadataRecord) {
        this.metadataRecord = metadataRecord;
    }

    public Set<SelectionCycleRecord> getSelectionCycleRecords() {
        return selectionCycleRecords;
    }

    public void setSelectionCycleRecords(Set<SelectionCycleRecord> selectionCycleRecords) {
        this.selectionCycleRecords = selectionCycleRecords;
    }

    public Set<AptamerRecord> getAptamerRecords() {
        return aptamerRecords;
    }

    public void setAptamerRecords(Set<AptamerRecord> aptamerRecords) {
        this.aptamerRecords = aptamerRecords;
    }
}
