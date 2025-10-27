package com.global.ums.controller.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.global.ums.annotation.RequireAuth;
import com.global.ums.entity.UmsPropertyKeys;
import com.global.ums.result.AjaxResult;
import com.global.ums.service.UmsPropertyKeysService;
import com.global.ums.utils.KeyValidationUtils;
import com.global.ums.utils.MessageUtils;
import com.global.ums.utils.SpringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 属性键配置管理控制器
 */
@RestController
@RequestMapping("/system/property-keys")
@RequireAuth
public class UmsPropertyKeysController {

    @Autowired
    private UmsPropertyKeysService propertyKeysService;

    /**
     * 获取所有属性键配置（分页）
     */
    @GetMapping("/list")
    public AjaxResult list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String key,
            @RequestParam(required = false) Integer scope) {
        try {
            Page<UmsPropertyKeys> page = new Page<>(pageNum, pageSize);
            LambdaQueryWrapper<UmsPropertyKeys> wrapper = new LambdaQueryWrapper<>();

            if (key != null && !key.isEmpty()) {
                wrapper.like(UmsPropertyKeys::getKey, key);
            }
            if (scope != null) {
                wrapper.eq(UmsPropertyKeys::getScope, scope);
            }

            Page<UmsPropertyKeys> result = propertyKeysService.page(page, wrapper);
            return AjaxResult.success(result);
        } catch (Exception e) {
            return AjaxResult.error(SpringUtils.getBean(MessageUtils.class).getMessage("property.keys.list.error", e.getMessage()));
        }
    }

    /**
     * 获取所有属性键配置（不分页）
     */
    @GetMapping("/all")
    public AjaxResult all() {
        try {
            List<UmsPropertyKeys> list = propertyKeysService.list();
            return AjaxResult.success(list);
        } catch (Exception e) {
            return AjaxResult.error(SpringUtils.getBean(MessageUtils.class).getMessage("property.keys.list.error", e.getMessage()));
        }
    }

    /**
     * 根据ID获取属性键配置
     */
    @GetMapping("/{id}")
    public AjaxResult getById(@PathVariable Long id) {
        try {
            UmsPropertyKeys config = propertyKeysService.getById(id);
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
    @GetMapping("/key/{key}")
    public AjaxResult getByKey(@PathVariable String key) {
        try {
            UmsPropertyKeys config = propertyKeysService.getByKey(key);
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
    @PostMapping
    public AjaxResult add(@RequestBody UmsPropertyKeys propertyKeys) {
        try {
            // 检查key是否已存在
            UmsPropertyKeys existing = propertyKeysService.getByKey(propertyKeys.getKey());
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
    @PutMapping
    public AjaxResult update(@RequestBody UmsPropertyKeys propertyKeys) {
        try {
            if (propertyKeys.getId() == null) {
                return AjaxResult.error(SpringUtils.getBean(MessageUtils.class).getMessage("property.keys.id.required"));
            }

            // 检查ID是否存在
            UmsPropertyKeys existing = propertyKeysService.getById(propertyKeys.getId());
            if (existing == null) {
                return AjaxResult.error(SpringUtils.getBean(MessageUtils.class).getMessage("property.keys.not.found"));
            }

            // 如果修改了key，检查新key是否已被使用
            if (!existing.getKey().equals(propertyKeys.getKey())) {
                UmsPropertyKeys keyExists = propertyKeysService.getByKey(propertyKeys.getKey());
                if (keyExists != null) {
                    return AjaxResult.error(SpringUtils.getBean(MessageUtils.class).getMessage("property.keys.key.exists"));
                }
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
    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable Long id) {
        try {
            UmsPropertyKeys existing = propertyKeysService.getById(id);
            if (existing == null) {
                return AjaxResult.error(SpringUtils.getBean(MessageUtils.class).getMessage("property.keys.not.found"));
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
    @DeleteMapping("/batch")
    public AjaxResult deleteBatch(@RequestBody List<Long> ids) {
        try {
            boolean success = propertyKeysService.removeByIds(ids);
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
     * 刷新缓存
     */
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
}
