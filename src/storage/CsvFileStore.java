package storage;

import exception.CsvFormatException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 한 종류의 객체를 CSV 파일 하나에 저장하고 불러오는 제네릭 저장소.
 *
 * @param <T> 저장할 객체 타입 (CsvSerializable 구현체)
 */
public class CsvFileStore<T extends CsvSerializable> {

    /** CSV 필드 배열을 객체로 변환하는 함수형 인터페이스. */
    @FunctionalInterface
    public interface CsvParser<T> {
        T parse(String[] fields) throws CsvFormatException;
    }

    private final Path path;
    private final String header;
    private final CsvParser<T> parser;

    public CsvFileStore(Path path, String header, CsvParser<T> parser) {
        this.path = path;
        this.header = header;
        this.parser = parser;
    }

    public Path getPath() {
        return path;
    }

    /**
     * 파일에서 객체 목록을 읽는다.
     * 파일이 없으면 빈 파일을 새로 만들고, 형식이 잘못된 줄은 건너뛰고 경고로 남긴다.
     */
    public LoadResult<T> load() throws IOException {
        List<T> items = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        String fileName = path.getFileName().toString();

        if (!Files.exists(path)) {
            save(Collections.emptyList());
            warnings.add(fileName + " 파일이 없어 새로 만들었습니다.");
            return new LoadResult<>(items, warnings);
        }

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (lineNo == 1) {
                    line = line.replace("﻿", ""); // 엑셀 등에서 저장한 UTF-8 BOM 제거
                    if (line.trim().equals(header)) {
                        continue; // 헤더 줄
                    }
                }
                if (line.trim().isEmpty()) {
                    continue;
                }
                try {
                    items.add(parser.parse(CsvUtil.parseLine(line)));
                } catch (CsvFormatException | IllegalArgumentException e) {
                    warnings.add(fileName + " " + lineNo + "번째 줄을 건너뜀: " + e.getMessage());
                }
            }
        }
        return new LoadResult<>(items, warnings);
    }

    /**
     * 객체 목록을 파일에 저장한다.
     * 임시 파일에 먼저 쓴 뒤 교체하므로, 저장 도중 오류가 나도 기존 파일이 깨지지 않는다.
     */
    public void save(Collection<? extends T> items) throws IOException {
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path temp = path.resolveSibling(path.getFileName() + ".tmp");
        try (BufferedWriter writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
            writer.write(header);
            writer.newLine();
            for (T item : items) {
                writer.write(CsvUtil.join(item.toCsvFields()));
                writer.newLine();
            }
        }
        Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
    }
}
