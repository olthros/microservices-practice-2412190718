# 作业 05：Docker 镜像与 Compose 编排

## 1. Dockerfile 与镜像构建
成功编写了多阶段构建的 Dockerfile。构建阶段使用了 `maven:3.9-eclipse-temurin-25` 下载依赖并打包，运行阶段使用了轻量级 `eclipse-temurin:25-jre-alpine` 运行 JAR 包。构建命令为 `docker build -t my-project:week05 ./monolith`。

## 2. Compose 编排与启动
在根目录配置了 `compose.yaml`，定义了 `app` 和 `mysql:8.4.11` 两个服务。通过命名数据卷 `mysql_data` 持久化数据，并利用 `healthcheck` 确保数据库健康后应用才启动。敏感信息通过 `.env` 文件注入，执行 `docker compose up -d --build` 成功。

## 3. 业务 API 验证
容器启动后，访问 `/actuator/health` 返回 UP。通过 curl 发送 POST 请求创建会员数据，并成功通过 GET 请求查询。

## 4. 数据持久化验证
执行 `docker compose down` 销毁容器后，再次执行 `docker compose up -d` 启动。重新查询数据发现之前创建的会员记录依然存在，证明了命名数据卷（Named Volume）成功保障了 MySQL 数据的持久化。

## 截图证明
所有构建、运行、测试、验证截图均保存在 `docs/homework/week-05/screenshots/` 目录下。