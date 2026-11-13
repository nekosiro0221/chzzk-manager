package model;

import exception.CsvFormatException;
import storage.CsvSerializable;
import storage.CsvUtil;

import java.time.LocalDate;

/**
 * 방송 한 회차의 기록.
 */
public class Broadcast implements CsvSerializable, Comparable<Broadcast> {

    public static final String CSV_HEADER = "id,title,category,date,durationMinutes,maxViewers";

    private final int id;
    private String title;
    private Category category;
    private LocalDate date;
    private int durationMinutes;
    private int maxViewers;

    public Broadcast(int id, String title, Category category, LocalDate date,
                     int durationMinutes, int maxViewers) {
        if (id <= 0) {
            throw new IllegalArgumentException("방송 ID는 1 이상이어야 합니다.");
        }
        this.id = id;
        setTitle(title);
        setCategory(category);
        setDate(date);
        setDurationMinutes(durationMinutes);
        setMaxViewers(maxViewers);
    }

    // ---------- getter ----------

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Category getCategory() {
        return category;
    }

    public LocalDate getDate() {
        return date;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public int getMaxViewers() {
        return maxViewers;
    }

    // ---------- setter (값 검증 포함) ----------

    public void setTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("방송 제목은 비어 있을 수 없습니다.");
        }
        this.title = title.trim();
    }

    public void setCategory(Category category) {
        if (category == null) {
            throw new IllegalArgumentException("카테고리를 지정해야 합니다.");
        }
        this.category = category;
    }

    public void setDate(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("방송 날짜를 지정해야 합니다.");
        }
        this.date = date;
    }

    public void setDurationMinutes(int durationMinutes) {
        if (durationMinutes <= 0 || durationMinutes > 24 * 60) {
            throw new IllegalArgumentException("방송 시간은 1~1440분 사이여야 합니다.");
        }
        this.durationMinutes = durationMinutes;
    }

    public void setMaxViewers(int maxViewers) {
        if (maxViewers < 0) {
            throw new IllegalArgumentException("시청자 수는 0 이상이어야 합니다.");
        }
        this.maxViewers = maxViewers;
    }

    /** "4시간 30분" 형식의 방송 시간 문자열. */
    public String getDurationText() {
        return formatMinutes(durationMinutes);
    }

    public static String formatMinutes(int minutes) {
        int h = minutes / 60;
        int m = minutes % 60;
        if (h == 0) {
            return m + "분";
        }
        return m == 0 ? h + "시간" : h + "시간 " + m + "분";
    }

    /** 날짜 오름차순, 같은 날이면 ID 오름차순. */
    @Override
    public int compareTo(Broadcast other) {
        int cmp = date.compareTo(other.date);
        return cmp != 0 ? cmp : Integer.compare(id, other.id);
    }

    @Override
    public String[] toCsvFields() {
        return new String[]{
                String.valueOf(id), title, category.name(), date.toString(),
                String.valueOf(durationMinutes), String.valueOf(maxViewers)
        };
    }

    public static Broadcast fromCsv(String[] f) throws CsvFormatException {
        CsvUtil.requireFieldCount(f, 6);
        return new Broadcast(
                CsvUtil.parseInt(f[0], "id"),
                f[1],
                CsvUtil.parseEnum(Category.class, f[2], "category"),
                CsvUtil.parseDate(f[3], "date"),
                CsvUtil.parseInt(f[4], "durationMinutes"),
                CsvUtil.parseInt(f[5], "maxViewers"));
    }

    @Override
    public String toString() {
        return String.format("[%3d] %s | %-10s | %s | %s | 최고 %,d명",
                id, date, category.getDisplayName(), title, getDurationText(), maxViewers);
    }
}
