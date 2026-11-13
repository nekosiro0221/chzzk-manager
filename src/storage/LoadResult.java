package storage;

import java.util.Collections;
import java.util.List;

/**
 * CSV 파일 로드 결과. 정상적으로 읽은 객체 목록과 건너뛴 줄에 대한 경고 메시지를 함께 담는다.
 *
 * @param <T> 로드한 객체 타입
 */
public class LoadResult<T> {
    private final List<T> items;
    private final List<String> warnings;

    public LoadResult(List<T> items, List<String> warnings) {
        this.items = items;
        this.warnings = warnings;
    }

    public List<T> getItems() {
        return Collections.unmodifiableList(items);
    }

    public List<String> getWarnings() {
        return Collections.unmodifiableList(warnings);
    }
}
