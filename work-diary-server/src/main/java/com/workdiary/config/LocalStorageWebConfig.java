package com.workdiary.config;

import com.workdiary.config.properties.StorageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 本地静态资源目录映射配置 (使得上传到本地的文件可以通过 URL 访问)
 */
@Configuration
public class LocalStorageWebConfig implements WebMvcConfigurer {

    @Autowired
    private StorageProperties storageProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        StorageProperties.Local localConfig = storageProperties.getLocal();

        // 如果开启的是本地存储模式，则映射虚拟路径到本机的物理绝对路径
        // 例如拦截 /files/** 的请求，映射到 file:/app/upload/
        String pathPattern = localConfig.getPrefix() + "/**";
        String physicsPath = "file:" + localConfig.getPath();

        if (!physicsPath.endsWith("/")) {
            physicsPath += "/";
        }

        registry.addResourceHandler(pathPattern)
                .addResourceLocations(physicsPath);
    }
}
