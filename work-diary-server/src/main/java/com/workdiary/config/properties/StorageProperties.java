package com.workdiary.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 文件上传相关配置映射属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "work-diary.storage")
public class StorageProperties {

    /**
     * 当前启用的存储类型: local, minio, oss, cos
     */
    private String type = "local";

    /**
     * 本地存储配置
     */
    private Local local = new Local();

    /**
     * MinIO存储配置
     */
    private Minio minio = new Minio();

    /**
     * 阿里云OSS存储配置
     */
    private Oss oss = new Oss();

    /**
     * 腾讯云COS存储配置
     */
    private Cos cos = new Cos();

    @Data
    public static class Local {
        // 本地存储物理根路径
        private String path = "/app/upload/";
        // 外部访问基础URL (含域名和端口)
        private String domain = "http://localhost:8080";
        // 访问前缀
        private String prefix = "/files";
    }

    @Data
    public static class Minio {
        private String endpoint;
        private String accessKey;
        private String secretKey;
        private String bucketName;
        private String domain; // 对外访问的域名(如果不配，默认使用endpoint构造)
    }

    @Data
    public static class Oss {
        private String endpoint;
        private String accessKey;
        private String secretKey;
        private String bucketName;
        private String domain; // 自定义绑定的域名
    }

    @Data
    public static class Cos {
        /** 存储桶所在地域，例如 ap-guangzhou */
        private String regionId;
        /** 云 API 密钥 SecretId */
        private String secretId;
        /** 云 API 密钥 SecretKey */
        private String secretKey;
        /** 存储桶名称，格式：BucketName-APPID，例如 work-diary-1234567890 */
        private String bucketName;
        /** 文件访问域名（CDN域名或默认域名，不填则自动拼接默认域名） */
        private String domain;
    }
}
