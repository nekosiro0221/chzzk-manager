package exception;

/**
 * CSV 파일의 한 줄이 올바른 형식이 아닐 때 발생하는 예외.
 */
public class CsvFormatException extends Exception {
    public CsvFormatException(String message) {
        super(message);
    }
}
