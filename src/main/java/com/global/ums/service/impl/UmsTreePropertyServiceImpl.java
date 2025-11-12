package com.global.ums.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.global.ums.entity.UmsTree;
import com.global.ums.entity.UmsTreeProperty;
import com.global.ums.mapper.UmsTreePropertyMapper;
import com.global.ums.service.UmsTreePropertyService;
import com.global.ums.service.UmsTreeService;
import com.global.ums.utils.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 树节点属性 Service 实现
 */
@Service
public class UmsTreePropertyServiceImpl extends ServiceImpl<UmsTreePropertyMapper, UmsTreeProperty>
        implements UmsTreePropertyService {

    private final UmsTreeService treeService;

    public UmsTreePropertyServiceImpl(UmsTreeService treeService) {
        this.treeService = treeService;
    }

    @Override
    public Page<UmsTreeProperty> pageProperties(Page<UmsTreeProperty> page, Long treeId, String key) {
        LambdaQueryWrapper<UmsTreeProperty> wrapper = Wrappers.lambdaQuery();
        if (treeId != null) {
            wrapper.eq(UmsTreeProperty::getTreeId, treeId);
        }
        if (StringUtils.isNotBlank(key)) {
            wrapper.like(UmsTreeProperty::getKey, key);
        }
        wrapper.orderByAsc(UmsTreeProperty::getTreeId).orderByAsc(UmsTreeProperty::getId);
        return this.page(page, wrapper);
    }

    @Override
    public List<UmsTreeProperty> listByTreeId(Long treeId) {
        return this.list(
                Wrappers.<UmsTreeProperty>lambdaQuery()
                        .eq(UmsTreeProperty::getTreeId, treeId)
                        .orderByAsc(UmsTreeProperty::getId)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createProperty(UmsTreeProperty property) {
        validate(property, false);
        if (property.getRequired() == null) {
            property.setRequired(Boolean.FALSE);
        }
        return this.save(property);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateProperty(UmsTreeProperty property) {
        validate(property, true);
        return this.updateById(property);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeProperty(Long id) {
        return this.removeById(id);
    }

    private void validate(UmsTreeProperty property, boolean isUpdate) {
        if (property == null || property.getTreeId() == null) {
            throw new IllegalArgumentException("tree.property.invalid");
        }
        UmsTree tree = treeService.getById(property.getTreeId());
        if (tree == null) {
            throw new IllegalArgumentException("tree.property.tree.not.found");
        }
        if (StringUtils.isBlank(property.getKey())) {
            throw new IllegalArgumentException("tree.property.key.blank");
        }
        LambdaQueryWrapper<UmsTreeProperty> dupWrapper = Wrappers.<UmsTreeProperty>lambdaQuery()
                .eq(UmsTreeProperty::getTreeId, property.getTreeId())
                .eq(UmsTreeProperty::getKey, property.getKey());
        if (isUpdate && property.getId() != null) {
            dupWrapper.ne(UmsTreeProperty::getId, property.getId());
        }
        if (this.count(dupWrapper) > 0) {
            throw new IllegalArgumentException("tree.property.key.duplicate");
        }
    }
}
