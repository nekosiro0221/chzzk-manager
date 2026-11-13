package model;

import exception.CsvFormatException;
import storage.CsvSerializable;
import storage.CsvUtil;

import java.time.LocalDate;

/**
 * 시청자(후원자) 정보. 닉네임으로 구분한다.
 */
public class Viewer implements CsvSerializable {

    public static final String CSV_HEADER = "nickname,firstSeen,blacklisted,memo";

    private final String nickname;
    private final LocalDate firstSeen;
    private boolean blacklisted;
    private String memo;

    public Viewer(String nickname, LocalDate firstSeen, boolean blacklisted, String memo) {
        if (nickname == null || nickname.trim().isEmpty()) {
            throw new IllegalArgumentException("닉네임은 비어 있을 수 없습니다.");
        }
        if (firstSeen == null) {
            throw new IllegalArgumentException("첫 방문일을 지정해야 합니다.");
        }
        this.nickname = nickname.trim();
        this.firstSeen = firstSeen;
        this.blacklisted = blacklisted;
        this.memo = memo == null ? "" : memo.trim();
    }

    public String getNickname() {
        return nickname;
    }

    public LocalDate getFirstSeen() {
        return firstSeen;
    }

    public boolean isBlacklisted() {
        return blacklisted;
    }

    public void setBlacklisted(boolean blacklisted) {
        this.blacklisted = blacklisted;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo == null ? "" : memo.trim();
    }

    @Override
    public String[] toCsvFields() {
        return new String[]{nickname, firstSeen.toString(), String.valueOf(blacklisted), memo};
    }

    public static Viewer fromCsv(String[] f) throws CsvFormatException {
        CsvUtil.requireFieldCount(f, 4);
        return new Viewer(
                f[0],
                CsvUtil.parseDate(f[1], "firstSeen"),
                CsvUtil.parseBoolean(f[2], "blacklisted"),
                f[3]);
    }

    @Override
    public String toString() {
        return String.format("%s%s | 첫 방문 %s%s",
                nickname,
                blacklisted ? " [차단]" : "",
                firstSeen,
                memo.isEmpty() ? "" : " | 메모: " + memo);
    }
}
