package model;

/**
 * 후원 종류.
 */
public enum DonationType {
    CHAT("일반 후원"),
    VIDEO("영상 후원"),
    MISSION("미션 후원");

    private final String displayName;

    DonationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
