package com.global.ums.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.global.ums.entity.UmsTree;

import java.util.List;

/**
 * ums_tree Service
 */
public interface UmsTreeService extends IService<UmsTree> {

    /**
     * 分页检索树节点
     */
    Page<UmsTree> pageTrees(Page<UmsTree> page, String name, Integer type);

    /**
     * 根据父节点获取子节点列表
     */
    List<UmsTree> listChildren(Long parentId);

    /**
     * 构建树形结构
     */
    List<UmsTree> buildTree(Integer type);

    /**
     * 创建节点
     */
    boolean createNode(UmsTree tree);

    /**
     * 更新节点
     */
    boolean updateNode(UmsTree tree);

    /**
     * 删除节点
     *
     * @param cascade true 时级联删除所有子节点
     */
    boolean removeNode(Long id, boolean cascade);
}
