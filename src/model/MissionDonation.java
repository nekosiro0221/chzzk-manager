package model;

import java.time.LocalDateTime;

/**
 * 스트리머에게 미션을 걸고 보내는 미션 후원.
 * 미션에 성공해야만 정산되고, 실패하면 환불되며, 진행중인 동안은 정산되지 않는다.
 */
public class MissionDonation extends Donation {

    private final String mission;
    private MissionStatus status;

    public MissionDonation(int id, int broadcastId, String donor, int amount,
                           LocalDateTime dateTime, String mission, MissionStatus status) {
        super(id, broadcastId, donor, amount, dateTime);
        if (mission == null || mission.trim().isEmpty()) {
            throw new IllegalArgumentException("미션 내용은 비어 있을 수 없습니다.");
        }
        this.mission = mission.trim();
        setStatus(status);
    }

    public String getMission() {
        return mission;
    }

    public MissionStatus getStatus() {
        return status;
    }

    public void setStatus(MissionStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("미션 상태를 지정해야 합니다.");
        }
        this.status = status;
    }

    /** 미션 성공 시에만 금액 전체가 정산된다. */
    @Override
    public int getSettledAmount() {
        return status == MissionStatus.SUCCESS ? getAmount() : 0;
    }

    @Override
    public DonationType getType() {
        return DonationType.MISSION;
    }

    @Override
    public String describeDetail() {
        return "미션 \"" + mission + "\" [" + status.getDisplayName() + "]";
    }

    @Override
    protected String[] detailFields() {
        return new String[]{mission, status.name()};
    }
}
