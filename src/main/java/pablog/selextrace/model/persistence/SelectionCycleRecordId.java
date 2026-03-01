package pablog.selextrace.model.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class SelectionCycleRecordId implements Serializable {

    @Column(nullable = false, length = 36)
    private String experimentId;

    @Column(nullable = false)
    private String name;

    public SelectionCycleRecordId() {
    }

    public SelectionCycleRecordId(String experimentId, String name) {
        this.experimentId = experimentId;
        this.name = name;
    }

    public String getExperimentId() {
        return experimentId;
    }

    public void setExperimentId(String experimentId) {
        this.experimentId = experimentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        SelectionCycleRecordId that = (SelectionCycleRecordId) o;
        return Objects.equals(experimentId, that.experimentId) && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(experimentId, name);
    }
}
