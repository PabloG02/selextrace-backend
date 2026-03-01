package pablog.selextrace.model.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Table;

@Entity
@Table(name = "aptamer_records")
public class AptamerRecord {

    @EmbeddedId
    private AptamerRecordId id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String sequence;

    @Column(nullable = false)
    private int startIndex;

    @Column(nullable = false)
    private int endIndex;

    public AptamerRecordId getId() {
        return id;
    }

    public void setId(AptamerRecordId id) {
        this.id = id;
    }

    public String getExperimentId() {
        return id == null ? null : id.getExperimentId();
    }

    public void setExperimentId(String experimentId) {
        if (id == null) {
            id = new AptamerRecordId();
        }
        id.setExperimentId(experimentId);
    }

    public int getAptamerNumericId() {
        return id == null ? 0 : id.getAptamerId();
    }

    public void setAptamerNumericId(int aptamerNumericId) {
        if (id == null) {
            id = new AptamerRecordId();
        }
        id.setAptamerId(aptamerNumericId);
    }

    public String getSequence() {
        return sequence;
    }

    public void setSequence(String sequence) {
        this.sequence = sequence;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public void setStartIndex(int startIndex) {
        this.startIndex = startIndex;
    }

    public int getEndIndex() {
        return endIndex;
    }

    public void setEndIndex(int endIndex) {
        this.endIndex = endIndex;
    }
}
