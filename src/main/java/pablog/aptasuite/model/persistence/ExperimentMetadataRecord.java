package pablog.aptasuite.model.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pablog.aptasuite.domain.metadata.Metadata;

@Entity
@Table(name = "experiment_metadata_records")
public class ExperimentMetadataRecord {

    @Id
    @Column(nullable = false, updatable = false, length = 36)
    private String experimentId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Metadata metadata;

    public String getExperimentId() {
        return experimentId;
    }

    public void setExperimentId(String experimentId) {
        this.experimentId = experimentId;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }
}
