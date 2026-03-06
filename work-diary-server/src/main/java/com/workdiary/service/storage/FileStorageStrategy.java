package com.workdiary.service.storage;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * 统一文件存储策略接口
 */
public interface FileStorageStrategy {

    /**
     * 上传文件
     *
     * @param file 接收到的文件流
     * @return 文件的可访问 URL
     */
    String upload(MultipartFile file);

    /**
     * 下载文件（私有桶场景，由服务端代理转发）
     *
     * @param objectKey 对象路径（如 20240101/uuid.jpg）
     * @return 包含文件流、Content-Type、文件名的下载结果
     */
    default DownloadResult download(String objectKey) {
        throw new UnsupportedOperationException("当前存储策略不支持服务端代理下载，请使用公开访问 URL");
    }

    /**
     * 下载结果包装
     */
    @Getter
    @AllArgsConstructor
    class DownloadResult {
        /** 文件输入流（调用方负责关闭） */
        private final InputStream inputStream;
        /** 文件 MIME 类型，例如 image/jpeg */
        private final String contentType;
        /** 文件名（含扩展名），用于 Content-Disposition */
        private final String filename;
        /** 文件大小（字节），-1 表示未知 */
        private final long contentLength;
    }
}
