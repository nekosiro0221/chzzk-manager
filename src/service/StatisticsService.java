package service;

import exception.DataNotFoundException;
import model.Broadcast;
import model.Category;
import model.Donation;
import model.DonationType;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * 방송·후원 데이터를 분석해 통계를 계산한다. (데이터를 변경하지 않음)
 */
public class StatisticsService {

    /** 개수·보낸 금액·정산 금액을 함께 누적하는 집계 단위. */
    public static class Tally {
        private int count;
        private long sentAmount;
        private long settledAmount;

        void add(Donation d) {
            count++;
            sentAmount += d.getAmount();
            settledAmount += d.getSettledAmount();
        }

        public int getCount() {
            return count;
        }

        public long getSentAmount() {
            return sentAmount;
        }

        public long getSettledAmount() {
            return settledAmount;
        }
    }

    /** 월별 요약. */
    public static class MonthlySummary {
        private final YearMonth month;
        private int broadcastCount;
        private int totalMinutes;
        private long viewerSum;
        private Broadcast bestBroadcast;
        private final Tally donations = new Tally();

        MonthlySummary(YearMonth month) {
            this.month = month;
        }

        public YearMonth getMonth() {
            return month;
        }

        public int getBroadcastCount() {
            return broadcastCount;
        }

        public int getTotalMinutes() {
            return totalMinutes;
        }

        /** 방송별 최고 시청자 수의 평균. 방송이 없으면 0. */
        public double getAverageMaxViewers() {
            return broadcastCount == 0 ? 0 : (double) viewerSum / broadcastCount;
        }

        /** 최고 시청자 수가 가장 많았던 방송. 없으면 null. */
        public Broadcast getBestBroadcast() {
            return bestBroadcast;
        }

        public Tally getDonations() {
            return donations;
        }
    }

    /** 카테고리별 통계. */
    public static class CategoryStat {
        private final Category category;
        private int broadcastCount;
        private int totalMinutes;
        private long viewerSum;
        private int peakViewers;

        CategoryStat(Category category) {
            this.category = category;
        }

        public Category getCategory() {
            return category;
        }

        public int getBroadcastCount() {
            return broadcastCount;
        }

        public int getTotalMinutes() {
            return totalMinutes;
        }

        public double getAverageMaxViewers() {
            return broadcastCount == 0 ? 0 : (double) viewerSum / broadcastCount;
        }

        public int getPeakViewers() {
            return peakViewers;
        }
    }

    /** 순위표 한 줄 (이름 + 집계). */
    public static class RankEntry {
        private final String label;
        private final Tally tally;

        RankEntry(String label, Tally tally) {
            this.label = label;
            this.tally = tally;
        }

        public String getLabel() {
            return label;
        }

        public Tally getTally() {
            return tally;
        }
    }

    private final BroadcastService broadcastService;
    private final DonationService donationService;

    public StatisticsService(BroadcastService broadcastService, DonationService donationService) {
        this.broadcastService = broadcastService;
        this.donationService = donationService;
    }

    /** 방송 또는 후원 데이터가 있는 달 목록 (최근 달부터). */
    public List<YearMonth> getAvailableMonths() {
        TreeSet<YearMonth> months = new TreeSet<>(Comparator.reverseOrder());
        for (Broadcast b : broadcastService.getAll()) {
            months.add(YearMonth.from(b.getDate()));
        }
        for (Donation d : donationService.getAll()) {
            months.add(YearMonth.from(d.getDateTime()));
        }
        return new ArrayList<>(months);
    }

    public MonthlySummary getMonthlySummary(YearMonth month) {
        MonthlySummary s = new MonthlySummary(month);
        for (Broadcast b : broadcastService.getAll()) {
            if (YearMonth.from(b.getDate()).equals(month)) {
                s.broadcastCount++;
                s.totalMinutes += b.getDurationMinutes();
                s.viewerSum += b.getMaxViewers();
                if (s.bestBroadcast == null || b.getMaxViewers() > s.bestBroadcast.getMaxViewers()) {
                    s.bestBroadcast = b;
                }
            }
        }
        for (Donation d : donationService.getAll()) {
            if (YearMonth.from(d.getDateTime()).equals(month)) {
                s.donations.add(d);
            }
        }
        return s;
    }

    /** 카테고리별 통계 (평균 최고 시청자 수 내림차순). 방송이 있는 카테고리만 포함. */
    public List<CategoryStat> getCategoryStats() {
        Map<Category, CategoryStat> map = new EnumMap<>(Category.class);
        for (Broadcast b : broadcastService.getAll()) {
            CategoryStat stat = map.computeIfAbsent(b.getCategory(), CategoryStat::new);
            stat.broadcastCount++;
            stat.totalMinutes += b.getDurationMinutes();
            stat.viewerSum += b.getMaxViewers();
            stat.peakViewers = Math.max(stat.peakViewers, b.getMaxViewers());
        }
        List<CategoryStat> list = new ArrayList<>(map.values());
        list.sort(Comparator.comparingDouble(CategoryStat::getAverageMaxViewers).reversed());
        return list;
    }

    /** 정산 금액 기준 후원자 순위 상위 limit명. */
    public List<RankEntry> getTopDonors(int limit) {
        Map<String, Tally> map = new HashMap<>();
        for (Donation d : donationService.getAll()) {
            map.computeIfAbsent(d.getDonor(), k -> new Tally()).add(d);
        }
        return toRanking(map, limit);
    }

    /** 정산 금액 기준 방송 순위 상위 limit개. 라벨은 "#ID 제목". */
    public List<RankEntry> getTopBroadcastsByDonation(int limit) {
        Map<String, Tally> map = new HashMap<>();
        for (Donation d : donationService.getAll()) {
            String label;
            try {
                Broadcast b = broadcastService.findById(d.getBroadcastId());
                label = "#" + b.getId() + " " + b.getTitle();
            } catch (DataNotFoundException e) {
                label = "#" + d.getBroadcastId() + " (삭제된 방송)";
            }
            map.computeIfAbsent(label, k -> new Tally()).add(d);
        }
        return toRanking(map, limit);
    }

    /** 후원 종류별 집계. */
    public Map<DonationType, Tally> getTypeTotals() {
        Map<DonationType, Tally> map = new EnumMap<>(DonationType.class);
        for (DonationType t : DonationType.values()) {
            map.put(t, new Tally());
        }
        for (Donation d : donationService.getAll()) {
            map.get(d.getType()).add(d);
        }
        return Collections.unmodifiableMap(map);
    }

    private static List<RankEntry> toRanking(Map<String, Tally> map, int limit) {
        List<RankEntry> list = new ArrayList<>();
        for (Map.Entry<String, Tally> e : map.entrySet()) {
            list.add(new RankEntry(e.getKey(), e.getValue()));
        }
        list.sort(Comparator.comparingLong((RankEntry r) -> r.getTally().getSettledAmount()).reversed()
                .thenComparing(RankEntry::getLabel));
        return list.size() > limit ? list.subList(0, limit) : list;
    }
}
