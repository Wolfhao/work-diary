package com.workdiary.service.storage.impl;

import com.workdiary.common.exception.ApiException;
import com.workdiary.service.storage.FileStorageStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * MinIO 文件上传策略 (占位实现)
 */
@Slf4j
@Service("minioFileStorageStrategy")
public class MinioFileStorageStrategy implements FileStorageStrategy {

    @Override
    public String upload(MultipartFile file) {
        log.info("执行 MinIO 上传逻辑...");
        // TODO: 集成 io.minio:minio SDK 即可实现真实上传
        throw new ApiException("MinIO 上传暂未完全接入，请先使用本地存储或完善此处逻辑");
    }
}
