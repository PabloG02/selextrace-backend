package pablog.selextrace.model.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "selection_cycle_records")
public class SelectionCycleRecord {

    @EmbeddedId
    private SelectionCycleRecordId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("experimentId")
    @JoinColumn(name = "experiment_id", nullable = false, updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ExperimentRecord experiment;

    @Column(nullable = false)
    private int round;

    @Column(nullable = false)
    private boolean controlSelection;

    @Column(nullable = false)
    private boolean counterSelection;

    private String barcode5Prime;

    private String barcode3Prime;

    @Column(nullable = false)
    private int totalSize;

    @Column(nullable = false)
    private int uniqueSize;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<Integer, Integer> counts = new HashMap<>();

    public SelectionCycleRecordId getId() {
        return id;
    }

    public void setId(SelectionCycleRecordId id) {
        this.id = id;
    }

    public Long getExperimentId() {
        return id == null ? null : id.getExperimentId();
    }

    protected void setExperimentId(Long experimentId) {
        if (id == null) {
            id = new SelectionCycleRecordId();
        }
        id.setExperimentId(experimentId);
    }

    public String getName() {
        return id == null ? null : id.getName();
    }

    public void setName(String name) {
        if (id == null) {
            id = new SelectionCycleRecordId();
        }
        id.setName(name);
    }

    public ExperimentRecord getExperiment() {
        return experiment;
    }

    protected void setExperiment(ExperimentRecord experiment) {
        this.experiment = experiment;
        if (experiment != null) {
            setExperimentId(experiment.getId());
        }
    }

    public int getRound() {
        return round;
    }

    public void setRound(int round) {
        this.round = round;
    }

    public boolean isControlSelection() {
        return controlSelection;
    }

    public void setControlSelection(boolean controlSelection) {
        this.controlSelection = controlSelection;
    }

    public boolean isCounterSelection() {
        return counterSelection;
    }

    public void setCounterSelection(boolean counterSelection) {
        this.counterSelection = counterSelection;
    }

    public String getBarcode5Prime() {
        return barcode5Prime;
    }

    public void setBarcode5Prime(String barcode5Prime) {
        this.barcode5Prime = barcode5Prime;
    }

    public String getBarcode3Prime() {
        return barcode3Prime;
    }

    public void setBarcode3Prime(String barcode3Prime) {
        this.barcode3Prime = barcode3Prime;
    }

    public int getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(int totalSize) {
        this.totalSize = totalSize;
    }

    public int getUniqueSize() {
        return uniqueSize;
    }

    public void setUniqueSize(int uniqueSize) {
        this.uniqueSize = uniqueSize;
    }

    public Map<Integer, Integer> getCounts() {
        return counts;
    }

    public void setCounts(Map<Integer, Integer> counts) {
        this.counts = counts;
    }
}
