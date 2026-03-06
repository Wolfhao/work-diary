package com.workdiary.service.storage.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.region.Region;
import com.workdiary.common.exception.ApiException;
import com.workdiary.config.properties.StorageProperties;
import com.workdiary.service.storage.FileStorageStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

/**
 * 腾讯云 COS 文件上传策略
 * <p>
 * 配置示例（application-prod.yml）:
 * 
 * <pre>
 * work-diary:
 *   storage:
 *     type: cos
 *     cos:
 *       region-id: ap-guangzhou
 *       secret-id: your_secret_id
 *       secret-key: your_secret_key
 *       bucket-name: work-diary-1234567890
 *       domain:                          # 可选，填写自定义 CDN 域名；留空则自动拼接默认域名
 * </pre>
 */
@Slf4j
@Service("cosFileStorageStrategy")
@RequiredArgsConstructor
public class CosFileStorageStrategy implements FileStorageStrategy {

    private final StorageProperties storageProperties;

    @Override
    public String upload(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ApiException("上传文件不能为空");
        }

        StorageProperties.Cos cosConfig = storageProperties.getCos();
        validateConfig(cosConfig);

        // 初始化 COS 客户端（每次上传用完即关闭，避免连接泄漏；如需高并发可改为 Bean 单例）
        COSClient cosClient = buildClient(cosConfig);
        try {
            String objectKey = buildObjectKey(file);

            InputStream inputStream = file.getInputStream();
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());

            PutObjectRequest request = new PutObjectRequest(
                    cosConfig.getBucketName(), objectKey, inputStream, metadata);

            PutObjectResult result = cosClient.putObject(request);
            log.info("腾讯云 COS 上传成功: eTag={}, key={}", result.getETag(), objectKey);

            return buildAccessUrl(cosConfig, objectKey);
        } catch (CosClientException e) {
            log.error("腾讯云 COS 上传失败", e);
            throw new ApiException("文件上传失败（COS）：" + e.getMessage());
        } catch (IOException e) {
            log.error("读取上传文件流失败", e);
            throw new ApiException("文件读取失败，请重试");
        } finally {
            cosClient.shutdown();
        }
    }

    /**
     * 从私有桶下载文件，返回对象流供 Controller 转发给客户端。
     * <p>
     * 注意：调用方负责关闭 {@link DownloadResult#getInputStream()}，
     * 同时需要在流关闭后调用 {@code cosClient.shutdown()}。
     * 此处将 COSClient 生命周期与 COSObject 绑定，通过包装流在 close() 时一并关闭。
     * </p>
     */
    @Override
    public DownloadResult download(String objectKey) {
        StorageProperties.Cos cosConfig = storageProperties.getCos();
        validateConfig(cosConfig);

        COSClient cosClient = buildClient(cosConfig);
        try {
            GetObjectRequest getObjectRequest = new GetObjectRequest(cosConfig.getBucketName(), objectKey);
            COSObject cosObject = cosClient.getObject(getObjectRequest);

            ObjectMetadata meta = cosObject.getObjectMetadata();
            String contentType = meta.getContentType();
            long contentLength = meta.getContentLength();

            // 从 objectKey 中提取文件名（取最后一段路径）
            String filename = objectKey.contains("/")
                    ? objectKey.substring(objectKey.lastIndexOf('/') + 1)
                    : objectKey;

            // 包装流：在 close() 时同步关闭 COSObject 和 COSClient，避免资源泄漏
            InputStream wrappedStream = new java.io.FilterInputStream(cosObject.getObjectContent()) {
                @Override
                public void close() throws IOException {
                    try {
                        super.close();
                    } finally {
                        cosClient.shutdown();
                    }
                }
            };

            log.info("COS 私有桶下载: key={}, size={}", objectKey, contentLength);
            return new DownloadResult(wrappedStream, contentType, filename, contentLength);

        } catch (CosClientException e) {
            cosClient.shutdown();
            log.error("COS 下载失败: key={}", objectKey, e);
            throw new ApiException("文件下载失败：" + e.getMessage());
        }
    }

    // ─── 私有辅助方法 ────────────────────────────────────────────────────────────

    /**
     * 校验 COS 必填配置
     */
    private void validateConfig(StorageProperties.Cos config) {
        if (StrUtil.hasBlank(config.getRegionId(), config.getSecretId(),
                config.getSecretKey(), config.getBucketName())) {
            throw new ApiException("腾讯云 COS 配置不完整，请检查 work-diary.storage.cos 配置项");
        }
    }

    /**
     * 构建 COSClient 实例
     */
    private COSClient buildClient(StorageProperties.Cos config) {
        COSCredentials credentials = new BasicCOSCredentials(config.getSecretId(), config.getSecretKey());
        ClientConfig clientConfig = new ClientConfig(new Region(config.getRegionId()));
        return new COSClient(credentials, clientConfig);
    }

    /**
     * 生成 COS 对象路径：yyyyMMdd/uuid.ext
     */
    private String buildObjectKey(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        // 提取扩展名（hutool FileUtil 不依赖文件系统路径，直接用字符串处理）
        String ext = "";
        if (StrUtil.isNotBlank(originalFilename) && originalFilename.contains(".")) {
            ext = "." + originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
        }
        String dateDir = DateUtil.format(new Date(), "yyyyMMdd");
        return dateDir + "/" + UUID.fastUUID().toString(true) + ext;
    }

    /**
     * 私有桶场景：上传后直接返回本服务的代理下载路径。
     * <p>
     * 小程序端拼接方式：{@code config.baseUrl + accessUrl}，例如：
     * {@code https://api.example.com/file/download?key=20240101/uuid.jpg}
     * </p>
     * <p>
     * 若业务后续改为公有桶或 CDN，只需在此处改为返回 domain/objectKey 即可，
     * 上层代码和数据库结构无需变更。
     * </p>
     */
    private String buildAccessUrl(StorageProperties.Cos config, String objectKey) {
        // 配置了 CDN/自定义域名 且 桶为公开时，可切换为直链；私有桶保持代理下载路径
        if (StrUtil.isNotBlank(config.getDomain())) {
            String domain = config.getDomain().endsWith("/")
                    ? config.getDomain().substring(0, config.getDomain().length() - 1)
                    : config.getDomain();
            return domain + "/" + objectKey;
        }
        // 私有桶：返回本服务代理下载路径（相对路径，小程序端拼接 baseUrl 使用）
        return "/file/download?key=" + objectKey;
    }
}
