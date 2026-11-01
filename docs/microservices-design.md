# 微服务拆分设计

## 拆分映射
- 单体 `monolith` 中的 `Member` 模块拆分为 `user-service`。
- 单体中的 `Course` 模块拆分为 `course-service`。
- 单体中的 `BookingService` 跨服务业务逻辑拆分为 `booking-service`。

## 微服务职责与端口
| 服务名 | 端口 | 独立数据库 | 拥有的数据表 | 职责说明 |
| :--- | :--- | :--- | :--- | :--- |
| user-service | 8081 | user_db | members | 维护会员基础信息及课时余额 |
| course-service | 8082 | course_db | courses | 维护课程基础信息、容量及已预约人数 |
| booking-service | 8083 | booking_db | bookings | 预约流水，记录用户ID和课程ID |

## 服务间调用关系
`booking-service` 在接收到预约请求时，先通过 HTTP 调用 `user-service` 检查会员剩余课时，再调用 `course-service` 检查课程是否满员并扣减名额。跨服务仅保存对方 ID，不建外键，不跨库 JOIN。

## Mermaid 架构图
```mermaid
graph TD
    Client[前端请求] -->|"POST /api/book"| Booking[booking-service:8083]
    Booking -->|"GET /api/members/{id}/check"| User[user-service:8081]
    Booking -->|"GET /api/courses/{id}/check"| Course[course-service:8082]
    Booking -->|"POST /api/courses/{id}/book"| Course