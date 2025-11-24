package com.global.ums.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.global.ums.constant.UserPropertiesConstant;
import com.global.ums.entity.Template;
import com.global.ums.entity.UserProperties;
import com.global.ums.enums.TemplateType;
import com.global.ums.mapper.TemplateMapper;
import com.global.ums.service.TemplateService;
import com.global.ums.service.UserPropertiesService;
import com.global.ums.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ums_template Service 实现
 */
@Service
public class TemplateServiceImpl extends ServiceImpl<TemplateMapper, Template> implements TemplateService {

    @Autowired
    private UserPropertiesService userPropertiesService;

    @Override
    public Page<Template> pageTrees(Page<Template> page, String name, Integer type) {
        LambdaQueryWrapper<Template> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.isNotBlank(name)) {
            wrapper.like(Template::getName, name);
        }
        if (type != null) {
            wrapper.eq(Template::getType, type);
        }
        wrapper.orderByAsc(Template::getParentId).orderByAsc(Template::getId);
        return this.page(page, wrapper);
    }

    @Override
    public List<Template> listChildren(Long parentId) {
        return this.list(
                Wrappers.<Template>lambdaQuery()
                        .eq(Template::getParentId, parentId)
                        .orderByAsc(Template::getId)
        );
    }

    @Override
    public List<Template> buildTree(Integer type) {
        LambdaQueryWrapper<Template> wrapper = Wrappers.lambdaQuery();
        if (type != null) {
            wrapper.eq(Template::getType, type);
        }
        wrapper.orderByAsc(Template::getParentId).orderByAsc(Template::getId);
        List<Template> nodes = this.list(wrapper);
        return assembleTree(nodes);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createNode(Template tree) {
        validateNode(tree, false);
        if (tree.getParentId() == null) {
            tree.setParentId(0L);
        }
        return this.save(tree);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateNode(Template tree) {
        validateNode(tree, true);
        return this.updateById(tree);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeNode(Long id, boolean cascade) {
        if (id == null) {
            return false;
        }

        // 检查模板是否被用户引用
        checkTemplateReference(id);

        if (!cascade) {
            long childCount = this.count(Wrappers.<Template>lambdaQuery().eq(Template::getParentId, id));
            if (childCount > 0) {
                throw new IllegalStateException("tree.node.delete.children.exists");
            }
        } else {
            removeChildrenRecursive(id);
        }
        return this.removeById(id);
    }

    /**
     * 检查模板是否被用户引用
     * @param templateId 模板ID
     */
    private void checkTemplateReference(Long templateId) {
        String templateIdString = String.valueOf(templateId);

        // 查询 user_properties 表中 key='templateId' 的所有记录
        List<UserProperties> templateBindings = userPropertiesService.list(
            Wrappers.<UserProperties>lambdaQuery()
                .eq(UserProperties::getKey, UserPropertiesConstant.KEY_TEMPLATE_ID)
        );

        // 检查是否有用户引用了该模板
        for (UserProperties binding : templateBindings) {
            if (binding.getValue() == null) {
                continue;
            }
            String bindingTemplateId = new String(binding.getValue());
            if (templateIdString.equals(bindingTemplateId)) {
                throw new IllegalStateException("template.delete.referenced");
            }
        }
    }

    private void removeChildrenRecursive(Long parentId) {
        List<Template> children = listChildren(parentId);
        if (children.isEmpty()) {
            return;
        }
        for (Template child : children) {
            removeChildrenRecursive(child.getId());
        }
        this.removeByIds(children.stream().map(Template::getId).collect(Collectors.toList()));
    }

    private void validateNode(Template tree, boolean isUpdate) {
        if (tree == null) {
            throw new IllegalArgumentException("tree.node.invalid");
        }
        if (StringUtils.isBlank(tree.getName())) {
            throw new IllegalArgumentException("tree.node.name.blank");
        }
        TemplateType templateType = TemplateType.fromValue(tree.getType());
        if (!templateType.isValid()) {
            throw new IllegalArgumentException("tree.node.type.invalid");
        }
        Long parentId = tree.getParentId();
        if (parentId == null) {
            tree.setParentId(0L);
        } else if (parentId > 0) {
            Template parent = this.getById(parentId);
            if (parent == null) {
                throw new IllegalArgumentException("tree.node.parent.not.found");
            }
            if (isUpdate) {
                ensureNoCycle(tree.getId(), parentId);
            }
        }
        LambdaQueryWrapper<Template> dupWrapper = Wrappers.<Template>lambdaQuery()
                .eq(Template::getParentId, tree.getParentId())
                .eq(Template::getName, tree.getName());
        if (isUpdate && tree.getId() != null) {
            dupWrapper.ne(Template::getId, tree.getId());
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
        Template parent = this.getById(parentId);
        if (parent != null) {
            ensureNoCycle(currentId, parent.getParentId());
        }
    }

    private List<Template> assembleTree(List<Template> nodes) {
        if (nodes.isEmpty()) {
            return nodes;
        }
        Map<Long, Template> indexed = nodes.stream()
                .collect(Collectors.toMap(Template::getId, Function.identity()));
        nodes.forEach(node -> node.setChildren(new ArrayList<>()));

        List<Template> roots = new ArrayList<>();
        for (Template node : nodes) {
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
