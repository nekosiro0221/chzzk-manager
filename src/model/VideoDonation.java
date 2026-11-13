package model;

import java.time.LocalDateTime;

/**
 * 영상을 재생해 달라고 요청하는 영상 후원.
 */
public class VideoDonation extends Donation {

    private final String videoUrl;
    private final int videoSeconds;

    public VideoDonation(int id, int broadcastId, String donor, int amount,
                         LocalDateTime dateTime, String videoUrl, int videoSeconds) {
        super(id, broadcastId, donor, amount, dateTime);
        if (videoUrl == null || videoUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("영상 주소는 비어 있을 수 없습니다.");
        }
        if (videoSeconds <= 0) {
            throw new IllegalArgumentException("영상 길이는 1초 이상이어야 합니다.");
        }
        this.videoUrl = videoUrl.trim();
        this.videoSeconds = videoSeconds;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public int getVideoSeconds() {
        return videoSeconds;
    }

    @Override
    public DonationType getType() {
        return DonationType.VIDEO;
    }

    @Override
    public String describeDetail() {
        return String.format("영상 %s (%d분 %02d초)", videoUrl, videoSeconds / 60, videoSeconds % 60);
    }

    @Override
    protected String[] detailFields() {
        return new String[]{videoUrl, String.valueOf(videoSeconds)};
    }
}
