package service;

import exception.DataNotFoundException;
import model.Viewer;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 시청자 등록·검색·메모·블랙리스트 관리를 담당한다.
 * 닉네임을 키로 하는 HashMap으로 빠르게 조회한다.
 */
public class ViewerService {

    private final Map<String, Viewer> viewers = new HashMap<>();

    /**
     * 파일에서 읽은 시청자 목록으로 교체한다. 닉네임이 중복되면 건너뛴다.
     *
     * @return 건너뛴 항목에 대한 경고 메시지
     */
    public List<String> loadAll(Collection<Viewer> loaded) {
        List<String> warnings = new ArrayList<>();
        viewers.clear();
        for (Viewer v : loaded) {
            if (viewers.containsKey(v.getNickname())) {
                warnings.add("시청자 '" + v.getNickname() + "'가 중복되어 건너뛰었습니다.");
                continue;
            }
            viewers.put(v.getNickname(), v);
        }
        return warnings;
    }

    /** 새 시청자를 등록한다. 이미 있는 닉네임이면 예외. */
    public Viewer register(String nickname, String memo) {
        String key = normalize(nickname);
        if (viewers.containsKey(key)) {
            throw new IllegalArgumentException("'" + key + "' 시청자는 이미 등록되어 있습니다.");
        }
        Viewer v = new Viewer(key, LocalDate.now(), false, memo);
        viewers.put(key, v);
        return v;
    }

    /** 시청자가 없으면 새로 만들어서 반환한다 (후원 등록 시 자동 등록용). */
    public Viewer getOrCreate(String nickname, LocalDate firstSeen) {
        String key = normalize(nickname);
        Viewer existing = viewers.get(key);
        if (existing != null) {
            return existing;
        }
        Viewer v = new Viewer(key, firstSeen, false, "");
        viewers.put(key, v);
        return v;
    }

    public Viewer find(String nickname) throws DataNotFoundException {
        Viewer v = viewers.get(normalize(nickname));
        if (v == null) {
            throw new DataNotFoundException("'" + nickname.trim() + "' 시청자를 찾을 수 없습니다.");
        }
        return v;
    }

    public boolean isBlacklisted(String nickname) {
        Viewer v = viewers.get(normalize(nickname));
        return v != null && v.isBlacklisted();
    }

    public void setBlacklisted(String nickname, boolean blacklisted) throws DataNotFoundException {
        find(nickname).setBlacklisted(blacklisted);
    }

    public void updateMemo(String nickname, String memo) throws DataNotFoundException {
        find(nickname).setMemo(memo);
    }

    public void delete(String nickname) throws DataNotFoundException {
        Viewer v = find(nickname);
        viewers.remove(v.getNickname());
    }

    /** 닉네임 순으로 정렬된 전체 목록. */
    public List<Viewer> getAll() {
        List<Viewer> list = new ArrayList<>(viewers.values());
        list.sort(Comparator.comparing(Viewer::getNickname));
        return list;
    }

    /** 닉네임에 키워드가 포함된 시청자 (대소문자 무시). */
    public List<Viewer> search(String keyword) {
        String k = keyword.trim().toLowerCase();
        List<Viewer> result = new ArrayList<>();
        for (Viewer v : getAll()) {
            if (v.getNickname().toLowerCase().contains(k)) {
                result.add(v);
            }
        }
        return result;
    }

    public List<Viewer> getBlacklist() {
        List<Viewer> result = new ArrayList<>();
        for (Viewer v : getAll()) {
            if (v.isBlacklisted()) {
                result.add(v);
            }
        }
        return result;
    }

    public int size() {
        return viewers.size();
    }

    private static String normalize(String nickname) {
        if (nickname == null || nickname.trim().isEmpty()) {
            throw new IllegalArgumentException("닉네임은 비어 있을 수 없습니다.");
        }
        return nickname.trim();
    }
}
