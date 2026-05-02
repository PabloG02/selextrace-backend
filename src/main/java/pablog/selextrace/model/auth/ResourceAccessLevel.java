package pablog.selextrace.model.auth;

public enum ResourceAccessLevel {
    MANAGER,
    VIEWER;

    public boolean allowsManagement() {
        return this == MANAGER;
    }
}
