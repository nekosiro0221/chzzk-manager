package service;

import exception.BlacklistedViewerException;
import exception.DataNotFoundException;
import model.ChatDonation;
import model.Donation;
import model.MissionDonation;
import model.MissionStatus;
import model.VideoDonation;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * 후원(치즈) 등록·조회·삭제와 미션 상태 변경을 담당한다.
 * 후원은 반드시 존재하는 방송에 연결되어야 하며, 블랙리스트 시청자의 후원은 거부한다.
 */
public class DonationService {

    private final List<Donation> donations = new ArrayList<>();
    private final BroadcastService broadcastService;
    private final ViewerService viewerService;

    public DonationService(BroadcastService broadcastService, ViewerService viewerService) {
        this.broadcastService = broadcastService;
        this.viewerService = viewerService;
    }

    /**
     * 파일에서 읽은 후원 목록으로 교체한다.
     * ID가 중복되거나 존재하지 않는 방송을 가리키는 후원은 건너뛴다.
     * 후원자가 시청자 목록에 없으면 자동으로 등록한다.
     *
     * @return 건너뛴 항목에 대한 경고 메시지
     */
    public List<String> loadAll(Collection<Donation> loaded) {
        List<String> warnings = new ArrayList<>();
        Set<Integer> ids = new HashSet<>();
        donations.clear();
        for (Donation d : loaded) {
            if (!ids.add(d.getId())) {
                warnings.add("후원 ID " + d.getId() + "가 중복되어 건너뛰었습니다.");
                continue;
            }
            if (!broadcastService.exists(d.getBroadcastId())) {
                warnings.add("후원 ID " + d.getId() + "의 방송(#" + d.getBroadcastId() + ")이 없어 건너뛰었습니다.");
                continue;
            }
            viewerService.getOrCreate(d.getDonor(), d.getDateTime().toLocalDate());
            donations.add(d);
        }
        return warnings;
    }

    public Donation registerChat(int broadcastId, String donor, int amount, LocalDateTime dateTime,
                                 String message) throws DataNotFoundException, BlacklistedViewerException {
        validate(broadcastId, donor);
        return register(new ChatDonation(nextId(), broadcastId, donor, amount, dateTime, message));
    }

    public Donation registerVideo(int broadcastId, String donor, int amount, LocalDateTime dateTime,
                                  String videoUrl, int videoSeconds)
            throws DataNotFoundException, BlacklistedViewerException {
        validate(broadcastId, donor);
        return register(new VideoDonation(nextId(), broadcastId, donor, amount, dateTime, videoUrl, videoSeconds));
    }

    public Donation registerMission(int broadcastId, String donor, int amount, LocalDateTime dateTime,
                                    String mission) throws DataNotFoundException, BlacklistedViewerException {
        validate(broadcastId, donor);
        return register(new MissionDonation(nextId(), broadcastId, donor, amount, dateTime,
                mission, MissionStatus.PENDING));
    }

    private void validate(int broadcastId, String donor) throws DataNotFoundException, BlacklistedViewerException {
        broadcastService.findById(broadcastId); // 없으면 DataNotFoundException
        if (donor == null || donor.trim().isEmpty()) {
            throw new IllegalArgumentException("후원자 닉네임은 비어 있을 수 없습니다.");
        }
        if (viewerService.isBlacklisted(donor)) {
            throw new BlacklistedViewerException(donor.trim());
        }
    }

    private Donation register(Donation d) {
        viewerService.getOrCreate(d.getDonor(), d.getDateTime().toLocalDate());
        donations.add(d);
        return d;
    }

    /** 미션 후원의 상태(진행중/성공/실패)를 변경한다. */
    public MissionDonation updateMissionStatus(int donationId, MissionStatus status) throws DataNotFoundException {
        Donation d = findById(donationId);
        if (!(d instanceof MissionDonation)) {
            throw new IllegalArgumentException("후원 #" + donationId + "는 미션 후원이 아닙니다.");
        }
        MissionDonation mission = (MissionDonation) d;
        mission.setStatus(status);
        return mission;
    }

    public void delete(int donationId) throws DataNotFoundException {
        donations.remove(findById(donationId));
    }

    /** 특정 방송에 연결된 후원을 모두 삭제하고 삭제한 개수를 반환한다. */
    public int deleteByBroadcast(int broadcastId) {
        int count = 0;
        Iterator<Donation> it = donations.iterator();
        while (it.hasNext()) {
            if (it.next().getBroadcastId() == broadcastId) {
                it.remove();
                count++;
            }
        }
        return count;
    }

    public Donation findById(int donationId) throws DataNotFoundException {
        for (Donation d : donations) {
            if (d.getId() == donationId) {
                return d;
            }
        }
        throw new DataNotFoundException("ID " + donationId + "인 후원이 없습니다.");
    }

    /** 일시순 전체 목록. */
    public List<Donation> getAll() {
        List<Donation> copy = new ArrayList<>(donations);
        Collections.sort(copy);
        return Collections.unmodifiableList(copy);
    }

    public List<Donation> findByBroadcast(int broadcastId) {
        List<Donation> result = new ArrayList<>();
        for (Donation d : getAll()) {
            if (d.getBroadcastId() == broadcastId) {
                result.add(d);
            }
        }
        return result;
    }

    public List<Donation> findByDonor(String nickname) {
        String key = nickname.trim();
        List<Donation> result = new ArrayList<>();
        for (Donation d : getAll()) {
            if (d.getDonor().equals(key)) {
                result.add(d);
            }
        }
        return result;
    }

    public List<MissionDonation> getPendingMissions() {
        List<MissionDonation> result = new ArrayList<>();
        for (Donation d : getAll()) {
            if (d instanceof MissionDonation && ((MissionDonation) d).getStatus() == MissionStatus.PENDING) {
                result.add((MissionDonation) d);
            }
        }
        return result;
    }

    public int size() {
        return donations.size();
    }

    private int nextId() {
        int max = 0;
        for (Donation d : donations) {
            max = Math.max(max, d.getId());
        }
        return max + 1;
    }
}
