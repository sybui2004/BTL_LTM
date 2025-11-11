package com.ltm.memorygame.dto.user.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSettingsRequest {
    @Min(0)
    @Max(100)
    private Integer musicVolume;
    
    @Min(0)
    @Max(100)
    private Integer soundFxVolume;
    
    private Boolean notificationEnabled;
    
    private String language;
}

