package com.healify.repository;

import com.healify.model.WeightRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WeightRecordRepository extends JpaRepository<WeightRecord, Long> {
    List<WeightRecord> findByUserIdOrderByRecordedAtDesc(Long userId);
    Optional<WeightRecord> findFirstByUserIdOrderByRecordedAtDesc(Long userId);
    List<WeightRecord> findByUserIdAndRecordedAtBetweenOrderByRecordedAtAsc(
            Long userId, LocalDate start, LocalDate end);
    List<WeightRecord> findByUserIdAndRecordedAtAfterOrderByRecordedAtAsc(
            Long userId, LocalDate after);

    /** 获取最近 N 周的体重记录，用于 AI 趋势分析 */
    default List<WeightRecord> getRecentForAnalysis(Long userId, int weeks) {
        LocalDate since = LocalDate.now().minusWeeks(weeks);
        return findByUserIdAndRecordedAtAfterOrderByRecordedAtAsc(userId, since);
    }
}
