package com.global.ums.controller.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.global.ums.annotation.RequireAuth;
import com.global.ums.entity.PropertyKeyItems;
import com.global.ums.result.AjaxResult;
import com.global.ums.service.PropertyKeyItemsService;
import com.global.ums.utils.MessageUtils;
import com.global.ums.utils.SpringUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 属性键枚举项管理控制器
 */
@Api(tags = "属性键枚举项管理")
@RestController
@RequestMapping("/system/property-key-items")
@RequireAuth
public class PropertyKeyItemsController {

    @Autowired
    private PropertyKeyItemsService propertyKeyItemsService;

    /**
     * 分页查询枚举项
     */
    @ApiOperation(value = "分页查询枚举项", notes = "支持按key和itemValue模糊查询")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "pageNum", value = "页码", defaultValue = "1", dataTypeClass = Integer.class, paramType = "query"),
            @ApiImplicitParam(name = "pageSize", value = "每页数量", defaultValue = "10", dataTypeClass = Integer.class, paramType = "query"),
            @ApiImplicitParam(name = "key", value = "属性键（支持模糊查询）", dataTypeClass = String.class, paramType = "query"),
            @ApiImplicitParam(name = "itemValue", value = "枚举值（支持模糊查询）", dataTypeClass = String.class, paramType = "query")
    })
    @GetMapping("/page")
    public AjaxResult page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String key,
            @RequestParam(required = false) String itemValue) {
        try {
            Page<PropertyKeyItems> page = new Page<>(pageNum, pageSize);
            LambdaQueryWrapper<PropertyKeyItems> wrapper = new LambdaQueryWrapper<>();

            if (key != null && !key.isEmpty()) {
                wrapper.like(PropertyKeyItems::getKey, key);
            }
            if (itemValue != null && !itemValue.isEmpty()) {
                wrapper.like(PropertyKeyItems::getItemValue, itemValue);
            }

            wrapper.orderByAsc(PropertyKeyItems::getKey)
                    .orderByAsc(PropertyKeyItems::getPriority);

            Page<PropertyKeyItems> result = propertyKeyItemsService.page(page, wrapper);
            return AjaxResult.success(result);
        } catch (Exception e) {
            return AjaxResult.error("分页查询枚举项失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有枚举项（不分页）
     */
    @ApiOperation(value = "查询所有枚举项", notes = "返回所有枚举项，按key和优先级排序")
    @GetMapping("/list")
    public AjaxResult list() {
        try {
            LambdaQueryWrapper<PropertyKeyItems> wrapper = new LambdaQueryWrapper<>();
            wrapper.orderByAsc(PropertyKeyItems::getKey)
                    .orderByAsc(PropertyKeyItems::getPriority);
            List<PropertyKeyItems> list = propertyKeyItemsService.list(wrapper);
            return AjaxResult.success(list);
        } catch (Exception e) {
            return AjaxResult.error("查询枚举项列表失败: " + e.getMessage());
        }
    }

    /**
     * 根据属性键获取所有枚举项
     */
    @ApiOperation(value = "根据属性键查询枚举项", notes = "查询指定属性键的所有枚举项，按优先级排序")
    @ApiImplicitParam(name = "key", value = "属性键名称", required = true, dataTypeClass = String.class, paramType = "path")
    @GetMapping("/key/{key}")
    public AjaxResult getByKey(@PathVariable String key) {
        try {
            List<PropertyKeyItems> items = propertyKeyItemsService.getItemsByKey(key);
            return AjaxResult.success(items);
        } catch (Exception e) {
            return AjaxResult.error("查询枚举项失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID获取枚举项
     */
    @ApiOperation(value = "根据ID查询枚举项", notes = "通过ID查询单个枚举项详情")
    @ApiImplicitParam(name = "id", value = "枚举项ID", required = true, dataTypeClass = Long.class, paramType = "path")
    @GetMapping("/{id}")
    public AjaxResult getById(@PathVariable Long id) {
        try {
            PropertyKeyItems item = propertyKeyItemsService.getById(id);
            if (item == null) {
                return AjaxResult.error("枚举项不存在");
            }
            return AjaxResult.success(item);
        } catch (Exception e) {
            return AjaxResult.error("查询枚举项失败: " + e.getMessage());
        }
    }

    /**
     * 添加枚举项
     */
    @ApiOperation(value = "添加枚举项", notes = "新增属性键枚举项")
    @PostMapping
    public AjaxResult add(@RequestBody PropertyKeyItems propertyKeyItems) {
        try {
            // 检查是否已存在相同的 key + itemValue 组合
            if (propertyKeyItemsService.existsItem(propertyKeyItems.getKey(), propertyKeyItems.getItemValue())) {
                return AjaxResult.error("该属性键下已存在相同的枚举值");
            }

            // 设置默认值
            if (propertyKeyItems.getPriority() == null) {
                propertyKeyItems.setPriority(0);
            }

            boolean success = propertyKeyItemsService.save(propertyKeyItems);
            if (success) {
                return AjaxResult.success("添加枚举项成功");
            } else {
                return AjaxResult.error("添加枚举项失败");
            }
        } catch (Exception e) {
            return AjaxResult.error("添加枚举项失败: " + e.getMessage());
        }
    }

    /**
     * 更新枚举项
     */
    @ApiOperation(value = "更新枚举项", notes = "修改枚举项信息。注意：key和itemValue字段不允许修改")
    @PutMapping
    public AjaxResult update(@RequestBody PropertyKeyItems propertyKeyItems) {
        try {
            if (propertyKeyItems.getId() == null) {
                return AjaxResult.error("枚举项ID不能为空");
            }

            // 检查ID是否存在
            PropertyKeyItems existing = propertyKeyItemsService.getById(propertyKeyItems.getId());
            if (existing == null) {
                return AjaxResult.error("枚举项不存在");
            }

            // 不允许修改 key 和 itemValue 字段
            if (!existing.getKey().equals(propertyKeyItems.getKey())) {
                return AjaxResult.error("不允许修改属性键名称");
            }
            if (!existing.getItemValue().equals(propertyKeyItems.getItemValue())) {
                return AjaxResult.error("不允许修改枚举值");
            }

            boolean success = propertyKeyItemsService.updateById(propertyKeyItems);
            if (success) {
                return AjaxResult.success("更新枚举项成功");
            } else {
                return AjaxResult.error("更新枚举项失败");
            }
        } catch (Exception e) {
            return AjaxResult.error("更新枚举项失败: " + e.getMessage());
        }
    }

    /**
     * 删除枚举项
     */
    @ApiOperation(value = "删除枚举项", notes = "根据ID删除单个枚举项")
    @ApiImplicitParam(name = "id", value = "枚举项ID", required = true, dataTypeClass = Long.class, paramType = "path")
    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable Long id) {
        try {
            PropertyKeyItems existing = propertyKeyItemsService.getById(id);
            if (existing == null) {
                return AjaxResult.error("枚举项不存在");
            }

            boolean success = propertyKeyItemsService.removeById(id);
            if (success) {
                return AjaxResult.success("删除枚举项成功");
            } else {
                return AjaxResult.error("删除枚举项失败");
            }
        } catch (Exception e) {
            return AjaxResult.error("删除枚举项失败: " + e.getMessage());
        }
    }

    /**
     * 批量删除枚举项
     */
    @ApiOperation(value = "批量删除枚举项", notes = "根据ID列表批量删除枚举项")
    @DeleteMapping("/batch")
    public AjaxResult deleteBatch(@RequestBody List<Long> ids) {
        try {
            boolean success = propertyKeyItemsService.removeByIds(ids);
            if (success) {
                return AjaxResult.success("批量删除枚举项成功");
            } else {
                return AjaxResult.error("批量删除枚举项失败");
            }
        } catch (Exception e) {
            return AjaxResult.error("批量删除枚举项失败: " + e.getMessage());
        }
    }
}
