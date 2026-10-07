# 作业 03：Spring Boot 工程搭建与验证

## 本周计划
1. 在仓库根目录创建 `monolith/` 模块，作为微服务演进前期的单体应用。
2. 使用 Spring Initializr 生成项目，配置 Java 25 与 Spring Boot 4.0.x，包名为 `com.zjgsu.czh.monolith`。
3. 添加 `/api/hello` 问候接口与 Spring Boot Actuator 健康检查。
4. 编写并运行 `@SpringBootTest` 上下文加载测试。
5. 更新 README 和项目提案文档。

## 启动与测试记录
### 测试命令与结果
命令：`./mvnw test`
结果：BUILD SUCCESS（上下文加载成功，无异常）

### 启动命令与结果
命令：`./mvnw spring-boot:run`
结果：应用在 8080 端口成功启动

### 接口响应记录
1. 访问 `http://localhost:8080/api/hello`
   响应：`Hello, 健身房会员管理系统!`
2. 访问 `http://localhost:8080/actuator/health`
   响应：`{"status":"UP"}`

## 截图说明
相关截图已保存在 `docs/homework/week-03/screenshots/` 目录中，包括工程结构、测试成功结果和接口访问结果。