package service;

import exception.DataNotFoundException;
import model.Broadcast;
import model.Category;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 방송 기록의 등록·조회·수정·삭제·검색을 담당한다.
 */
public class BroadcastService {

    /** 방송 목록 정렬 기준. */
    public enum SortOrder {
        DATE_DESC("최신순", Comparator.<Broadcast>reverseOrder()),
        DATE_ASC("오래된순", Comparator.<Broadcast>naturalOrder()),
        VIEWERS_DESC("최고 시청자 많은순",
                Comparator.comparingInt(Broadcast::getMaxViewers).reversed()
                        .thenComparing(Comparator.reverseOrder())),
        DURATION_DESC("방송 시간 긴순",
                Comparator.comparingInt(Broadcast::getDurationMinutes).reversed()
                        .thenComparing(Comparator.reverseOrder()));

        private final String displayName;
        private final Comparator<Broadcast> comparator;

        SortOrder(String displayName, Comparator<Broadcast> comparator) {
            this.displayName = displayName;
            this.comparator = comparator;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private final List<Broadcast> broadcasts = new ArrayList<>();

    /**
     * 파일에서 읽은 방송 목록으로 교체한다. ID가 중복된 방송은 건너뛴다.
     *
     * @return 건너뛴 항목에 대한 경고 메시지
     */
    public List<String> loadAll(Collection<Broadcast> loaded) {
        List<String> warnings = new ArrayList<>();
        Set<Integer> ids = new HashSet<>();
        broadcasts.clear();
        for (Broadcast b : loaded) {
            if (!ids.add(b.getId())) {
                warnings.add("방송 ID " + b.getId() + "가 중복되어 건너뛰었습니다.");
                continue;
            }
            broadcasts.add(b);
        }
        return warnings;
    }

    public Broadcast add(String title, Category category, LocalDate date, int durationMinutes, int maxViewers) {
        Broadcast b = new Broadcast(nextId(), title, category, date, durationMinutes, maxViewers);
        broadcasts.add(b);
        return b;
    }

    /**
     * 방송 정보를 수정한다. 모든 값이 유효할 때만 한꺼번에 반영한다.
     */
    public Broadcast update(int id, String title, Category category, LocalDate date,
                            int durationMinutes, int maxViewers) throws DataNotFoundException {
        Broadcast target = findById(id);
        // 임시 객체로 먼저 검증 → 잘못된 값이면 여기서 예외가 나고 원본은 바뀌지 않음
        new Broadcast(id, title, category, date, durationMinutes, maxViewers);
        target.setTitle(title);
        target.setCategory(category);
        target.setDate(date);
        target.setDurationMinutes(durationMinutes);
        target.setMaxViewers(maxViewers);
        return target;
    }

    public void delete(int id) throws DataNotFoundException {
        Broadcast target = findById(id);
        broadcasts.remove(target);
    }

    public Broadcast findById(int id) throws DataNotFoundException {
        for (Broadcast b : broadcasts) {
            if (b.getId() == id) {
                return b;
            }
        }
        throw new DataNotFoundException("ID " + id + "인 방송이 없습니다.");
    }

    public boolean exists(int id) {
        for (Broadcast b : broadcasts) {
            if (b.getId() == id) {
                return true;
            }
        }
        return false;
    }

    /** 날짜순(오래된순) 전체 목록. */
    public List<Broadcast> getAll() {
        return getAll(SortOrder.DATE_ASC);
    }

    public List<Broadcast> getAll(SortOrder order) {
        List<Broadcast> copy = new ArrayList<>(broadcasts);
        copy.sort(order.comparator);
        return Collections.unmodifiableList(copy);
    }

    /** 제목에 키워드가 포함된 방송 (대소문자 무시). */
    public List<Broadcast> searchByTitle(String keyword) {
        String k = keyword.trim().toLowerCase();
        List<Broadcast> result = new ArrayList<>();
        for (Broadcast b : getAll(SortOrder.DATE_DESC)) {
            if (b.getTitle().toLowerCase().contains(k)) {
                result.add(b);
            }
        }
        return result;
    }

    public List<Broadcast> findByCategory(Category category) {
        List<Broadcast> result = new ArrayList<>();
        for (Broadcast b : getAll(SortOrder.DATE_DESC)) {
            if (b.getCategory() == category) {
                result.add(b);
            }
        }
        return result;
    }

    public int size() {
        return broadcasts.size();
    }

    private int nextId() {
        int max = 0;
        for (Broadcast b : broadcasts) {
            max = Math.max(max, b.getId());
        }
        return max + 1;
    }
}
