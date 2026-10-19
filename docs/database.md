# 数据库设计文档 (作业 04 持久化阶段)

## 表结构说明

### 1. members (会员表)
| 字段名 | 类型 | 约束 | 说明 |
| :--- | :--- | :--- | :--- |
| id | BIGINT | 主键, 自增 | 会员唯一标识 |
| name | VARCHAR(255) | NOT NULL | 会员姓名 |
| phone | VARCHAR(255) | 无 | 联系电话 |
| remaining_sessions | INT | 无 | 剩余课时数 |

### 2. courses (课程表)
| 字段名 | 类型 | 约束 | 说明 |
| :--- | :--- | :--- | :--- |
| id | BIGINT | 主键, 自增 | 课程唯一标识 |
| name | VARCHAR(255) | NOT NULL | 课程名称 |
| coach | VARCHAR(255) | NOT NULL | 教练姓名 |
| capacity | INT | NOT NULL | 课程容量 |
| booked_count | INT | NOT NULL | 已预约人数 |

## 表关系说明
现阶段（单体架构）采用在 `BookingService` 中通过 `@Transactional` 注解来保证业务原子性。会员与课程通过“预约行为”产生业务关联。在后续微服务拆分时，此处将演进为独立的“预约记录表”或“订单表”。

## 测试数据准备方式
使用 Spring Data JPA 在 `BookingServiceTest.java` 中通过代码直接构建实体 (`new Member`, `new Course`) 并保存，实现数据准备。测试环境使用 H2 内存数据库，保证测试可重复执行。