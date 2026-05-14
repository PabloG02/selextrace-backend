package pablog.selextrace.model.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class AptamerRecordId implements Serializable {

    @Column(nullable = false)
    private Long experimentId;

    @Column(nullable = false)
    private int aptamerId;

    public AptamerRecordId() {
    }

    public AptamerRecordId(Long experimentId, int aptamerId) {
        this.experimentId = experimentId;
        this.aptamerId = aptamerId;
    }

    public Long getExperimentId() {
        return experimentId;
    }

    public void setExperimentId(Long experimentId) {
        this.experimentId = experimentId;
    }

    public int getAptamerId() {
        return aptamerId;
    }

    public void setAptamerId(int aptamerNumericId) {
        this.aptamerId = aptamerNumericId;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        AptamerRecordId that = (AptamerRecordId) o;
        return aptamerId == that.aptamerId && Objects.equals(experimentId, that.experimentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(experimentId, aptamerId);
    }
}
