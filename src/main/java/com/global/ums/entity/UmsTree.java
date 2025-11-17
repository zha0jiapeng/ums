package com.global.ums.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 组织/应用树节点实体
 */
@Data
@TableName("ums_tree")
public class UmsTree {

    /**
     * 主键 ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 父节点 ID，根节点可为 0
     */
    private Long parentId;

    /**
     * 节点名称（父节点下唯一）
     */
    private String name;

    /**
     * 节点中文描述
     */
    private String description;

    /**
     * 节点类型：1 应用 2 部门
     */
    private Integer type;

    /**
     * 动态表单 JSON
     */
    private String formJson;

    /**
     * 子节点集合（仅出参使用）
     */
    @TableField(exist = false)
    private List<UmsTree> children;

    /**
     * 新增属性时的默认值集合，key 为属性键，value 为默认值
     */
    @TableField(exist = false)
    private List<Map<String, Object>> propertyDefaults;

    /**
     * 删除属性时提交的属性键集合
     */
    @TableField(exist = false)
    private List<String> deleteKeys;
}
