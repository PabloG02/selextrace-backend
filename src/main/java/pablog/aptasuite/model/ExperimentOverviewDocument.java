package pablog.aptasuite.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import pablog.aptasuite.dto.ExperimentOverviewDTO;

import java.time.Instant;

@Document(collection = "experiments")
public class ExperimentOverviewDocument {

    @Id
    private String id;
    private ExperimentOverviewDTO overview;

    @CreatedDate
    private Instant createdAt;

    public ExperimentOverviewDocument() {
    }

    public ExperimentOverviewDocument(ExperimentOverviewDTO overview) {
        this.overview = overview;
    }

    public ExperimentOverviewDocument(String id, ExperimentOverviewDTO overview) {
        this.id = id;
        this.overview = overview;
    }

    public String getId() {
        return id;
    }

    public ExperimentOverviewDTO getOverview() {
        return overview;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setOverview(ExperimentOverviewDTO overview) {
        this.overview = overview;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
