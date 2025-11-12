package com.global.ums.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.global.ums.entity.UmsTreeProperty;

import java.util.List;

/**
 * 树节点属性 Service
 */
public interface UmsTreePropertyService extends IService<UmsTreeProperty> {

    Page<UmsTreeProperty> pageProperties(Page<UmsTreeProperty> page, Long treeId, String key);

    List<UmsTreeProperty> listByTreeId(Long treeId);

    boolean createProperty(UmsTreeProperty property);

    boolean updateProperty(UmsTreeProperty property);

    boolean removeProperty(Long id);
}
