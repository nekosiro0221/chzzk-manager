package model;

/**
 * 미션 후원의 진행 상태.
 */
public enum MissionStatus {
    PENDING("진행중"),
    SUCCESS("성공"),
    FAILED("실패");

    private final String displayName;

    MissionStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
