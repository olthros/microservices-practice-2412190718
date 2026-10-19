# 作业 04：单体应用实现与持久化

## 业务建模
实现了“会员预约课程”核心流程，涉及 `Member` 和 `Course` 两个实体。
业务规则在 `BookingService` 中实现：会员课时不足或课程满员时，抛出异常阻断流程。

## 单体应用实现
- **Model**: `Member`, `Course`
- **Repository**: `MemberRepository`, `CourseRepository`
- **Service**: `BookingService`（包含事务控制和规则校验）
- **Controller**: `BookingController`（提供创建会员、创建课程、查询课程、预约接口，均以 `/api` 开头，使用 DTO `BookingRequest` 接收参数）。
- **运行结果**：通过 `curl` 调用，成功创建数据并完成预约。截图见 `screenshots/` 目录。

## 数据库持久化
- 已成功接入 MySQL，表结构详见根目录 `docs/database.md`。
- 使用了 Spring Data JPA 进行数据访问，配置写在 `application.yml` 中。
- 测试数据准备：在 `BookingServiceTest` 中使用 JPA 自动生成，未写 SQL 脚本。

## 测试与验证
- **测试命令**：在 `monolith/` 目录下执行 `./mvnw test`。
- **测试结果**：通过测试（BUILD SUCCESS）。覆盖了预约成功、课时不足异常、课程满员异常等场景。
- **截图证明**：测试成功日志和 API 响应截图均存放于 `screenshots/` 目录。