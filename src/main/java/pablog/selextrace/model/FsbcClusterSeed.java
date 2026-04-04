package pablog.selextrace.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;

@Embeddable
public class FsbcClusterSeed implements Serializable {

    @Column(name = "cluster_id", nullable = false)
    private int clusterId;

    @Column(name = "seed_string", nullable = false, length = 128)
    private String seedString;

    @Column(name = "member_count", nullable = false)
    private int memberCount;

    @Column(name = "total_count", nullable = false)
    private int totalCount;

    public FsbcClusterSeed() {
    }

    public FsbcClusterSeed(int clusterId, String seedString, int memberCount, int totalCount) {
        this.clusterId = clusterId;
        this.seedString = seedString;
        this.memberCount = memberCount;
        this.totalCount = totalCount;
    }

    public int getClusterId() {
        return clusterId;
    }

    public void setClusterId(int clusterId) {
        this.clusterId = clusterId;
    }

    public String getSeedString() {
        return seedString;
    }

    public void setSeedString(String seedString) {
        this.seedString = seedString;
    }

    public int getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(int memberCount) {
        this.memberCount = memberCount;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }
}
