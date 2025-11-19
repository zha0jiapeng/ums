package com.global.ums.dto;

import lombok.Data;

import java.util.List;

/**
 * 批量获取用户属性请求DTO
 */
@Data
public class BatchGetKeysRequestDTO {

    /**
     * 属性键列表
     */
    private List<KeyDTO> keys;

    /**
     * 属性键DTO
     */
    @Data
    public static class KeyDTO {
        /**
         * 属性键名
         */
        private String key;
    }
}
