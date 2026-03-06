# ── 阶段一：构建（Maven + JDK 17）────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# 注入阿里云 Maven 镜像，解决国内服务器拉依赖慢的问题
COPY maven-settings.xml /root/.m2/settings.xml

# 先只拷贝 pom.xml，利用 Docker 层缓存预下载依赖（代码变化时无需重新下载）
COPY pom.xml .
RUN mvn dependency:go-offline -q

# 再拷贝源码并打包（跳过单测）
COPY src ./src
RUN mvn clean package -DskipTests -q

# ── 阶段二：运行（精简 JRE 17）──────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="leejim"
LABEL description="Work Diary Backend Service"

# 创建非 root 用户，提升安全性
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# 从构建阶段拷贝 JAR
COPY --from=builder /build/target/work-diary-server-*.jar app.jar

# 文件上传目录（本地存储策略时使用，COS 可忽略）
RUN mkdir -p /data/upload && chown -R appuser:appgroup /data /app

USER appuser

# 暴露服务端口
EXPOSE 8080

# JVM 参数：限制内存，适配小规格云服务器
ENTRYPOINT ["java", \
  "-Xms128m", "-Xmx512m", \
  "-XX:+UseContainerSupport", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
