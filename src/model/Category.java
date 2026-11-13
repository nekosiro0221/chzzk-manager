package model;

/**
 * 방송 카테고리.
 */
public enum Category {
    LEAGUE_OF_LEGENDS("리그 오브 레전드"),
    JUST_CHATTING("저스트 채팅"),
    VALORANT("발로란트"),
    MAPLESTORY("메이플스토리"),
    BATTLEGROUNDS("배틀그라운드"),
    MINECRAFT("마인크래프트"),
    MUSIC("음악/노래"),
    OTHER("기타");

    private final String displayName;

    Category(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
