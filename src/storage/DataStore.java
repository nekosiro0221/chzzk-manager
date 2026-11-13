package storage;

import model.Broadcast;
import model.Donation;
import model.Viewer;
import service.BroadcastService;
import service.DonationService;
import service.ViewerService;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 방송/후원/시청자 데이터를 data 폴더의 CSV 파일 3개에 저장하고 불러온다.
 */
public class DataStore {

    private final CsvFileStore<Broadcast> broadcastStore;
    private final CsvFileStore<Donation> donationStore;
    private final CsvFileStore<Viewer> viewerStore;

    public DataStore(Path dataDir) {
        this.broadcastStore = new CsvFileStore<>(dataDir.resolve("broadcasts.csv"),
                Broadcast.CSV_HEADER, Broadcast::fromCsv);
        this.donationStore = new CsvFileStore<>(dataDir.resolve("donations.csv"),
                Donation.CSV_HEADER, Donation::fromCsv);
        this.viewerStore = new CsvFileStore<>(dataDir.resolve("viewers.csv"),
                Viewer.CSV_HEADER, Viewer::fromCsv);
    }

    /**
     * 파일 3개를 읽어 각 서비스에 채운다.
     * 순서가 중요하다: 시청자 → 방송 → 후원 (후원은 방송 존재 여부를 검사하고 시청자를 자동 등록하므로).
     * 파일 하나를 읽지 못해도 나머지는 계속 읽는다.
     *
     * @return 사용자에게 보여줄 경고 메시지 목록
     */
    public List<String> loadAll(BroadcastService broadcasts, DonationService donations, ViewerService viewers) {
        List<String> messages = new ArrayList<>();
        try {
            LoadResult<Viewer> r = viewerStore.load();
            messages.addAll(r.getWarnings());
            messages.addAll(viewers.loadAll(r.getItems()));
        } catch (IOException e) {
            messages.add("시청자 파일을 읽지 못했습니다: " + e.getMessage());
        }
        try {
            LoadResult<Broadcast> r = broadcastStore.load();
            messages.addAll(r.getWarnings());
            messages.addAll(broadcasts.loadAll(r.getItems()));
        } catch (IOException e) {
            messages.add("방송 파일을 읽지 못했습니다: " + e.getMessage());
        }
        try {
            LoadResult<Donation> r = donationStore.load();
            messages.addAll(r.getWarnings());
            messages.addAll(donations.loadAll(r.getItems()));
        } catch (IOException e) {
            messages.add("후원 파일을 읽지 못했습니다: " + e.getMessage());
        }
        return messages;
    }

    /** 세 서비스의 현재 데이터를 모두 파일에 저장한다. */
    public void saveAll(BroadcastService broadcasts, DonationService donations, ViewerService viewers)
            throws IOException {
        broadcastStore.save(broadcasts.getAll());
        donationStore.save(donations.getAll());
        viewerStore.save(viewers.getAll());
    }

    public Path getDataDirectory() {
        return broadcastStore.getPath().toAbsolutePath().getParent();
    }
}
