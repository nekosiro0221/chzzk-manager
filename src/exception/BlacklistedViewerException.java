package exception;

/**
 * 블랙리스트에 등록된 시청자의 후원을 등록하려 할 때 발생하는 예외.
 */
public class BlacklistedViewerException extends Exception {
    public BlacklistedViewerException(String nickname) {
        super("'" + nickname + "' 님은 블랙리스트에 등록된 시청자라 후원을 등록할 수 없습니다.");
    }
}
