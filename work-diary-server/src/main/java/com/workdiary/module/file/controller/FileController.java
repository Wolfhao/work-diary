package com.workdiary.module.file.controller;

import com.workdiary.common.api.Result;
import com.workdiary.infrastructure.storage.FileStorageFactory;
import com.workdiary.infrastructure.storage.FileStorageStrategy;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
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


    /**
     * 飞书MD专用：万能路径图片预览
     * 格式：https://suntool.online/imageView/xxx/xxx/xxx.png
     * 任意层级 / 都支持
     */
    @Operation(summary = "万能路径图片预览（飞书MD专用）", description = "任意路径 /imageView/xxx/xxx.png 直接预览，支持无限层级目录")
    @GetMapping("/imageView/**")
    public void image(HttpServletRequest request, HttpServletResponse response) {

        // 1. 自动获取 /images/ 后面的完整路径（包含所有 /）
        String fullPath = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String encodedKey = fullPath.replaceFirst("/file/imageView/", "");

        String key = null;
        try {
            key = URLDecoder.decode(encodedKey, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.error("图片预览失败: key={}", key, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        log.info("最终文件key = {}", key); // 你会看到正确中文了！

        if (key == null || key.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        // 2. 完全沿用你现有的下载逻辑
        FileStorageStrategy.DownloadResult result = fileStorageFactory.getStrategy().download(key);

        // 3. 强制设置为图片类型（飞书必须）
        String contentType = result.getContentType();
        if (!contentType.startsWith("image/")) {
            // 如果不是图片，自动覆盖成 image/png（保证飞书能识别）
            contentType = "image/png";
        }
        response.setContentType(contentType);

        // 4. 长度
        if (result.getContentLength() > 0) {
            response.setContentLengthLong(result.getContentLength());
        }

        // 5. inline 预览模式（飞书必须）
        String encodedFilename = URLEncoder.encode(result.getFilename(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        response.setHeader("Content-Disposition", "inline; filename*=UTF-8''" + encodedFilename);

        // 6. 流写入（和你原来完全一样）
        try (InputStream in = result.getInputStream();
             ServletOutputStream out = response.getOutputStream()) {

            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            out.flush();

        } catch (Exception e) {
            log.error("图片预览失败: key={}", key, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
