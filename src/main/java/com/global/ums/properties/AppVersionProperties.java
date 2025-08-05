package com.global.ums.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 应用版本信息配置属性
 */
@Component
@ConfigurationProperties(prefix = "app.version")
public class AppVersionProperties {

    /**
     * 应用名称
     */
    private String name = "UMS User Management System";

    /**
     * 版本号
     */
    private String version = "1.0.0";

    /**
     * 构建时间
     */
    private String buildTime = "2024-01-01";

    /**
     * 作者信息
     */
    private String author = "Global Team";

    /**
     * 描述信息的国际化key
     */
    private String descriptionKey = "system.version.description";

    // Getter和Setter方法
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getBuildTime() {
        return buildTime;
    }

    public void setBuildTime(String buildTime) {
        this.buildTime = buildTime;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getDescriptionKey() {
        return descriptionKey;
    }

    public void setDescriptionKey(String descriptionKey) {
        this.descriptionKey = descriptionKey;
    }

    @Override
    public String toString() {
        return "AppVersionProperties{" +
                "name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", buildTime='" + buildTime + '\'' +
                ", author='" + author + '\'' +
                ", descriptionKey='" + descriptionKey + '\'' +
                '}';
    }
} 