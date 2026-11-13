package storage;

import exception.CsvFormatException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * CSV 한 줄의 생성/파싱과 필드 변환을 담당하는 유틸리티 클래스.
 * 쉼표(,)나 따옴표(")가 들어간 값도 안전하게 저장/복원한다.
 */
public final class CsvUtil {

    private CsvUtil() {
        // 인스턴스 생성 방지
    }

    /** 필드 배열을 CSV 한 줄로 합친다. */
    public static String join(String[] fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(escape(fields[i]));
        }
        return sb.toString();
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        // 한 줄 = 한 레코드 이므로 줄바꿈은 공백으로 치환
        String v = value.replace("\r", " ").replace("\n", " ");
        if (v.contains(",") || v.contains("\"") || v.startsWith(" ") || v.endsWith(" ")) {
            return "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }

    /** CSV 한 줄을 필드 배열로 분리한다. 따옴표 처리를 지원한다. */
    public static String[] parseLine(String line) throws CsvFormatException {
        List<String> fields = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cur.append('"'); // "" -> "
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cur.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                fields.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        if (inQuotes) {
            throw new CsvFormatException("따옴표가 닫히지 않았습니다.");
        }
        fields.add(cur.toString());
        return fields.toArray(new String[0]);
    }

    // ---------- 필드 변환 헬퍼 (형식이 잘못되면 CsvFormatException) ----------

    public static void requireFieldCount(String[] f, int expected) throws CsvFormatException {
        if (f.length != expected) {
            throw new CsvFormatException("필드 개수가 " + expected + "개여야 하는데 " + f.length + "개입니다.");
        }
    }

    public static int parseInt(String value, String fieldName) throws CsvFormatException {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new CsvFormatException(fieldName + " 값이 숫자가 아닙니다: '" + value + "'");
        }
    }

    public static LocalDate parseDate(String value, String fieldName) throws CsvFormatException {
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new CsvFormatException(fieldName + " 날짜 형식이 잘못되었습니다: '" + value + "'");
        }
    }

    public static LocalDateTime parseDateTime(String value, String fieldName) throws CsvFormatException {
        try {
            return LocalDateTime.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new CsvFormatException(fieldName + " 날짜/시간 형식이 잘못되었습니다: '" + value + "'");
        }
    }

    public static boolean parseBoolean(String value, String fieldName) throws CsvFormatException {
        String v = value.trim().toLowerCase();
        if (v.equals("true")) {
            return true;
        }
        if (v.equals("false")) {
            return false;
        }
        throw new CsvFormatException(fieldName + " 값은 true/false 여야 합니다: '" + value + "'");
    }

    public static <E extends Enum<E>> E parseEnum(Class<E> type, String value, String fieldName)
            throws CsvFormatException {
        try {
            return Enum.valueOf(type, value.trim());
        } catch (IllegalArgumentException e) {
            throw new CsvFormatException(fieldName + " 값이 올바르지 않습니다: '" + value + "'");
        }
    }
}
