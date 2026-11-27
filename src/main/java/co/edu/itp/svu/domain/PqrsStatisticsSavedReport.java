package co.edu.itp.svu.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

/** Immutable statistical snapshot with editable narrative and ownership metadata. */
@Document(collection = "pqrs_statistics_saved_report")
public class PqrsStatisticsSavedReport {

    @Id
    private String id;

    private String title;
    private String scope;
    private LocalDate startDate;
    private LocalDate endDate;
    private String officeId;
    private String officeIdType;
    private String officeIdentity;
    private String officeName;
    private Instant cutoff;
    private String statisticsSnapshot;
    private long received;
    private Map<String, String> narrative;
    private List<String> availableIndicators;
    private Instant checkedAt;
    private String createdBy;
    private String modifiedBy;
    private Instant createdAt;
    private Instant modifiedAt;
    private Instant generatedAt;
    private int modelVersion;
    private int methodologyVersion;
    private int catalogVersion;

    @Version
    private Long version;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getOfficeId() {
        return officeId;
    }

    public void setOfficeId(String officeId) {
        this.officeId = officeId;
    }

    public String getOfficeIdType() {
        return officeIdType;
    }

    public void setOfficeIdType(String officeIdType) {
        this.officeIdType = officeIdType;
    }

    public String getOfficeIdentity() {
        return officeIdentity;
    }

    public void setOfficeIdentity(String officeIdentity) {
        this.officeIdentity = officeIdentity;
    }

    public String getOfficeName() {
        return officeName;
    }

    public void setOfficeName(String officeName) {
        this.officeName = officeName;
    }

    public Instant getCutoff() {
        return cutoff;
    }

    public void setCutoff(Instant cutoff) {
        this.cutoff = cutoff;
    }

    public String getStatisticsSnapshot() {
        return statisticsSnapshot;
    }

    public void setStatisticsSnapshot(String statisticsSnapshot) {
        this.statisticsSnapshot = statisticsSnapshot;
    }

    public long getReceived() {
        return received;
    }

    public void setReceived(long received) {
        this.received = received;
    }

    public Map<String, String> getNarrative() {
        return narrative;
    }

    public void setNarrative(Map<String, String> narrative) {
        this.narrative = Map.copyOf(narrative);
    }

    public List<String> getAvailableIndicators() {
        return availableIndicators;
    }

    public void setAvailableIndicators(List<String> availableIndicators) {
        this.availableIndicators = List.copyOf(availableIndicators);
    }

    public Instant getCheckedAt() {
        return checkedAt;
    }

    public void setCheckedAt(Instant checkedAt) {
        this.checkedAt = checkedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getModifiedAt() {
        return modifiedAt;
    }

    public void setModifiedAt(Instant modifiedAt) {
        this.modifiedAt = modifiedAt;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Instant generatedAt) {
        this.generatedAt = generatedAt;
    }

    public int getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(int modelVersion) {
        this.modelVersion = modelVersion;
    }

    public int getMethodologyVersion() {
        return methodologyVersion;
    }

    public void setMethodologyVersion(int methodologyVersion) {
        this.methodologyVersion = methodologyVersion;
    }

    public int getCatalogVersion() {
        return catalogVersion;
    }

    public void setCatalogVersion(int catalogVersion) {
        this.catalogVersion = catalogVersion;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "Saved PQRSD report";
    }
}
