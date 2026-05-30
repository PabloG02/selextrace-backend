package pablog.selextrace.model.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import pablog.selextrace.model.auth.IdentityProvider;

@Entity
@DiscriminatorValue("PASSWORD")
@Table(name = "password_identities")
public class PasswordIdentityRecord extends UserIdentityRecord {

    @Column(nullable = false, length = 255)
    private String passwordHash;

    /// When `true`, the user must set a new password on their next login.
    @Column(nullable = false)
    private boolean mustChangePassword = false;

    @Override
    public IdentityProvider getProvider() { return IdentityProvider.PASSWORD; }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public boolean getMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }
}
