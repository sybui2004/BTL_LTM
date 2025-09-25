package com.ltm.memorygame.dao.user;

import com.ltm.memorygame.model.user.UserSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSettingRepository extends JpaRepository<UserSetting, Long> {
}
