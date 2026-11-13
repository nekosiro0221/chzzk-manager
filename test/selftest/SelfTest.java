package selftest;

import exception.BlacklistedViewerException;
import exception.CsvFormatException;
import exception.DataNotFoundException;
import model.Broadcast;
import model.Category;
import model.Donation;
import model.MissionDonation;
import model.MissionStatus;
import model.VideoDonation;
import service.BroadcastService;
import service.DonationService;
import service.StatisticsService;
import service.ViewerService;
import storage.CsvUtil;
import storage.DataStore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * 외부 라이브러리(JUnit) 없이 실행하는 자동 테스트.
 * 정상 동작과 예외 처리를 함께 검사하고 결과를 출력한다.
 *
 * 실행: README의 "자동 테스트 실행" 참고
 */
public class SelfTest {

    @FunctionalInterface
    private interface TestCase {
        void run() throws Exception;
    }

    private static int passed = 0;
    private static final List<String> failures = new ArrayList<>();

    public static void main(String[] args) {
        System.out.println("===== CHZZK Manager 자동 테스트 =====");

        test("CSV: 쉼표/따옴표가 포함된 값 저장 후 복원", SelfTest::csvRoundTrip);
        test("CSV: 따옴표가 닫히지 않은 줄은 CsvFormatException", SelfTest::csvUnclosedQuote);
        test("방송: 등록 / 수정 / 삭제", SelfTest::broadcastCrud);
        test("방송: 잘못된 값(방송 시간 0분)은 거부되고 원본 유지", SelfTest::broadcastInvalidUpdate);
        test("방송: 없는 ID 조회 시 DataNotFoundException", SelfTest::broadcastNotFound);
        test("방송: 제목 검색(대소문자 무시) / 정렬", SelfTest::broadcastSearchAndSort);
        test("후원: 없는 방송에 후원 등록 시 DataNotFoundException", SelfTest::donationMissingBroadcast);
        test("후원: 블랙리스트 시청자 후원 거부", SelfTest::donationBlacklisted);
        test("후원: 후원 등록 시 시청자 자동 등록", SelfTest::donationAutoRegistersViewer);
        test("후원: 미션 상태별 정산 금액 (다형성)", SelfTest::missionSettlement);
        test("후원: 미션이 아닌 후원의 상태 변경 거부", SelfTest::missionStatusOnNonMission);
        test("후원: 방송 삭제 시 연결된 후원 함께 삭제", SelfTest::cascadeDelete);
        test("시청자: 중복 닉네임 등록 거부", SelfTest::viewerDuplicate);
        test("통계: 월별 요약 / 후원자 랭킹", SelfTest::statistics);
        test("파일: 저장 후 다시 불러오면 데이터와 후원 종류가 그대로", SelfTest::fileRoundTrip);
        test("파일: 파일이 없으면 새로 만들고 빈 상태로 시작", SelfTest::fileMissing);
        test("파일: 손상된 줄은 건너뛰고 정상 줄만 로드", SelfTest::fileCorrupted);

        System.out.println("-------------------------------------");
        System.out.printf("결과: %d개 통과, %d개 실패%n", passed, failures.size());
        for (String f : failures) {
            System.out.println("  실패: " + f);
        }
        if (!failures.isEmpty()) {
            System.exit(1);
        }
    }

    // ---------------- 테스트 케이스 ----------------

    private static void csvRoundTrip() throws Exception {
        String[] original = {"1", "노래방송, 신청곡", "그는 \"안녕\"이라 말했다", "", " 앞뒤공백 "};
        String[] parsed = CsvUtil.parseLine(CsvUtil.join(original));
        check(Arrays.equals(original, parsed), "복원 결과가 다름: " + Arrays.toString(parsed));
    }

    private static void csvUnclosedQuote() {
        expect(CsvFormatException.class, () -> CsvUtil.parseLine("1,\"닫히지 않음,2"));
    }

    private static void broadcastCrud() throws Exception {
        BroadcastService bs = new BroadcastService();
        Broadcast b = bs.add("첫 방송", Category.JUST_CHATTING, LocalDate.of(2026, 11, 1), 60, 100);
        check(b.getId() == 1 && bs.size() == 1, "등록 실패");
        bs.update(1, "수정된 제목", Category.VALORANT, LocalDate.of(2026, 11, 2), 90, 200);
        Broadcast u = bs.findById(1);
        check(u.getTitle().equals("수정된 제목") && u.getCategory() == Category.VALORANT
                && u.getDurationMinutes() == 90 && u.getMaxViewers() == 200, "수정 실패");
        bs.delete(1);
        check(bs.size() == 0, "삭제 실패");
    }

    private static void broadcastInvalidUpdate() throws Exception {
        BroadcastService bs = new BroadcastService();
        bs.add("원본", Category.MUSIC, LocalDate.of(2026, 11, 1), 60, 100);
        expect(IllegalArgumentException.class,
                () -> bs.update(1, "바뀌면 안 됨", Category.MUSIC, LocalDate.of(2026, 11, 1), 0, 100));
        check(bs.findById(1).getTitle().equals("원본"), "잘못된 수정이 일부 반영됨");
    }

    private static void broadcastNotFound() {
        expect(DataNotFoundException.class, () -> new BroadcastService().findById(42));
    }

    private static void broadcastSearchAndSort() {
        BroadcastService bs = new BroadcastService();
        bs.add("LoL 랭크", Category.LEAGUE_OF_LEGENDS, LocalDate.of(2026, 11, 1), 60, 300);
        bs.add("저챗", Category.JUST_CHATTING, LocalDate.of(2026, 11, 2), 60, 900);
        bs.add("lol 내전", Category.LEAGUE_OF_LEGENDS, LocalDate.of(2026, 11, 3), 60, 500);
        check(bs.searchByTitle("LOL").size() == 2, "대소문자 무시 검색 실패");
        check(bs.getAll(BroadcastService.SortOrder.VIEWERS_DESC).get(0).getTitle().equals("저챗"), "시청자순 정렬 실패");
        check(bs.getAll(BroadcastService.SortOrder.DATE_DESC).get(0).getTitle().equals("lol 내전"), "최신순 정렬 실패");
    }

    private static void donationMissingBroadcast() {
        Fixture f = new Fixture();
        expect(DataNotFoundException.class,
                () -> f.ds.registerChat(999, "누군가", 1000, LocalDateTime.now(), "hi"));
    }

    private static void donationBlacklisted() throws Exception {
        Fixture f = new Fixture();
        f.vs.register("도배맨", "");
        f.vs.setBlacklisted("도배맨", true);
        expect(BlacklistedViewerException.class,
                () -> f.ds.registerChat(1, "도배맨", 1000, LocalDateTime.now(), "도배"));
        check(f.ds.size() == 0, "차단된 후원이 저장됨");
    }

    private static void donationAutoRegistersViewer() throws Exception {
        Fixture f = new Fixture();
        f.ds.registerChat(1, "새시청자", 1000, LocalDateTime.of(2026, 11, 1, 20, 0), "첫 후원");
        check(f.vs.find("새시청자").getFirstSeen().equals(LocalDate.of(2026, 11, 1)), "시청자 자동 등록 실패");
    }

    private static void missionSettlement() throws Exception {
        Fixture f = new Fixture();
        Donation d = f.ds.registerMission(1, "미션러", 10000, LocalDateTime.now(), "이기기");
        check(d.getSettledAmount() == 0, "진행중 미션이 정산됨");
        f.ds.updateMissionStatus(d.getId(), MissionStatus.SUCCESS);
        check(d.getSettledAmount() == 10000, "성공 미션이 정산되지 않음");
        f.ds.updateMissionStatus(d.getId(), MissionStatus.FAILED);
        check(d.getSettledAmount() == 0, "실패 미션이 정산됨");
        check(d.getAmount() == 10000, "보낸 금액이 바뀜");
    }

    private static void missionStatusOnNonMission() throws Exception {
        Fixture f = new Fixture();
        Donation d = f.ds.registerChat(1, "일반", 1000, LocalDateTime.now(), "");
        expect(IllegalArgumentException.class, () -> f.ds.updateMissionStatus(d.getId(), MissionStatus.SUCCESS));
    }

    private static void cascadeDelete() throws Exception {
        Fixture f = new Fixture();
        f.bs.add("두번째", Category.OTHER, LocalDate.of(2026, 11, 2), 60, 10);
        f.ds.registerChat(1, "a", 1000, LocalDateTime.now(), "");
        f.ds.registerChat(1, "b", 1000, LocalDateTime.now(), "");
        f.ds.registerChat(2, "c", 1000, LocalDateTime.now(), "");
        check(f.ds.deleteByBroadcast(1) == 2 && f.ds.size() == 1, "연결 후원 삭제 실패");
    }

    private static void viewerDuplicate() {
        ViewerService vs = new ViewerService();
        vs.register("고양이팬", "");
        expect(IllegalArgumentException.class, () -> vs.register("  고양이팬  ", ""));
    }

    private static void statistics() throws Exception {
        Fixture f = new Fixture();
        f.bs.add("12월 방송", Category.OTHER, LocalDate.of(2026, 12, 1), 30, 50);
        f.ds.registerChat(1, "A", 3000, LocalDateTime.of(2026, 11, 1, 20, 0), "");
        f.ds.registerChat(1, "B", 5000, LocalDateTime.of(2026, 11, 1, 21, 0), "");
        Donation m = f.ds.registerMission(1, "A", 4000, LocalDateTime.of(2026, 11, 1, 22, 0), "미션");
        f.ds.updateMissionStatus(m.getId(), MissionStatus.SUCCESS);

        StatisticsService ss = new StatisticsService(f.bs, f.ds);
        StatisticsService.MonthlySummary nov = ss.getMonthlySummary(YearMonth.of(2026, 11));
        check(nov.getBroadcastCount() == 1 && nov.getTotalMinutes() == 120, "11월 방송 집계 오류");
        check(nov.getDonations().getSentAmount() == 12000 && nov.getDonations().getSettledAmount() == 12000,
                "11월 후원 집계 오류");
        List<StatisticsService.RankEntry> top = ss.getTopDonors(10);
        check(top.get(0).getLabel().equals("A") && top.get(0).getTally().getSettledAmount() == 7000, "랭킹 1위 오류");
        check(ss.getAvailableMonths().get(0).equals(YearMonth.of(2026, 12)), "월 목록 정렬 오류");
    }

    private static void fileRoundTrip() throws Exception {
        Path dir = Files.createTempDirectory("chzzk-test");
        try {
            Fixture f = new Fixture();
            f.vs.register("차단됨", "메모, 쉼표 포함");
            f.vs.setBlacklisted("차단됨", true);
            f.ds.registerChat(1, "A", 1000, LocalDateTime.of(2026, 11, 1, 20, 0), "안녕, \"반가워\"");
            f.ds.registerVideo(1, "B", 2000, LocalDateTime.of(2026, 11, 1, 20, 5), "https://youtu.be/x", 90);
            Donation m = f.ds.registerMission(1, "C", 3000, LocalDateTime.of(2026, 11, 1, 20, 10), "미션");
            f.ds.updateMissionStatus(m.getId(), MissionStatus.FAILED);
            new DataStore(dir).saveAll(f.bs, f.ds, f.vs);

            Fixture g = new Fixture(false);
            List<String> warnings = new DataStore(dir).loadAll(g.bs, g.ds, g.vs);
            check(warnings.isEmpty(), "경고가 발생함: " + warnings);
            check(g.bs.size() == 1 && g.ds.size() == 3 && g.vs.size() == 4, "개수가 다름");
            check(g.ds.findById(2) instanceof VideoDonation, "영상 후원 타입이 복원되지 않음");
            check(((MissionDonation) g.ds.findById(3)).getStatus() == MissionStatus.FAILED, "미션 상태가 복원되지 않음");
            check(g.ds.findById(1).describeDetail().contains("\"반가워\""), "메시지가 복원되지 않음");
            check(g.vs.find("차단됨").isBlacklisted() && g.vs.find("차단됨").getMemo().equals("메모, 쉼표 포함"),
                    "시청자 정보가 복원되지 않음");
        } finally {
            deleteRecursively(dir);
        }
    }

    private static void fileMissing() throws Exception {
        Path dir = Files.createTempDirectory("chzzk-test").resolve("new-folder");
        try {
            Fixture g = new Fixture(false);
            List<String> warnings = new DataStore(dir).loadAll(g.bs, g.ds, g.vs);
            check(warnings.size() == 3, "파일 생성 안내가 3개여야 함: " + warnings);
            check(Files.exists(dir.resolve("broadcasts.csv")), "파일이 생성되지 않음");
            check(g.bs.size() == 0 && g.ds.size() == 0 && g.vs.size() == 0, "빈 상태가 아님");
        } finally {
            deleteRecursively(dir.getParent());
        }
    }

    private static void fileCorrupted() throws Exception {
        Path dir = Files.createTempDirectory("chzzk-test");
        try {
            write(dir.resolve("broadcasts.csv"),
                    Broadcast.CSV_HEADER,
                    "1,정상 방송,MUSIC,2026-11-01,60,100",
                    "2,숫자 오류,MUSIC,2026-11-02,육십,100",
                    "3,카테고리 오류,UNKNOWN,2026-11-03,60,100",
                    "4,필드 부족,MUSIC",
                    "5,날짜 오류,MUSIC,2026-13-45,60,100",
                    "6,음수 시간,MUSIC,2026-11-06,-5,100");
            write(dir.resolve("donations.csv"),
                    Donation.CSV_HEADER,
                    "1,CHAT,1,A,1000,2026-11-01T20:00,정상,",
                    "2,CHAT,99,B,1000,2026-11-01T20:00,없는 방송,",
                    "3,MISSION,1,C,1000,2026-11-01T20:00,미션,MAYBE");
            write(dir.resolve("viewers.csv"), model.Viewer.CSV_HEADER);

            Fixture g = new Fixture(false);
            List<String> warnings = new DataStore(dir).loadAll(g.bs, g.ds, g.vs);
            check(g.bs.size() == 1, "정상 방송 1건만 로드되어야 함 (실제 " + g.bs.size() + ")");
            check(g.ds.size() == 1, "정상 후원 1건만 로드되어야 함 (실제 " + g.ds.size() + ")");
            check(warnings.size() == 7, "경고 7개가 나와야 함: " + warnings);
        } finally {
            deleteRecursively(dir);
        }
    }

    // ---------------- 도우미 ----------------

    /** 방송 1건(ID 1)이 등록된 서비스 묶음. */
    private static class Fixture {
        final BroadcastService bs = new BroadcastService();
        final ViewerService vs = new ViewerService();
        final DonationService ds = new DonationService(bs, vs);

        Fixture() {
            this(true);
        }

        Fixture(boolean withBroadcast) {
            if (withBroadcast) {
                bs.add("테스트 방송", Category.JUST_CHATTING, LocalDate.of(2026, 11, 1), 120, 100);
            }
        }
    }

    private static void test(String name, TestCase testCase) {
        try {
            testCase.run();
            passed++;
            System.out.println("[PASS] " + name);
        } catch (Throwable t) {
            failures.add(name + " - " + t);
            System.out.println("[FAIL] " + name + " - " + t.getMessage());
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void expect(Class<? extends Throwable> expected, TestCase action) {
        try {
            action.run();
        } catch (Throwable t) {
            if (expected.isInstance(t)) {
                return;
            }
            throw new AssertionError(expected.getSimpleName() + " 대신 " + t + " 발생");
        }
        throw new AssertionError(expected.getSimpleName() + "가 발생하지 않음");
    }

    private static void write(Path file, String... lines) throws IOException {
        Files.write(file, Arrays.asList(lines), StandardCharsets.UTF_8);
    }

    private static void deleteRecursively(Path dir) throws IOException {
        if (dir == null || !Files.exists(dir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        }
    }
}
