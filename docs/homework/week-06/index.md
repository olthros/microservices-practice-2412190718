# 作业 06：微服务拆分与跨服务调用

## 1. 服务拆分设计
- 将单体应用拆分为 `user-service` (8081)、`course-service` (8082) 和 `booking-service` (8083)。
- 每个服务拥有独立数据库，通过 `RestTemplate` 进行 HTTP 通信。
- 详细架构图参见根目录 `docs/microservices-design.md`。

## 2. 代码拆分
- 各个微服务均为独立的 Maven 工程，包含独立的 `pom.xml` 和 `src`。
- `booking-service` 在调用依赖服务时使用了 `try-catch` 进行异常捕获，确保当远端服务不可用时能返回明确的错误信息，不会导致本地数据脏写。
- 代码截图见 `screenshots/` 目录。

## 3. 本地编排
- 更新了根目录的 `compose.yaml`，配置了自定义网络 `gym-network` 和数据卷。
- 一键启动所有容器后，MySQL 状态为 `(healthy)`，三个微服务均为 `Up`。
- 容器间通过服务名（如 `mysql`）通信。

## 4. 验证
- 编写并运行了 `scripts/smoke-test.sh`。
- 成功验证了健康检查接口和正常跨服务预约（返回 `SUCCESS`）。
- 模拟依赖服务不可用（`docker stop user-service`），预约接口成功返回 `FAILED: remote service unavailable...`。
- 完整运行结果截图见 `screenshots/` 目录。