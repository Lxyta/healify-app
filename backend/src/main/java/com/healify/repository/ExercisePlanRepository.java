package com.healify.repository;

import com.healify.model.ExercisePlan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface ExercisePlanRepository extends JpaRepository<ExercisePlan, Long> {
    List<ExercisePlan> findByUserIdAndWeekStartDate(Long userId, LocalDate weekStartDate);
    List<ExercisePlan> findByUserIdAndWeekStartDateAndDayOfWeek(
            Long userId, LocalDate weekStartDate, Integer dayOfWeek);
    void deleteByUserIdAndWeekStartDate(Long userId, LocalDate weekStartDate);
    List<ExercisePlan> findByUserId(Long userId);
}
