package com.workdiary.service.storage.impl;

import com.workdiary.common.exception.ApiException;
import com.workdiary.service.storage.FileStorageStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 阿里云 OSS 文件上传策略 (占位实现)
 */
@Slf4j
@Service("ossFileStorageStrategy")
public class OssFileStorageStrategy implements FileStorageStrategy {

    @Override
    public String upload(MultipartFile file) {
        log.info("执行 阿里云OSS 上传逻辑...");
        // TODO: 集成 aliyun-sdk-oss SDK 即可实现真实上传
        throw new ApiException("阿里云 OSS 上传暂未完全接入，请先使用本地存储或完善此处逻辑");
    }
}
