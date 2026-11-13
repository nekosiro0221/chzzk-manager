package storage;

/**
 * CSV 파일 한 줄로 저장될 수 있는 객체가 구현하는 인터페이스.
 */
public interface CsvSerializable {
    /** 객체를 CSV 한 줄을 구성하는 필드 배열로 변환한다. */
    String[] toCsvFields();
}
