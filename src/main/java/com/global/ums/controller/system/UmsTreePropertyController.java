package com.global.ums.controller.system;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.global.ums.annotation.RequireAuth;
import com.global.ums.entity.UmsTreeProperty;
import com.global.ums.result.AjaxResult;
import com.global.ums.service.UmsTreePropertyService;
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
 * 树节点属性 CRUD 控制器
 */
@Api(tags = "树节点属性配置")
@RestController
@RequestMapping("/system/tree/properties")
@RequireAuth
public class UmsTreePropertyController {

    @Autowired
    private UmsTreePropertyService propertyService;

    @ApiOperation("分页查询树属性")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "pageNum", value = "页码", defaultValue = "1", dataTypeClass = Integer.class),
            @ApiImplicitParam(name = "pageSize", value = "每页数量", defaultValue = "10", dataTypeClass = Integer.class),
            @ApiImplicitParam(name = "treeId", value = "树节点ID", dataTypeClass = Long.class),
            @ApiImplicitParam(name = "key", value = "属性键(模糊)", dataTypeClass = String.class)
    })
    @GetMapping("/page")
    public AjaxResult page(@RequestParam(defaultValue = "1") Integer pageNum,
                           @RequestParam(defaultValue = "10") Integer pageSize,
                           @RequestParam(required = false) Long treeId,
                           @RequestParam(required = false) String key) {
        Page<UmsTreeProperty> page = new Page<>(pageNum, pageSize);
        return AjaxResult.success(propertyService.pageProperties(page, treeId, key));
    }

    @ApiOperation("按树节点查询属性列表")
    @GetMapping("/tree/{treeId}")
    public AjaxResult list(@PathVariable Long treeId) {
        List<UmsTreeProperty> list = propertyService.listByTreeId(treeId);
        return AjaxResult.success(list);
    }

    @ApiOperation("属性详情")
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id) {
        UmsTreeProperty property = propertyService.getById(id);
        if (property == null) {
            return AjaxResult.errorI18n("tree.property.not.found");
        }
        return AjaxResult.success(property);
    }

    @ApiOperation("新增属性")
    @PostMapping
    public AjaxResult create(@RequestBody UmsTreeProperty property) {
        try {
            propertyService.createProperty(property);
            String msg = SpringUtils.getBean(MessageUtils.class).getMessage("tree.property.create.success");
            return AjaxResult.success(msg, property);
        } catch (IllegalArgumentException ex) {
            return AjaxResult.errorI18n(ex.getMessage());
        } catch (Exception ex) {
            return AjaxResult.errorI18n("tree.property.create.error");
        }
    }

    @ApiOperation("更新属性")
    @PutMapping
    public AjaxResult update(@RequestBody UmsTreeProperty property) {
        try {
            propertyService.updateProperty(property);
            String msg = SpringUtils.getBean(MessageUtils.class).getMessage("tree.property.update.success");
            return AjaxResult.success(msg, property);
        } catch (IllegalArgumentException ex) {
            return AjaxResult.errorI18n(ex.getMessage());
        } catch (Exception ex) {
            return AjaxResult.errorI18n("tree.property.update.error");
        }
    }

    @ApiOperation("删除属性")
    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable Long id) {
        if (propertyService.removeProperty(id)) {
            return AjaxResult.successI18n("tree.property.delete.success");
        }
        return AjaxResult.errorI18n("tree.property.delete.error");
    }
}
