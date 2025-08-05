package com.global.ums.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.global.ums.constant.UserPropertiesConstant;
import lombok.Data;

import java.util.List;

/**
 * 用户实体类
 */
@Data
@TableName("ums_user")
public class User {
    
    /**
     * 用户id
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 类型
     */
    private Integer type;
    
    /**
     * 类型描述（非数据库字段）
     */
    @TableField(exist = false)
   private String typeDesc;

    @TableField(exist = false)
   private String password;

    /**
     * 值
     */
   private String uniqueId;
    
    /**
     * 用户属性列表（非数据库字段）
     */
    @TableField(exist = false)
    private List<UserProperties> properties;

    /**
     * 从属性列表中获取用户名
     * @return 用户名
     */
    public String getUsernameFromProperties() {
        if (properties == null || properties.isEmpty()) {
            return null;
        }
        return properties.stream()
                .filter(p -> UserPropertiesConstant.KEY_USERNAME.equals(p.getKey()))
                .map(p -> new String(p.getValue()))
                .findFirst()
                .orElse(null);
    }
} 