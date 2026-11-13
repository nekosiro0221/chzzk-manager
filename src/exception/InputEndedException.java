package exception;

/**
 * 표준 입력이 끝났을 때(EOF, Ctrl+Z/Ctrl+D) 발생하는 예외.
 * 프로그램은 이 예외를 받으면 데이터를 저장하고 안전하게 종료한다.
 */
public class InputEndedException extends RuntimeException {
    public InputEndedException() {
        super("입력이 종료되었습니다.");
    }
}
