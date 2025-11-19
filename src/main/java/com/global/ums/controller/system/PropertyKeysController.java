package com.global.ums.controller.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.global.ums.annotation.RequireAuth;
import com.global.ums.entity.PropertyKeys;
import com.global.ums.enums.DataType;
import com.global.ums.result.AjaxResult;
import com.global.ums.service.PropertyKeysService;
import com.global.ums.utils.KeyValidationUtils;
import com.global.ums.utils.MessageUtils;
import com.global.ums.utils.SpringUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 属性键配置管理控制器
 */
@Api(tags = "属性键配置管理")
@RestController
@RequestMapping("/system/property-keys")
@RequireAuth
public class PropertyKeysController {

    @Autowired
    private PropertyKeysService propertyKeysService;

    /**
     * 获取所有属性键配置（分页）
     */
    @ApiOperation(value = "分页查询属性键配置", notes = "支持按key模糊查询和scope精确查询")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "pageNum", value = "页码", defaultValue = "1", dataTypeClass = Integer.class, paramType = "query"),
            @ApiImplicitParam(name = "pageSize", value = "每页数量", defaultValue = "10", dataTypeClass = Integer.class, paramType = "query"),
            @ApiImplicitParam(name = "key", value = "属性键（支持模糊查询）", dataTypeClass = String.class, paramType = "query"),
            @ApiImplicitParam(name = "scope", value = "作用域（0:用户级 1:系统级）", dataTypeClass = Integer.class, paramType = "query")
    })
    @GetMapping("/page")
    public AjaxResult page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String key,
            @RequestParam(required = false) Integer scope) {
        try {
            Page<PropertyKeys> page = new Page<>(pageNum, pageSize);
            LambdaQueryWrapper<PropertyKeys> wrapper = new LambdaQueryWrapper<>();

            if (key != null && !key.isEmpty()) {
                wrapper.like(PropertyKeys::getKey, key);
            }
            if (scope != null) {
                wrapper.eq(PropertyKeys::getScope, scope);
            }

            Page<PropertyKeys> result = propertyKeysService.page(page, wrapper);
            return AjaxResult.success(result);
        } catch (Exception e) {
            return AjaxResult.error(SpringUtils.getBean(MessageUtils.class).getMessage("property.keys.list.error", e.getMessage()));
        }
    }

    /**
     * 获取所有属性键配置（不分页）
     */
    @ApiOperation(value = "查询所有属性键配置", notes = "返回所有属性键配置，不分页")
    @GetMapping("/list")
    public AjaxResult list() {
        try {
            List<PropertyKeys> list = propertyKeysService.list();
            return AjaxResult.success(list);
        } catch (Exception e) {
            return AjaxResult.error(SpringUtils.getBean(MessageUtils.class).getMessage("property.keys.list.error", e.getMessage()));
        }
    }

    /**
     * 根据ID获取属性键配置
     */
    @ApiOperation(value = "根据ID查询属性键配置", notes = "通过ID查询单个属性键配置详情")
    @ApiImplicitParam(name = "id", value = "属性键ID", required = true, dataTypeClass = Long.class, paramType = "path")
    @GetMapping("/{id}")
    public AjaxResult getById(@PathVariable Long id) {
        try {
            PropertyKeys config = propertyKeysService.getById(id);
            if (config == null) {
                return AjaxResult.error(SpringUtils.getBean(MessageUtils.class).getMessage("property.keys.not.found"));
            }
            return AjaxResult.success(config);
        } catch (Exception e) {
            return AjaxResult.error(SpringUtils.getBean(MessageUtils.class).getMessage("property.keys.get.error", e.getMessage()));
        }
    }

    /**
     * 根据key获取属性键配置
     */
    @ApiOperation(value = "根据key查询属性键配置", notes = "通过属性键名称查询配置详情")
    @ApiImplicitParam(name = "key", value = "属性键名称", required = true, dataTypeClass = String.class, paramType = "path")
    @GetMapping("/key/{key}")
    public AjaxResult getByKey(@PathVariable String key) {
        try {
            PropertyKeys config = propertyKeysService.getByKey(key);
            if (config == null) {
                return AjaxResult.error(SpringUtils.getBean(MessageUtils.class).getMessage("property.keys.not.found"));
            }
            return AjaxResult.success(config);
        } catch (Exception e) {
            return AjaxResult.error(SpringUtils.getBean(MessageUtils.class).getMessage("property.keys.get.error", e.getMessage()));
        }
    }

    /**
     * 添加属性键配置
     */
    @ApiOperation(value = "添加属性键配置", notes = "新增属性键配置，添加成功后自动刷新缓存")
    @PostMapping
    public AjaxResult add(@RequestBody PropertyKeys propertyKeys) {
        try {
            // 检查key是否已存在
            PropertyKeys existing = propertyKeysService.getByKey(propertyKeys.getKey());
            if (existing != null) {
                return AjaxResult.error(SpringUtils.getBean(MessageUtils.class).getMessage("property.keys.key.exists"));
            }

            boolean success = propertyKeysService.save(propertyKeys);
            if (success) {
                // 刷新缓存
                propertyKeysService.refreshCache();
                // 重新加载 KeyValidationUtils
                KeyValidationUtils.reloadConfig();
                return AjaxResult.successI18n("property.keys.add.success");
            } else {
                return AjaxResult.error(SpringUtils.getBean(MessageUtils.class).getMessage("property.keys.add.error"));
            }
        } catch (Exception e) {
            return AjaxResult.error(SpringUtils.getBean(MessageUtils.class).getMessage("property.keys.add.error", e.getMessage()));
        }
    }

    /**
     * 更新属性键配置
     */
    @ApiOperation(value = "更新属性键配置", notes = "修改属性键配置，更新成功后自动刷新缓存。注意：key字段不允许修改")
    @PutMapping
    public AjaxResult update(@RequestBody PropertyKeys propertyKeys) {
        try {
            if (propertyKeys.getId() == null) {
                return AjaxResult.error(SpringUtils.getBean(MessageUtils.class).getMessage("property.keys.id.required"));
            }

            // 检查ID是否存在
            PropertyKeys existing = propertyKeysService.getById(propertyKeys.getId());
            if (existing == null) {
                return AjaxResult.error(SpringUtils.getBean(MessageUtils.class).getMessage("property.keys.not.found"));
            }

            // 不允许修改 key 字段
            if (!existing.getKey().equals(propertyKeys.getKey())) {
                return AjaxResult.error("不允许修改属性键名称");
            }

            boolean success = propertyKeysService.updateById(propertyKeys);
            if (success) {
                // 刷新缓存
                propertyKeysService.refreshCache();
                // 重新加载 KeyValidationUtils
                KeyValidationUtils.reloadConfig();
                return AjaxResult.successI18n("property.keys.update.success");
            } else {
                return AjaxResult.error(SpringUtils.getBean(MessageUtils.class).getMessage("property.keys.update.error"));
            }
        } catch (Exception e) {
            return AjaxResult.error(SpringUtils.getBean(MessageUtils.class).getMessage("property.keys.update.error", e.getMessage()));
        }
    }

    /**
     * 删除属性键配置
     */
    @ApiOperation(value = "删除属性键配置", notes = "根据ID删除单个属性键配置。注意：如果该key已被使用则不允许删除")
    @ApiImplicitParam(name = "id", value = "属性键ID", required = true, dataTypeClass = Long.class, paramType = "path")
    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable Long id) {
        try {
            PropertyKeys existing = propertyKeysService.getById(id);
            if (existing == null) {
                return AjaxResult.error(SpringUtils.getBean(MessageUtils.class).getMessage("property.keys.not.found"));
            }

            // 检查该 key 是否在 UserProperties 表中使用过
            if (propertyKeysService.isKeyUsedInUserProperties(existing.getKey())) {
                return AjaxResult.error("该属性键已被使用，不允许删除");
            }

            boolean success = propertyKeysService.removeById(id);
            if (success) {
                // 刷新缓存
                propertyKeysService.refreshCache();
                // 重新加载 KeyValidationUtils
                KeyValidationUtils.reloadConfig();
                return AjaxResult.successI18n("property.keys.delete.success");
            } else {
                return AjaxResult.error(SpringUtils.getBean(MessageUtils.class).getMessage("property.keys.delete.error"));
            }
        } catch (Exception e) {
            return AjaxResult.error(SpringUtils.getBean(MessageUtils.class).getMessage("property.keys.delete.error", e.getMessage()));
        }
    }

    /**
     * 批量删除属性键配置
     */
    @ApiOperation(value = "批量删除属性键配置", notes = "根据ID列表批量删除属性键配置。注意：如果某个key已被使用则跳过该key")
    @DeleteMapping("/batch")
    public AjaxResult deleteBatch(@RequestBody List<Long> ids) {
        try {
            List<Long> cannotDelete = new ArrayList<>();
            List<Long> toDelete = new ArrayList<>();

            // 检查每个 key 是否被使用
            for (Long id : ids) {
                PropertyKeys propertyKey = propertyKeysService.getById(id);
                if (propertyKey != null) {
                    if (propertyKeysService.isKeyUsedInUserProperties(propertyKey.getKey())) {
                        cannotDelete.add(id);
                    } else {
                        toDelete.add(id);
                    }
                }
            }

            // 删除未被使用的 key
            if (!toDelete.isEmpty()) {
                propertyKeysService.removeByIds(toDelete);
                // 刷新缓存
                propertyKeysService.refreshCache();
                // 重新加载 KeyValidationUtils
                KeyValidationUtils.reloadConfig();
            }

            if (!cannotDelete.isEmpty()) {
                return AjaxResult.success("部分删除成功，" + cannotDelete.size() + " 个属性键已被使用无法删除");
            }

            return AjaxResult.successI18n("property.keys.delete.success");
        } catch (Exception e) {
            return AjaxResult.error(SpringUtils.getBean(MessageUtils.class).getMessage("property.keys.delete.error", e.getMessage()));
        }
    }

    /**
     * 刷新缓存
     */
    @ApiOperation(value = "刷新属性键配置缓存", notes = "手动刷新属性键配置缓存和验证工具")
    @PostMapping("/refresh")
    public AjaxResult refresh() {
        try {
            propertyKeysService.refreshCache();
            KeyValidationUtils.reloadConfig();
            return AjaxResult.successI18n("property.keys.refresh.success");
        } catch (Exception e) {
            return AjaxResult.error(SpringUtils.getBean(MessageUtils.class).getMessage("property.keys.refresh.error", e.getMessage()));
        }
    }

    /**
     * 获取所有数据类型枚举
     */
    @ApiOperation(value = "获取数据类型枚举", notes = "获取所有可用的数据类型枚举值，用于前端选择器等场景")
    @GetMapping("/data-types")
    public AjaxResult getDataTypes() {
        try {
            List<Map<String, Object>> dataTypes = new ArrayList<>();

            for (DataType dataType : DataType.values()) {
                // 跳过 UNKNOWN 类型
                if (dataType == DataType.UNKNOWN) {
                    continue;
                }

                Map<String, Object> item = new HashMap<>();
                item.put("value", dataType.getValue());
                item.put("code", dataType.getCode());
                item.put("description", dataType.getDescription());
                dataTypes.add(item);
            }

            return AjaxResult.success(dataTypes);
        } catch (Exception e) {
            return AjaxResult.error("获取数据类型失败: " + e.getMessage());
        }
    }
}
