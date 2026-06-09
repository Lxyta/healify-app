package com.healify.controller;

import com.healify.dto.HealthProfileDTO;
import com.healify.model.HealthProfile;
import com.healify.service.HealthProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class HealthProfileController {

    private final HealthProfileService profileService;

    @GetMapping
    public ResponseEntity<HealthProfile> getProfile(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(profileService.getProfile(userId));
    }

    @PutMapping
    public ResponseEntity<HealthProfile> saveProfile(Authentication auth,
                                                      @Valid @RequestBody HealthProfileDTO dto) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(profileService.saveOrUpdate(userId, dto));
    }
}
