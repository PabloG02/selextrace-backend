package pablog.selextrace.model.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import pablog.selextrace.model.auth.IdentityProvider;

@Entity
@DiscriminatorValue("LOCAL")
@Table(name = "password_identities")
public class PasswordIdentityRecord extends UserIdentityRecord {

    @Column(nullable = false, length = 255)
    private String passwordHash;

    @Override
    public IdentityProvider getProvider() { return IdentityProvider.PASSWORD; }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
}
