package com.workdiary.module.file.controller;

import com.workdiary.common.api.Result;
import com.workdiary.infrastructure.storage.FileStorageFactory;
import com.workdiary.infrastructure.storage.FileStorageStrategy;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
@Tag(name = "File Storage", description = "统一文件上传/下载接口")
public class FileController {

    private final FileStorageFactory fileStorageFactory;

    @Operation(summary = "单文件上传", description = "上传商单相关的截图等。底层会自动匹配本地/COS/OSS/MinIO策略。")
    @PostMapping("/upload")
    public Result<String> upload(
            @Parameter(description = "文件实体", required = true) @RequestParam("file") MultipartFile file) {
        String fileUrl = fileStorageFactory.getStrategy().upload(file);
        return Result.success(fileUrl, "上传成功");
    }

    @Operation(summary = "私有桶文件代理下载", description = "通过服务端代理下载私有存储桶中的文件。传入对象路径（key），例如：20240101/uuid.jpg")
    @GetMapping("/download")
    public void download(
            @Parameter(description = "对象路径，如 20240101/uuid.jpg", required = true) @RequestParam("key") String key,
            HttpServletResponse response) {

        if (!StringUtils.hasText(key)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        FileStorageStrategy.DownloadResult result = fileStorageFactory.getStrategy().download(key);

        String contentType = StringUtils.hasText(result.getContentType())
                ? result.getContentType()
                : "application/octet-stream";
        response.setContentType(contentType);

        if (result.getContentLength() > 0) {
            response.setContentLengthLong(result.getContentLength());
        }

        String encodedFilename = URLEncoder.encode(result.getFilename(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        response.setHeader("Content-Disposition", "inline; filename*=UTF-8''" + encodedFilename);

        try (InputStream in = result.getInputStream();
                OutputStream out = response.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            out.flush();
        } catch (IOException e) {
            log.error("文件代理下载写入响应失败: key={}", key, e);
        }
    }
}
