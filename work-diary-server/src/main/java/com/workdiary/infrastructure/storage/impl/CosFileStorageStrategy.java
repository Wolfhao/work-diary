package com.workdiary.infrastructure.storage.impl;

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
import com.workdiary.infrastructure.storage.FileStorageStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

/**
 * 腾讯云 COS 文件上传策略
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

            String filename = objectKey.contains("/")
                    ? objectKey.substring(objectKey.lastIndexOf('/') + 1)
                    : objectKey;

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

    private void validateConfig(StorageProperties.Cos config) {
        if (StrUtil.hasBlank(config.getRegionId(), config.getSecretId(),
                config.getSecretKey(), config.getBucketName())) {
            throw new ApiException("腾讯云 COS 配置不完整，请检查 work-diary.storage.cos 配置项");
        }
    }

    private COSClient buildClient(StorageProperties.Cos config) {
        COSCredentials credentials = new BasicCOSCredentials(config.getSecretId(), config.getSecretKey());
        ClientConfig clientConfig = new ClientConfig(new Region(config.getRegionId()));
        return new COSClient(credentials, clientConfig);
    }

    private String buildObjectKey(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (StrUtil.isNotBlank(originalFilename) && originalFilename.contains(".")) {
            ext = "." + originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
        }
        String dateDir = DateUtil.format(new Date(), "yyyyMMdd");
        return dateDir + "/" + UUID.fastUUID().toString(true) + ext;
    }

    private String buildAccessUrl(StorageProperties.Cos config, String objectKey) {
        if (StrUtil.isNotBlank(config.getDomain())) {
            String domain = config.getDomain().endsWith("/")
                    ? config.getDomain().substring(0, config.getDomain().length() - 1)
                    : config.getDomain();
            return domain + "/" + objectKey;
        }
        return "/file/download?key=" + objectKey;
    }
}
