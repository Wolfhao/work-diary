package com.workdiary.service.storage.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.UUID;
import com.workdiary.common.exception.ApiException;
import com.workdiary.config.properties.StorageProperties;
import com.workdiary.service.storage.FileStorageStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Date;

/**
 * 本地文件上传策略
 */
@Slf4j
@Service("localFileStorageStrategy")
@RequiredArgsConstructor
public class LocalFileStorageStrategy implements FileStorageStrategy {

    private final StorageProperties storageProperties;

    @Override
    public String upload(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ApiException("上传文件不能为空");
        }

        StorageProperties.Local localConfig = storageProperties.getLocal();
        String originalFilename = file.getOriginalFilename();
        String extName = FileUtil.extName(originalFilename);

        // 生成按天分类的新文件名: /20231015/uuid.png
        String dateDir = DateUtil.format(new Date(), "yyyyMMdd");
        String newFileName = UUID.fastUUID().toString(true) + "." + extName;
        String relativePath = dateDir + "/" + newFileName;

        // 绝对存储路径
        String absolutePath = localConfig.getPath() + relativePath;

        try {
            File dest = new File(absolutePath);
            if (!dest.getParentFile().exists()) {
                dest.getParentFile().mkdirs();
            }
            file.transferTo(dest);
            log.info("本地文件上传成功: {}", absolutePath);

            // 返回可直接访问的 URL 路径
            return localConfig.getDomain() + localConfig.getPrefix() + "/" + relativePath;
        } catch (IOException e) {
            log.error("本地文件上传异常", e);
            throw new ApiException("文件保存失败，请稍后重试");
        }
    }
}
