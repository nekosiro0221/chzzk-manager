package app;

import service.BroadcastService;
import service.DonationService;
import service.StatisticsService;
import service.ViewerService;
import storage.DataStore;
import ui.ConsoleUI;
import ui.InputReader;

import java.io.Console;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 프로그램 시작점. 객체들을 생성하고 연결한 뒤 콘솔 화면을 실행한다.
 *
 * 사용법: java -cp out app.Main [데이터 폴더 경로]   (기본값: data)
 */
public class Main {

    public static void main(String[] args) {
        Path dataDir = Paths.get(args.length > 0 ? args[0] : "data");

        BroadcastService broadcastService = new BroadcastService();
        ViewerService viewerService = new ViewerService();
        DonationService donationService = new DonationService(broadcastService, viewerService);
        StatisticsService statisticsService = new StatisticsService(broadcastService, donationService);
        DataStore dataStore = new DataStore(dataDir);
        InputReader inputReader = new InputReader(System.in, detectInputCharset());

        new ConsoleUI(broadcastService, donationService, viewerService,
                statisticsService, dataStore, inputReader).run();
    }

    /**
     * 콘솔 입력 문자 인코딩을 결정한다.
     * Windows 한글 콘솔(CP949)과 macOS/Linux/IDE(UTF-8) 모두에서 한글 입력이 깨지지 않도록 한다.
     */
    private static Charset detectInputCharset() {
        String stdin = System.getProperty("stdin.encoding");
        if (stdin != null) {
            try {
                return Charset.forName(stdin);
            } catch (IllegalArgumentException ignored) {
                // 알 수 없는 인코딩이면 다음 방법 사용
            }
        }
        Console console = System.console();
        if (console != null) {
            return console.charset();
        }
        return Charset.defaultCharset();
    }
}
