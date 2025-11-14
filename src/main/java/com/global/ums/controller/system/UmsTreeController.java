package com.global.ums.controller.system;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.global.ums.annotation.RequireAuth;
import com.global.ums.entity.UmsTree;
import com.global.ums.result.AjaxResult;
import com.global.ums.service.UmsTreeService;
import com.global.ums.utils.MessageUtils;
import com.global.ums.utils.SpringUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ums_tree CRUD 控制器
 */
@Slf4j
@Api(value = "组织应用树管理", tags = "组织应用树管理")
@RestController
@RequestMapping("/system/tree")
@RequireAuth
public class UmsTreeController {

    @Autowired
    private UmsTreeService treeService;

    @ApiOperation("分页查询树节点")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "pageNum", value = "页码", defaultValue = "1", dataTypeClass = Integer.class),
            @ApiImplicitParam(name = "pageSize", value = "每页数量", defaultValue = "10", dataTypeClass = Integer.class),
            @ApiImplicitParam(name = "name", value = "名称(模糊)", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "类型 1=应用 2=部门", dataTypeClass = Integer.class)
    })
    @GetMapping("/page")
    public AjaxResult page(@RequestParam(defaultValue = "1") Integer pageNum,
                           @RequestParam(defaultValue = "10") Integer pageSize,
                           @RequestParam(required = false) String name,
                           @RequestParam(required = false) Integer type) {
        Page<UmsTree> page = new Page<>(pageNum, pageSize);
        return AjaxResult.success(treeService.pageTrees(page, name, type));
    }

    @ApiOperation("查询子节点列表")
    @GetMapping("/children/{parentId}")
    public AjaxResult children(@PathVariable Long parentId) {
        List<UmsTree> children = treeService.listChildren(parentId);
        return AjaxResult.success(children);
    }

    @ApiOperation("获取完整树形结构")
    @GetMapping("/tree")
    public AjaxResult tree(@RequestParam(required = false) Integer type) {
        return AjaxResult.success(treeService.buildTree(type));
    }

    @ApiOperation("节点详情")
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id) {
        UmsTree node = treeService.getById(id);
        if (node == null) {
            return AjaxResult.errorI18n("tree.node.not.found");
        }
        return AjaxResult.success(node);
    }

    @ApiOperation(value = "新增节点", notes = "支持字段：name(名称)、description(描述)、type(类型:1=应用 2=部门)、parentId(父节点ID)、formJson(动态表单JSON)")
    @PostMapping
    public AjaxResult create(@RequestBody UmsTree tree) {
        try {
            treeService.createNode(tree);
            String msg = SpringUtils.getBean(MessageUtils.class).getMessage("tree.node.create.success");
            return AjaxResult.success(msg, tree);
        } catch (IllegalArgumentException ex) {
            log.error("创建节点参数校验失败: {}", ex.getMessage(), ex);
            return AjaxResult.errorI18n(ex.getMessage());
        } catch (Exception ex) {
            log.error("创建节点失败", ex);
            return AjaxResult.errorI18n("tree.node.create.error");
        }
    }

    @ApiOperation(value = "更新节点", notes = "支持字段：id(必填)、name(名称)、description(描述)、type(类型:1=应用 2=部门)、parentId(父节点ID)、formJson(动态表单JSON)")
    @PutMapping
    public AjaxResult update(@RequestBody UmsTree tree) {
        try {
            treeService.updateNode(tree);
            String msg = SpringUtils.getBean(MessageUtils.class).getMessage("tree.node.update.success");
            return AjaxResult.success(msg, tree);
        } catch (IllegalArgumentException ex) {
            log.error("更新节点参数校验失败: {}", ex.getMessage(), ex);
            return AjaxResult.errorI18n(ex.getMessage());
        } catch (Exception ex) {
            log.error("更新节点失败", ex);
            return AjaxResult.errorI18n("tree.node.update.error");
        }
    }

    @ApiOperation("删除节点")
    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable Long id,
                             @RequestParam(defaultValue = "false") boolean cascade) {
        try {
            treeService.removeNode(id, cascade);
            return AjaxResult.successI18n("tree.node.delete.success");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            log.error("删除节点参数校验失败: {}", ex.getMessage(), ex);
            return AjaxResult.errorI18n(ex.getMessage());
        } catch (Exception ex) {
            log.error("删除节点失败", ex);
            return AjaxResult.errorI18n("tree.node.delete.error");
        }
    }
}
