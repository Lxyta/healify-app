package com.healify.repository;

import com.healify.model.MealPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface MealPlanRepository extends JpaRepository<MealPlan, Long> {
    List<MealPlan> findByUserId(Long userId);
    List<MealPlan> findByUserIdAndWeekStartDate(Long userId, LocalDate weekStartDate);
    List<MealPlan> findByUserIdAndWeekStartDateAndDayOfWeek(
            Long userId, LocalDate weekStartDate, Integer dayOfWeek);
    void deleteByUserIdAndWeekStartDate(Long userId, LocalDate weekStartDate);
}
