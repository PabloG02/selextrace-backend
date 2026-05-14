package pablog.selextrace.model.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;
import pablog.selextrace.domain.metadata.Metadata;

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
    private Metadata metadata;

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
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }
}