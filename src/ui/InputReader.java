package ui;

import exception.InputEndedException;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;
import java.util.function.Function;

/**
 * 콘솔 입력을 받아 검증하는 클래스.
 * 잘못된 값을 입력하면 오류 메시지를 보여주고 올바른 값을 입력할 때까지 다시 묻는다.
 */
public class InputReader {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("H:mm");

    private final Scanner scanner;

    public InputReader(InputStream in, Charset charset) {
        this.scanner = new Scanner(in, charset.name());
    }

    /** 한 줄을 읽는다 (앞뒤 공백 제거). 입력이 끝났으면 InputEndedException. */
    public String readLine(String prompt) {
        System.out.print(prompt);
        if (!scanner.hasNextLine()) {
            System.out.println();
            throw new InputEndedException();
        }
        return scanner.nextLine().trim();
    }

    /** 비어 있지 않은 문자열을 입력받는다. */
    public String readRequired(String prompt) {
        while (true) {
            String s = readLine(prompt);
            if (!s.isEmpty()) {
                return s;
            }
            System.out.println("  [!] 값을 입력해 주세요.");
        }
    }

    /** 엔터만 누르면 기존 값을 유지한다 (수정 화면용). */
    public String readOrKeep(String prompt, String current) {
        String s = readLine(prompt + " [현재: " + current + "] (엔터=유지): ");
        return s.isEmpty() ? current : s;
    }

    /** min~max 범위의 정수를 입력받는다. "10,000"처럼 쉼표가 있어도 된다. */
    public int readInt(String prompt, int min, int max) {
        while (true) {
            String s = readLine(prompt);
            Integer value = parseIntInRange(s, min, max);
            if (value != null) {
                return value;
            }
        }
    }

    /** 엔터만 누르면 기본값을 사용한다. */
    public int readIntOrDefault(String prompt, int min, int max, int defaultValue) {
        while (true) {
            String s = readLine(prompt);
            if (s.isEmpty()) {
                return defaultValue;
            }
            Integer value = parseIntInRange(s, min, max);
            if (value != null) {
                return value;
            }
        }
    }

    private Integer parseIntInRange(String s, int min, int max) {
        try {
            int value = Integer.parseInt(s.replace(",", "").replace(" ", ""));
            if (value < min || value > max) {
                System.out.printf("  [!] %,d ~ %,d 사이의 숫자를 입력해 주세요.%n", min, max);
                return null;
            }
            return value;
        } catch (NumberFormatException e) {
            System.out.println("  [!] 숫자를 입력해 주세요.");
            return null;
        }
    }

    /** yyyy-MM-dd 형식의 날짜를 입력받는다. 엔터만 누르면 기본값. */
    public LocalDate readDate(String prompt, LocalDate defaultValue) {
        while (true) {
            String s = readLine(prompt + " (yyyy-MM-dd, 엔터=" + defaultValue + "): ");
            if (s.isEmpty()) {
                return defaultValue;
            }
            try {
                return LocalDate.parse(s);
            } catch (DateTimeParseException e) {
                System.out.println("  [!] 날짜 형식이 잘못되었습니다. 예) 2026-11-13");
            }
        }
    }

    /** HH:mm 형식의 시각을 입력받는다. 엔터만 누르면 기본값. */
    public LocalTime readTime(String prompt, LocalTime defaultValue) {
        while (true) {
            String s = readLine(prompt + " (HH:mm, 엔터=" + defaultValue.format(TIME_FORMAT) + "): ");
            if (s.isEmpty()) {
                return defaultValue;
            }
            try {
                return LocalTime.parse(s, TIME_FORMAT);
            } catch (DateTimeParseException e) {
                System.out.println("  [!] 시각 형식이 잘못되었습니다. 예) 21:30");
            }
        }
    }

    /** y/n 질문. */
    public boolean confirm(String prompt) {
        while (true) {
            String s = readLine(prompt + " (y/n): ").toLowerCase();
            if (s.equals("y") || s.equals("yes") || s.equals("ㅛ")) {
                return true;
            }
            if (s.equals("n") || s.equals("no") || s.equals("ㅜ")) {
                return false;
            }
            System.out.println("  [!] y 또는 n을 입력해 주세요.");
        }
    }

    /** 목록에서 번호로 하나를 고른다. */
    public <T> T choose(String title, List<T> options, Function<T, String> labeler) {
        printOptions(title, options, labeler);
        int n = readInt("번호 선택> ", 1, options.size());
        return options.get(n - 1);
    }

    /** 목록에서 번호로 하나를 고른다. 엔터만 누르면 현재 값을 유지한다. */
    public <T> T chooseOrKeep(String title, List<T> options, Function<T, String> labeler, T current) {
        printOptions(title, options, labeler);
        int n = readIntOrDefault("번호 선택 [현재: " + labeler.apply(current) + "] (엔터=유지)> ",
                1, options.size(), 0);
        return n == 0 ? current : options.get(n - 1);
    }

    private <T> void printOptions(String title, List<T> options, Function<T, String> labeler) {
        System.out.println(title);
        for (int i = 0; i < options.size(); i++) {
            System.out.printf("  %d. %s%n", i + 1, labeler.apply(options.get(i)));
        }
    }
}
