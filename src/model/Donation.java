package model;

import exception.CsvFormatException;
import storage.CsvSerializable;
import storage.CsvUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 후원(치즈) 한 건을 나타내는 추상 클래스.
 * 종류별 세부 정보와 정산 방식은 하위 클래스(일반/영상/미션)가 정의한다.
 */
public abstract class Donation implements CsvSerializable, Comparable<Donation> {

    public static final String CSV_HEADER = "id,type,broadcastId,donor,amount,dateTime,detail1,detail2";
    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final int id;
    private final int broadcastId;
    private final String donor;
    private final int amount;
    private final LocalDateTime dateTime;

    protected Donation(int id, int broadcastId, String donor, int amount, LocalDateTime dateTime) {
        if (id <= 0) {
            throw new IllegalArgumentException("후원 ID는 1 이상이어야 합니다.");
        }
        if (donor == null || donor.trim().isEmpty()) {
            throw new IllegalArgumentException("후원자 닉네임은 비어 있을 수 없습니다.");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("후원 금액은 1 이상이어야 합니다.");
        }
        if (dateTime == null) {
            throw new IllegalArgumentException("후원 일시를 지정해야 합니다.");
        }
        this.id = id;
        this.broadcastId = broadcastId;
        this.donor = donor.trim();
        this.amount = amount;
        this.dateTime = dateTime;
    }

    public int getId() {
        return id;
    }

    public int getBroadcastId() {
        return broadcastId;
    }

    public String getDonor() {
        return donor;
    }

    /** 후원자가 보낸 금액. */
    public int getAmount() {
        return amount;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    /**
     * 실제로 정산(수익으로 인정)되는 금액.
     * 기본은 후원 금액 전체이며, 미션 후원처럼 조건이 있는 경우 하위 클래스가 재정의한다.
     */
    public int getSettledAmount() {
        return amount;
    }

    /** 후원 종류. */
    public abstract DonationType getType();

    /** 종류별 세부 정보를 사람이 읽기 좋은 문자열로 반환. */
    public abstract String describeDetail();

    /** 종류별 세부 정보를 CSV 필드 2개(detail1, detail2)로 반환. */
    protected abstract String[] detailFields();

    @Override
    public final String[] toCsvFields() {
        String[] detail = detailFields();
        return new String[]{
                String.valueOf(id), getType().name(), String.valueOf(broadcastId), donor,
                String.valueOf(amount), dateTime.toString(), detail[0], detail[1]
        };
    }

    /** CSV 필드로부터 종류에 맞는 하위 클래스 객체를 만든다 (팩토리 메소드). */
    public static Donation fromCsv(String[] f) throws CsvFormatException {
        CsvUtil.requireFieldCount(f, 8);
        int id = CsvUtil.parseInt(f[0], "id");
        DonationType type = CsvUtil.parseEnum(DonationType.class, f[1], "type");
        int broadcastId = CsvUtil.parseInt(f[2], "broadcastId");
        String donor = f[3];
        int amount = CsvUtil.parseInt(f[4], "amount");
        LocalDateTime dateTime = CsvUtil.parseDateTime(f[5], "dateTime");

        switch (type) {
            case CHAT:
                return new ChatDonation(id, broadcastId, donor, amount, dateTime, f[6]);
            case VIDEO:
                return new VideoDonation(id, broadcastId, donor, amount, dateTime,
                        f[6], CsvUtil.parseInt(f[7], "videoSeconds"));
            case MISSION:
                return new MissionDonation(id, broadcastId, donor, amount, dateTime,
                        f[6], CsvUtil.parseEnum(MissionStatus.class, f[7], "missionStatus"));
            default:
                throw new CsvFormatException("알 수 없는 후원 종류: " + type);
        }
    }

    /** 일시 오름차순, 같으면 ID 오름차순. */
    @Override
    public int compareTo(Donation other) {
        int cmp = dateTime.compareTo(other.dateTime);
        return cmp != 0 ? cmp : Integer.compare(id, other.id);
    }

    @Override
    public String toString() {
        return String.format("[%3d] %s | 방송#%d | %s | %s님 %,d치즈 | %s",
                id, dateTime.format(DISPLAY_FORMAT), broadcastId, getType().getDisplayName(),
                donor, amount, describeDetail());
    }
}
