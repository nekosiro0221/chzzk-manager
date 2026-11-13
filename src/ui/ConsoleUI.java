package ui;

import exception.BlacklistedViewerException;
import exception.DataNotFoundException;
import exception.InputEndedException;
import model.Broadcast;
import model.Category;
import model.Donation;
import model.DonationType;
import model.MissionDonation;
import model.MissionStatus;
import model.Viewer;
import service.BroadcastService;
import service.DonationService;
import service.StatisticsService;
import service.ViewerService;
import storage.DataStore;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 콘솔 메뉴 화면. 사용자 입력을 받아 서비스 객체를 호출하고 결과를 출력한다.
 * 데이터를 변경하는 작업이 끝나면 자동으로 파일에 저장한다.
 */
public class ConsoleUI {

    /** 메뉴에서 실행할 작업. 서비스가 던지는 예외를 한 곳에서 처리하기 위한 함수형 인터페이스. */
    @FunctionalInterface
    private interface Action {
        void run() throws DataNotFoundException, BlacklistedViewerException;
    }

    private static final String LINE = "------------------------------------------------------------";
    private static final int RECENT_COUNT = 5;

    private final BroadcastService broadcastService;
    private final DonationService donationService;
    private final ViewerService viewerService;
    private final StatisticsService statisticsService;
    private final DataStore dataStore;
    private final InputReader in;

    public ConsoleUI(BroadcastService broadcastService, DonationService donationService,
                     ViewerService viewerService, StatisticsService statisticsService,
                     DataStore dataStore, InputReader in) {
        this.broadcastService = broadcastService;
        this.donationService = donationService;
        this.viewerService = viewerService;
        this.statisticsService = statisticsService;
        this.dataStore = dataStore;
        this.in = in;
    }

    // =====================================================================
    // 시작 / 메인 메뉴
    // =====================================================================

    public void run() {
        System.out.println("============================================================");
        System.out.println("              치지직 방송 관리 매니저 (CHZZK Manager)");
        System.out.println("============================================================");
        load();

        try {
            while (true) {
                System.out.println();
                System.out.println("[메인 메뉴]");
                System.out.println("  1. 방송 기록 관리");
                System.out.println("  2. 후원(치즈) 관리");
                System.out.println("  3. 시청자 관리");
                System.out.println("  4. 통계 / 분석");
                System.out.println("  5. 지금 저장하기");
                System.out.println("  0. 저장 후 종료");
                int menu = in.readInt("선택> ", 0, 5);
                switch (menu) {
                    case 1: broadcastMenu(); break;
                    case 2: donationMenu(); break;
                    case 3: viewerMenu(); break;
                    case 4: statisticsMenu(); break;
                    case 5: save(true); break;
                    case 0:
                        save(true);
                        System.out.println("프로그램을 종료합니다. 방송 화이팅!");
                        return;
                    default: break;
                }
            }
        } catch (InputEndedException e) {
            // Ctrl+Z / Ctrl+D 등으로 입력이 끊겨도 데이터는 저장하고 종료
            System.out.println("입력이 종료되어 데이터를 저장하고 종료합니다.");
            save(true);
        }
    }

    private void load() {
        System.out.println("데이터 폴더: " + dataStore.getDataDirectory());
        List<String> warnings = dataStore.loadAll(broadcastService, donationService, viewerService);
        for (String w : warnings) {
            System.out.println("  [!] " + w);
        }
        System.out.printf("불러오기 완료: 방송 %d건, 후원 %d건, 시청자 %d명%n",
                broadcastService.size(), donationService.size(), viewerService.size());
    }

    /** 현재 데이터를 파일에 저장한다. 실패해도 프로그램은 계속 실행되며 데이터는 메모리에 남아 있다. */
    private void save(boolean verbose) {
        try {
            dataStore.saveAll(broadcastService, donationService, viewerService);
            if (verbose) {
                System.out.println("[저장 완료] " + dataStore.getDataDirectory());
            }
        } catch (IOException e) {
            System.out.println("  [!] 파일 저장에 실패했습니다: " + e.getMessage());
            System.out.println("      (데이터는 메모리에 남아 있으니 문제를 해결한 뒤 '지금 저장하기'를 다시 시도하세요.)");
        }
    }

    /** 하위 메뉴 번호를 입력받는다. 0은 뒤로 가기. */
    private int readSubMenu(int max) {
        return in.readInt("선택 (0=뒤로)> ", 0, max);
    }

    /** 작업을 실행하고, 예상 가능한 예외는 메시지로 보여준 뒤 메뉴로 돌아간다. */
    private void execute(Action action) {
        try {
            action.run();
        } catch (DataNotFoundException | BlacklistedViewerException e) {
            System.out.println("  [!] " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("  [!] 입력값 오류: " + e.getMessage());
        }
    }

    // =====================================================================
    // 1. 방송 기록 관리
    // =====================================================================

    private void broadcastMenu() {
        while (true) {
            System.out.println();
            System.out.println("[방송 기록 관리]");
            System.out.println("  1. 방송 등록");
            System.out.println("  2. 방송 목록 (정렬)");
            System.out.println("  3. 방송 검색 (제목 / 카테고리)");
            System.out.println("  4. 방송 상세 보기 (후원 내역 포함)");
            System.out.println("  5. 방송 수정");
            System.out.println("  6. 방송 삭제");
            System.out.println("  0. 뒤로");
            int menu = readSubMenu(6);
            switch (menu) {
                case 1: execute(this::addBroadcast); break;
                case 2: execute(this::listBroadcasts); break;
                case 3: execute(this::searchBroadcasts); break;
                case 4: execute(this::showBroadcastDetail); break;
                case 5: execute(this::updateBroadcast); break;
                case 6: execute(this::deleteBroadcast); break;
                case 0: return;
                default: break;
            }
        }
    }

    private void addBroadcast() {
        System.out.println(LINE);
        String title = in.readRequired("방송 제목: ");
        Category category = in.choose("카테고리:", Arrays.asList(Category.values()), Category::getDisplayName);
        LocalDate date = in.readDate("방송 날짜", LocalDate.now());
        int minutes = in.readInt("방송 시간(분): ", 1, 24 * 60);
        int viewers = in.readInt("최고 시청자 수: ", 0, 10_000_000);

        Broadcast b = broadcastService.add(title, category, date, minutes, viewers);
        save(false);
        System.out.println("[등록 완료] " + b);
    }

    private void listBroadcasts() {
        if (broadcastService.size() == 0) {
            System.out.println("등록된 방송이 없습니다.");
            return;
        }
        BroadcastService.SortOrder order = in.choose("정렬 기준:",
                Arrays.asList(BroadcastService.SortOrder.values()), BroadcastService.SortOrder::getDisplayName);
        printList("방송 목록 (" + order.getDisplayName() + ")", broadcastService.getAll(order), "등록된 방송이 없습니다.");
    }

    private void searchBroadcasts() {
        List<String> modes = Arrays.asList("제목 키워드로 검색", "카테고리로 검색");
        String mode = in.choose("검색 방법:", modes, s -> s);
        List<Broadcast> result;
        if (mode.equals(modes.get(0))) {
            String keyword = in.readRequired("검색할 키워드: ");
            result = broadcastService.searchByTitle(keyword);
        } else {
            Category category = in.choose("카테고리:", Arrays.asList(Category.values()), Category::getDisplayName);
            result = broadcastService.findByCategory(category);
        }
        printList("검색 결과 " + result.size() + "건", result, "검색 결과가 없습니다.");
    }

    private void showBroadcastDetail() throws DataNotFoundException {
        printRecentBroadcasts();
        int id = in.readInt("방송 ID: ", 1, Integer.MAX_VALUE);
        Broadcast b = broadcastService.findById(id);
        List<Donation> donations = donationService.findByBroadcast(id);

        long sent = 0;
        long settled = 0;
        for (Donation d : donations) {
            sent += d.getAmount();
            settled += d.getSettledAmount();
        }
        System.out.println(LINE);
        System.out.println("제목       : " + b.getTitle());
        System.out.println("카테고리   : " + b.getCategory().getDisplayName());
        System.out.println("날짜       : " + b.getDate());
        System.out.println("방송 시간  : " + b.getDurationText());
        System.out.printf("최고 시청자: %,d명%n", b.getMaxViewers());
        System.out.printf("후원       : %d건 / 보낸 금액 %,d치즈 / 정산 %,d치즈%n", donations.size(), sent, settled);
        printList("이 방송의 후원 내역", donations, "후원 내역이 없습니다.");
    }

    private void updateBroadcast() throws DataNotFoundException {
        printRecentBroadcasts();
        int id = in.readInt("수정할 방송 ID: ", 1, Integer.MAX_VALUE);
        Broadcast b = broadcastService.findById(id);
        System.out.println("현재: " + b);

        String title = in.readOrKeep("방송 제목", b.getTitle());
        Category category = in.chooseOrKeep("카테고리:", Arrays.asList(Category.values()),
                Category::getDisplayName, b.getCategory());
        LocalDate date = in.readDate("방송 날짜", b.getDate());
        int minutes = in.readIntOrDefault("방송 시간(분) [현재: " + b.getDurationMinutes() + "] (엔터=유지): ",
                1, 24 * 60, b.getDurationMinutes());
        int viewers = in.readIntOrDefault("최고 시청자 수 [현재: " + b.getMaxViewers() + "] (엔터=유지): ",
                0, 10_000_000, b.getMaxViewers());

        broadcastService.update(id, title, category, date, minutes, viewers);
        save(false);
        System.out.println("[수정 완료] " + broadcastService.findById(id));
    }

    private void deleteBroadcast() throws DataNotFoundException {
        printRecentBroadcasts();
        int id = in.readInt("삭제할 방송 ID: ", 1, Integer.MAX_VALUE);
        Broadcast b = broadcastService.findById(id);
        int linked = donationService.findByBroadcast(id).size();

        System.out.println("삭제 대상: " + b);
        if (linked > 0) {
            System.out.println("  이 방송에 연결된 후원 " + linked + "건도 함께 삭제됩니다.");
        }
        if (!in.confirm("정말 삭제할까요?")) {
            System.out.println("삭제를 취소했습니다.");
            return;
        }
        int removed = donationService.deleteByBroadcast(id);
        broadcastService.delete(id);
        save(false);
        System.out.println("[삭제 완료] 방송 #" + id + " (연결된 후원 " + removed + "건 함께 삭제)");
    }

    private void printRecentBroadcasts() {
        List<Broadcast> all = broadcastService.getAll(BroadcastService.SortOrder.DATE_DESC);
        if (all.isEmpty()) {
            System.out.println("(등록된 방송이 없습니다)");
            return;
        }
        System.out.println("최근 방송:");
        for (int i = 0; i < Math.min(RECENT_COUNT, all.size()); i++) {
            System.out.println("  " + all.get(i));
        }
    }

    // =====================================================================
    // 2. 후원 관리
    // =====================================================================

    private void donationMenu() {
        while (true) {
            System.out.println();
            System.out.println("[후원(치즈) 관리]");
            System.out.println("  1. 후원 등록");
            System.out.println("  2. 전체 후원 목록");
            System.out.println("  3. 방송별 후원 조회");
            System.out.println("  4. 미션 결과 처리 (성공/실패)");
            System.out.println("  5. 후원 삭제");
            System.out.println("  0. 뒤로");
            int menu = readSubMenu(5);
            switch (menu) {
                case 1: execute(this::addDonation); break;
                case 2: printList("전체 후원 목록", donationService.getAll(), "등록된 후원이 없습니다."); break;
                case 3: execute(this::listDonationsByBroadcast); break;
                case 4: execute(this::processMission); break;
                case 5: execute(this::deleteDonation); break;
                case 0: return;
                default: break;
            }
        }
    }

    private void addDonation() throws DataNotFoundException, BlacklistedViewerException {
        if (broadcastService.size() == 0) {
            System.out.println("먼저 방송을 등록해 주세요. 후원은 방송에 연결되어야 합니다.");
            return;
        }
        printRecentBroadcasts();
        int broadcastId = in.readInt("후원이 들어온 방송 ID: ", 1, Integer.MAX_VALUE);
        Broadcast b = broadcastService.findById(broadcastId);

        String donor = in.readRequired("후원자 닉네임: ");
        if (viewerService.isBlacklisted(donor)) {
            throw new BlacklistedViewerException(donor);  // 나머지 정보를 입력하기 전에 미리 차단
        }
        DonationType type = in.choose("후원 종류:", Arrays.asList(DonationType.values()), DonationType::getDisplayName);
        int amount = in.readInt("후원 금액(치즈): ", 1, 10_000_000);
        LocalTime time = in.readTime("후원 시각", LocalTime.now().withSecond(0).withNano(0));
        LocalDateTime dateTime = b.getDate().atTime(time);

        Donation d;
        switch (type) {
            case CHAT:
                String message = in.readLine("후원 메시지(없으면 엔터): ");
                d = donationService.registerChat(broadcastId, donor, amount, dateTime, message);
                break;
            case VIDEO:
                String url = in.readRequired("영상 주소: ");
                int seconds = in.readInt("재생 시간(초): ", 1, 60 * 60);
                d = donationService.registerVideo(broadcastId, donor, amount, dateTime, url, seconds);
                break;
            case MISSION:
                String mission = in.readRequired("미션 내용: ");
                d = donationService.registerMission(broadcastId, donor, amount, dateTime, mission);
                break;
            default:
                return;
        }
        save(false);
        System.out.println("[등록 완료] " + d);
    }

    private void listDonationsByBroadcast() throws DataNotFoundException {
        printRecentBroadcasts();
        int id = in.readInt("방송 ID: ", 1, Integer.MAX_VALUE);
        Broadcast b = broadcastService.findById(id);
        printList("'" + b.getTitle() + "' 후원 내역", donationService.findByBroadcast(id), "후원 내역이 없습니다.");
    }

    private void processMission() throws DataNotFoundException {
        List<MissionDonation> pending = donationService.getPendingMissions();
        if (pending.isEmpty()) {
            System.out.println("진행중인 미션이 없습니다.");
            return;
        }
        printList("진행중인 미션", pending, "");
        int id = in.readInt("처리할 후원 ID: ", 1, Integer.MAX_VALUE);
        MissionStatus status = in.choose("결과:",
                Arrays.asList(MissionStatus.SUCCESS, MissionStatus.FAILED), MissionStatus::getDisplayName);
        MissionDonation updated = donationService.updateMissionStatus(id, status);
        save(false);
        System.out.println("[처리 완료] " + updated);
        if (status == MissionStatus.FAILED) {
            System.out.println("  미션 실패: 이 후원은 정산 금액에서 제외됩니다.");
        }
    }

    private void deleteDonation() throws DataNotFoundException {
        int id = in.readInt("삭제할 후원 ID: ", 1, Integer.MAX_VALUE);
        Donation d = donationService.findById(id);
        System.out.println("삭제 대상: " + d);
        if (!in.confirm("정말 삭제할까요?")) {
            System.out.println("삭제를 취소했습니다.");
            return;
        }
        donationService.delete(id);
        save(false);
        System.out.println("[삭제 완료] 후원 #" + id);
    }

    // =====================================================================
    // 3. 시청자 관리
    // =====================================================================

    private void viewerMenu() {
        while (true) {
            System.out.println();
            System.out.println("[시청자 관리]");
            System.out.println("  1. 시청자 목록");
            System.out.println("  2. 시청자 검색 / 상세 보기");
            System.out.println("  3. 시청자 등록");
            System.out.println("  4. 메모 수정");
            System.out.println("  5. 블랙리스트 추가 / 해제");
            System.out.println("  6. 블랙리스트 보기");
            System.out.println("  7. 시청자 삭제");
            System.out.println("  0. 뒤로");
            int menu = readSubMenu(7);
            switch (menu) {
                case 1: printList("시청자 목록", viewerService.getAll(), "등록된 시청자가 없습니다."); break;
                case 2: execute(this::searchViewer); break;
                case 3: execute(this::registerViewer); break;
                case 4: execute(this::updateViewerMemo); break;
                case 5: execute(this::toggleBlacklist); break;
                case 6: printList("블랙리스트", viewerService.getBlacklist(), "블랙리스트가 비어 있습니다."); break;
                case 7: execute(this::deleteViewer); break;
                case 0: return;
                default: break;
            }
        }
    }

    private void searchViewer() throws DataNotFoundException {
        String keyword = in.readRequired("닉네임(일부만 입력해도 됨): ");
        List<Viewer> found = viewerService.search(keyword);
        if (found.isEmpty()) {
            throw new DataNotFoundException("'" + keyword + "'가 포함된 시청자가 없습니다.");
        }
        Viewer v = found.size() == 1 ? found.get(0) : in.choose("검색 결과 " + found.size() + "명:", found, Viewer::getNickname);

        List<Donation> donations = donationService.findByDonor(v.getNickname());
        long sent = 0;
        long settled = 0;
        for (Donation d : donations) {
            sent += d.getAmount();
            settled += d.getSettledAmount();
        }
        System.out.println(LINE);
        System.out.println(v);
        System.out.printf("후원 %d건 / 보낸 금액 %,d치즈 / 정산 %,d치즈%n", donations.size(), sent, settled);
        printList("후원 내역", donations, "후원 내역이 없습니다.");
    }

    private void registerViewer() {
        String nickname = in.readRequired("닉네임: ");
        String memo = in.readLine("메모(없으면 엔터): ");
        Viewer v = viewerService.register(nickname, memo);
        save(false);
        System.out.println("[등록 완료] " + v);
    }

    private void updateViewerMemo() throws DataNotFoundException {
        String nickname = in.readRequired("닉네임: ");
        Viewer v = viewerService.find(nickname);
        String memo = in.readOrKeep("새 메모", v.getMemo().isEmpty() ? "(없음)" : v.getMemo());
        viewerService.updateMemo(nickname, memo.equals("(없음)") ? "" : memo);
        save(false);
        System.out.println("[수정 완료] " + viewerService.find(nickname));
    }

    private void toggleBlacklist() throws DataNotFoundException {
        String nickname = in.readRequired("닉네임: ");
        Viewer v = viewerService.find(nickname);
        boolean next = !v.isBlacklisted();
        String action = next ? "블랙리스트에 추가" : "블랙리스트에서 해제";
        if (!in.confirm("'" + v.getNickname() + "' 님을 " + action + "할까요?")) {
            System.out.println("취소했습니다.");
            return;
        }
        viewerService.setBlacklisted(nickname, next);
        save(false);
        System.out.println("[완료] '" + v.getNickname() + "' 님을 " + action + "했습니다.");
    }

    private void deleteViewer() throws DataNotFoundException {
        String nickname = in.readRequired("삭제할 닉네임: ");
        Viewer v = viewerService.find(nickname);
        int count = donationService.findByDonor(v.getNickname()).size();
        if (count > 0) {
            System.out.println("  [!] 후원 기록이 " + count + "건 있는 시청자는 삭제할 수 없습니다. "
                    + "(대신 블랙리스트 기능을 사용하세요)");
            return;
        }
        if (!in.confirm("'" + v.getNickname() + "' 님을 삭제할까요?")) {
            System.out.println("취소했습니다.");
            return;
        }
        viewerService.delete(nickname);
        save(false);
        System.out.println("[삭제 완료] " + v.getNickname());
    }

    // =====================================================================
    // 4. 통계 / 분석
    // =====================================================================

    private void statisticsMenu() {
        while (true) {
            System.out.println();
            System.out.println("[통계 / 분석]");
            System.out.println("  1. 월별 요약");
            System.out.println("  2. 카테고리별 통계");
            System.out.println("  3. 후원자 랭킹 TOP 10");
            System.out.println("  4. 후원 많이 받은 방송 TOP 5");
            System.out.println("  5. 후원 종류별 합계");
            System.out.println("  0. 뒤로");
            int menu = readSubMenu(5);
            switch (menu) {
                case 1: execute(this::showMonthlySummary); break;
                case 2: showCategoryStats(); break;
                case 3: showRanking("후원자 랭킹 TOP 10 (정산 금액 기준)", statisticsService.getTopDonors(10)); break;
                case 4: showRanking("후원 많이 받은 방송 TOP 5 (정산 금액 기준)",
                        statisticsService.getTopBroadcastsByDonation(5)); break;
                case 5: showTypeTotals(); break;
                case 0: return;
                default: break;
            }
        }
    }

    private void showMonthlySummary() {
        List<YearMonth> months = statisticsService.getAvailableMonths();
        if (months.isEmpty()) {
            System.out.println("데이터가 없습니다.");
            return;
        }
        YearMonth month = in.choose("조회할 달:", months, m -> m.getYear() + "년 " + m.getMonthValue() + "월");
        StatisticsService.MonthlySummary s = statisticsService.getMonthlySummary(month);

        System.out.println(LINE);
        System.out.printf("[%d년 %d월 요약]%n", month.getYear(), month.getMonthValue());
        System.out.printf("방송 횟수         : %d회%n", s.getBroadcastCount());
        System.out.printf("총 방송 시간      : %s%n", Broadcast.formatMinutes(s.getTotalMinutes()));
        System.out.printf("평균 최고 시청자  : %,.0f명%n", s.getAverageMaxViewers());
        if (s.getBestBroadcast() != null) {
            System.out.println("최고 기록 방송    : " + s.getBestBroadcast());
        }
        StatisticsService.Tally t = s.getDonations();
        System.out.printf("후원              : %d건 / 보낸 금액 %,d치즈 / 정산 %,d치즈%n",
                t.getCount(), t.getSentAmount(), t.getSettledAmount());
    }

    private void showCategoryStats() {
        List<StatisticsService.CategoryStat> stats = statisticsService.getCategoryStats();
        if (stats.isEmpty()) {
            System.out.println("방송 데이터가 없습니다.");
            return;
        }
        System.out.println(LINE);
        System.out.println("[카테고리별 통계 (평균 최고 시청자 많은순)]");
        for (StatisticsService.CategoryStat c : stats) {
            System.out.printf("%-10s | 평균 %,7.0f명 | 방송 %d회 | 총 %s | 최고 %,d명%n",
                    c.getCategory().getDisplayName(), c.getAverageMaxViewers(),
                    c.getBroadcastCount(), Broadcast.formatMinutes(c.getTotalMinutes()), c.getPeakViewers());
        }
    }

    private void showRanking(String title, List<StatisticsService.RankEntry> ranking) {
        System.out.println(LINE);
        System.out.println("[" + title + "]");
        if (ranking.isEmpty()) {
            System.out.println("후원 데이터가 없습니다.");
            return;
        }
        int rank = 1;
        for (StatisticsService.RankEntry r : ranking) {
            StatisticsService.Tally t = r.getTally();
            System.out.printf("%2d위  %-20s 정산 %,9d치즈  (보낸 금액 %,d치즈, %d건)%n",
                    rank++, r.getLabel(), t.getSettledAmount(), t.getSentAmount(), t.getCount());
        }
    }

    private void showTypeTotals() {
        Map<DonationType, StatisticsService.Tally> totals = statisticsService.getTypeTotals();
        System.out.println(LINE);
        System.out.println("[후원 종류별 합계]");
        long sentSum = 0;
        long settledSum = 0;
        for (Map.Entry<DonationType, StatisticsService.Tally> e : totals.entrySet()) {
            StatisticsService.Tally t = e.getValue();
            System.out.printf("%-6s : %3d건 / 보낸 금액 %,9d치즈 / 정산 %,9d치즈%n",
                    e.getKey().getDisplayName(), t.getCount(), t.getSentAmount(), t.getSettledAmount());
            sentSum += t.getSentAmount();
            settledSum += t.getSettledAmount();
        }
        System.out.printf("합계   : 보낸 금액 %,d치즈 / 정산 %,d치즈 (미정산·환불 %,d치즈)%n",
                sentSum, settledSum, sentSum - settledSum);
    }

    // =====================================================================
    // 출력 도우미
    // =====================================================================

    private static <T> void printList(String title, List<T> items, String emptyMessage) {
        System.out.println(LINE);
        System.out.println("[" + title + "]");
        if (items.isEmpty()) {
            System.out.println(emptyMessage);
            return;
        }
        for (T item : items) {
            System.out.println("  " + item);
        }
        System.out.println("  (총 " + items.size() + "건)");
    }
}
