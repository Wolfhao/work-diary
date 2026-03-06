package com.workdiary.service.storage;

import com.workdiary.common.exception.ApiException;
import com.workdiary.config.properties.StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 文件存储策略分发工厂类
 */
@Component
@RequiredArgsConstructor
public class FileStorageFactory {

    private final Map<String, FileStorageStrategy> strategyMap;
    private final StorageProperties storageProperties;

    /**
     * 根据配置文件 (work-diary.storage.type) 动态获取对应的上传策略
     */
    public FileStorageStrategy getStrategy() {
        String type = storageProperties.getType();
        FileStorageStrategy strategy = strategyMap.get(type + "FileStorageStrategy");

        if (strategy == null) {
            throw new ApiException("未找到对应的文件存储策略: " + type);
        }

        return strategy;
    }
}
