FROM openjdk:8-jre

# 设置工作目录
WORKDIR /app

# 创建配置文件目录
RUN mkdir -p /app/config

# 复制已经编译好的jar包到容器中
# 注意：使用这个Dockerfile前需要先在本地执行 mvn clean package -DskipTests
COPY ums-0.0.1-SNAPSHOT.jar /app/app.jar

# 暴露应用端口
EXPOSE 8080

# 设置启动命令，直接在命令行指定数据库连接参数
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]