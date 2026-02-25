package pablog.aptasuite.model.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class AptamerRecordId implements Serializable {

    @Column(nullable = false, length = 36)
    private String experimentId;

    @Column(nullable = false)
    private int aptamerId;

    public AptamerRecordId() {
    }

    public AptamerRecordId(String experimentId, int aptamerId) {
        this.experimentId = experimentId;
        this.aptamerId = aptamerId;
    }

    public String getExperimentId() {
        return experimentId;
    }

    public void setExperimentId(String experimentId) {
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
