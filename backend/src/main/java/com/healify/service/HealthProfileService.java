package com.healify.service;

import com.healify.dto.HealthProfileDTO;
import com.healify.model.HealthProfile;
import com.healify.repository.HealthProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HealthProfileService {

    private final HealthProfileRepository profileRepository;

    public HealthProfile getProfile(Long userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("健康档案未创建，请先完善档案"));
    }

    public HealthProfile saveOrUpdate(Long userId, HealthProfileDTO dto) {
        HealthProfile profile = profileRepository.findByUserId(userId)
                .orElse(HealthProfile.builder().userId(userId).build());

        profile.setGender(dto.getGender());
        profile.setBirthDate(dto.getBirthDate());
        profile.setHeightCm(dto.getHeightCm());
        profile.setCurrentWeightKg(dto.getCurrentWeightKg());
        profile.setTargetWeightKg(dto.getTargetWeightKg());
        profile.setActivityLevel(dto.getActivityLevel());
        profile.setDietPreference(dto.getDietPreference());
        profile.setAllergies(dto.getAllergies());
        profile.setDailyCalorieGoal(dto.getDailyCalorieGoal());
        profile.setDailyProteinG(dto.getDailyProteinG());
        profile.setExerciseFrequency(dto.getExerciseFrequency());
        profile.setExerciseDuration(dto.getExerciseDuration());

        return profileRepository.save(profile);
    }
}
