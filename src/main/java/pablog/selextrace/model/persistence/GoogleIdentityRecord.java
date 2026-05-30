package pablog.selextrace.model.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import pablog.selextrace.model.auth.IdentityProvider;

@Entity
@DiscriminatorValue("GOOGLE")
@Table(name = "google_identities")
public class GoogleIdentityRecord extends UserIdentityRecord {

    @Column(nullable = false, updatable = false, unique = true)
    private String providerSubject;

    @Override
    public IdentityProvider getProvider() {
        return IdentityProvider.GOOGLE;
    }

    public String getProviderSubject() {
        return providerSubject;
    }

    public void setProviderSubject(String providerSubject) {
        this.providerSubject = providerSubject;
    }
}
