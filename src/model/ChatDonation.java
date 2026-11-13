package model;

import java.time.LocalDateTime;

/**
 * 채팅 메시지와 함께 보내는 일반 후원.
 */
public class ChatDonation extends Donation {

    private final String message;

    public ChatDonation(int id, int broadcastId, String donor, int amount,
                        LocalDateTime dateTime, String message) {
        super(id, broadcastId, donor, amount, dateTime);
        this.message = message == null ? "" : message.trim();
    }

    public String getMessage() {
        return message;
    }

    @Override
    public DonationType getType() {
        return DonationType.CHAT;
    }

    @Override
    public String describeDetail() {
        return message.isEmpty() ? "(메시지 없음)" : "\"" + message + "\"";
    }

    @Override
    protected String[] detailFields() {
        return new String[]{message, ""};
    }
}
