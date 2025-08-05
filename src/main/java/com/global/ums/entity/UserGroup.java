package com.global.ums.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 用户组实体类
 */
@Data
@TableName("user_group")
public class UserGroup {
    
    /**
     * ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 上级用户ID
     */
    private Long parentUserId;
    
    /**
     * 用户对象（非数据库字段）
     */
    @TableField(exist = false)
    private User user;
    
    /**
     * 上级用户对象（非数据库字段）
     */
    @TableField(exist = false)
    private User parentUser;
} 