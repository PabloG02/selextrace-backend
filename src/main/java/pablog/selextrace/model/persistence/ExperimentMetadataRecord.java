package pablog.selextrace.model.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;
import pablog.selextrace.domain.metadata.Accumulator;
import pablog.selextrace.domain.metadata.Metadata;
import pablog.selextrace.domain.metadata.ParserStat;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Entity
@Table(name = "experiment_metadata_records")
public class ExperimentMetadataRecord {

    @Id
    @Column(nullable = false, updatable = false)
    private Long experimentId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "experiment_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ExperimentRecord experiment;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, ConcurrentHashMap<Integer, Accumulator>> qualityScoresForward = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, ConcurrentHashMap<Integer, Accumulator>> qualityScoresReverse = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, ConcurrentHashMap<Integer, ConcurrentHashMap<Byte, Integer>>> nucleotideDistributionForward = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, ConcurrentHashMap<Integer, ConcurrentHashMap<Byte, Integer>>> nucleotideDistributionReverse = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, ConcurrentHashMap<Byte, Integer>>>>
            nucleotideDistributionAccepted = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<ParserStat, Integer> parserStatistics = new EnumMap<>(ParserStat.class);

    public Long getExperimentId() {
        return experimentId;
    }

    public ExperimentRecord getExperiment() {
        return experiment;
    }

    protected void setExperiment(ExperimentRecord experiment) {
        this.experiment = experiment;
        if (experiment != null) {
            this.experimentId = experiment.getId();
        }
    }

    public Metadata getMetadata() {
        Metadata metadata = new Metadata();
        metadata.qualityScoresForward = qualityScoresForward;
        metadata.qualityScoresReverse = qualityScoresReverse;
        metadata.nucleotideDistributionForward = nucleotideDistributionForward;
        metadata.nucleotideDistributionReverse = nucleotideDistributionReverse;
        metadata.nucleotideDistributionAccepted = nucleotideDistributionAccepted;
        metadata.parserStatistics = parserStatistics;
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        if (metadata == null) {
            metadata = new Metadata();
        }

        this.qualityScoresForward = metadata.qualityScoresForward;
        this.qualityScoresReverse = metadata.qualityScoresReverse;
        this.nucleotideDistributionForward = metadata.nucleotideDistributionForward;
        this.nucleotideDistributionReverse = metadata.nucleotideDistributionReverse;
        this.nucleotideDistributionAccepted = metadata.nucleotideDistributionAccepted;
        this.parserStatistics = metadata.parserStatistics;
    }
}