package exception;

/**
 * 요청한 ID/닉네임에 해당하는 데이터가 없을 때 발생하는 예외.
 */
public class DataNotFoundException extends Exception {
    public DataNotFoundException(String message) {
        super(message);
    }
}
