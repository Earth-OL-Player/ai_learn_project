package com.earth.online.player.ailearn.system.infrastructure;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 系统设置 MyBatis 仓储。
 */
@Mapper
public interface SystemSettingMapper {

    /**
     * 按设置键查询设置值。
     *
     * @param settingKey 设置键
     * @return 设置值
     */
    @Select("SELECT setting_value FROM system_settings WHERE setting_key = #{settingKey}")
    String findValue(@Param("settingKey") String settingKey);

    /**
     * 新增或更新系统设置。
     *
     * @param settingKey 设置键
     * @param settingValue 设置值
     * @return 影响行数
     */
    @Insert("""
            INSERT INTO system_settings(setting_key, setting_value)
            VALUES(#{settingKey}, #{settingValue})
            ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value)
            """)
    int upsertValue(@Param("settingKey") String settingKey, @Param("settingValue") String settingValue);
}
