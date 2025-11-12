package com.global.ums.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.global.ums.entity.UmsTree;
import com.global.ums.enums.TreeType;
import com.global.ums.mapper.UmsTreeMapper;
import com.global.ums.service.UmsTreeService;
import com.global.ums.utils.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ums_tree Service 实现
 */
@Service
public class UmsTreeServiceImpl extends ServiceImpl<UmsTreeMapper, UmsTree> implements UmsTreeService {

    @Override
    public Page<UmsTree> pageTrees(Page<UmsTree> page, String name, Integer type) {
        LambdaQueryWrapper<UmsTree> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.isNotBlank(name)) {
            wrapper.like(UmsTree::getName, name);
        }
        if (type != null) {
            wrapper.eq(UmsTree::getType, type);
        }
        wrapper.orderByAsc(UmsTree::getParentId).orderByAsc(UmsTree::getId);
        return this.page(page, wrapper);
    }

    @Override
    public List<UmsTree> listChildren(Long parentId) {
        return this.list(
                Wrappers.<UmsTree>lambdaQuery()
                        .eq(UmsTree::getParentId, parentId)
                        .orderByAsc(UmsTree::getId)
        );
    }

    @Override
    public List<UmsTree> buildTree(Integer type) {
        LambdaQueryWrapper<UmsTree> wrapper = Wrappers.lambdaQuery();
        if (type != null) {
            wrapper.eq(UmsTree::getType, type);
        }
        wrapper.orderByAsc(UmsTree::getParentId).orderByAsc(UmsTree::getId);
        List<UmsTree> nodes = this.list(wrapper);
        return assembleTree(nodes);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createNode(UmsTree tree) {
        validateNode(tree, false);
        if (tree.getParentId() == null) {
            tree.setParentId(0L);
        }
        return this.save(tree);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateNode(UmsTree tree) {
        validateNode(tree, true);
        return this.updateById(tree);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeNode(Long id, boolean cascade) {
        if (id == null) {
            return false;
        }
        if (!cascade) {
            long childCount = this.count(Wrappers.<UmsTree>lambdaQuery().eq(UmsTree::getParentId, id));
            if (childCount > 0) {
                throw new IllegalStateException("tree.node.delete.children.exists");
            }
        } else {
            removeChildrenRecursive(id);
        }
        return this.removeById(id);
    }

    private void removeChildrenRecursive(Long parentId) {
        List<UmsTree> children = listChildren(parentId);
        if (children.isEmpty()) {
            return;
        }
        for (UmsTree child : children) {
            removeChildrenRecursive(child.getId());
        }
        this.removeByIds(children.stream().map(UmsTree::getId).collect(Collectors.toList()));
    }

    private void validateNode(UmsTree tree, boolean isUpdate) {
        if (tree == null) {
            throw new IllegalArgumentException("tree.node.invalid");
        }
        if (StringUtils.isBlank(tree.getName())) {
            throw new IllegalArgumentException("tree.node.name.blank");
        }
        TreeType treeType = TreeType.fromValue(tree.getType());
        if (!treeType.isValid()) {
            throw new IllegalArgumentException("tree.node.type.invalid");
        }
        Long parentId = tree.getParentId();
        if (parentId == null) {
            tree.setParentId(0L);
        } else if (parentId > 0) {
            UmsTree parent = this.getById(parentId);
            if (parent == null) {
                throw new IllegalArgumentException("tree.node.parent.not.found");
            }
            if (isUpdate) {
                ensureNoCycle(tree.getId(), parentId);
            }
        }
        LambdaQueryWrapper<UmsTree> dupWrapper = Wrappers.<UmsTree>lambdaQuery()
                .eq(UmsTree::getParentId, tree.getParentId())
                .eq(UmsTree::getName, tree.getName());
        if (isUpdate && tree.getId() != null) {
            dupWrapper.ne(UmsTree::getId, tree.getId());
        }
        if (this.count(dupWrapper) > 0) {
            throw new IllegalArgumentException("tree.node.name.duplicate");
        }
    }

    private void ensureNoCycle(Long currentId, Long parentId) {
        if (currentId == null || parentId == null || parentId == 0) {
            return;
        }
        if (Objects.equals(currentId, parentId)) {
            throw new IllegalArgumentException("tree.node.parent.cycle");
        }
        UmsTree parent = this.getById(parentId);
        if (parent != null) {
            ensureNoCycle(currentId, parent.getParentId());
        }
    }

    private List<UmsTree> assembleTree(List<UmsTree> nodes) {
        if (nodes.isEmpty()) {
            return nodes;
        }
        Map<Long, UmsTree> indexed = nodes.stream()
                .collect(Collectors.toMap(UmsTree::getId, Function.identity()));
        nodes.forEach(node -> node.setChildren(new ArrayList<>()));

        List<UmsTree> roots = new ArrayList<>();
        for (UmsTree node : nodes) {
            Long parentId = node.getParentId();
            if (parentId == null || parentId == 0L || !indexed.containsKey(parentId)) {
                roots.add(node);
                continue;
            }
            indexed.get(parentId).getChildren().add(node);
        }
        return roots;
    }
}
