package com.global.ums.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.global.ums.entity.Template;

import java.util.List;

/**
 * ums_template Service
 */
public interface TemplateService extends IService<Template> {

    /**
     * 分页检索模板节点
     */
    Page<Template> pageTrees(Page<Template> page, String name, Integer type);

    /**
     * 根据父节点获取子节点列表
     */
    List<Template> listChildren(Long parentId);

    /**
     * 构建树形结构
     */
    List<Template> buildTree(Integer type);

    /**
     * 创建节点
     */
    boolean createNode(Template tree);

    /**
     * 更新节点
     */
    boolean updateNode(Template tree);

    /**
     * 删除节点
     *
     * @param cascade true 时级联删除所有子节点
     */
    boolean removeNode(Long id, boolean cascade);
}
