# week-09 API 网关、JWT 认证与网关鉴权

本次运行时间：2026-11-18，Docker Compose 全量启动。

## 1. API Gateway

实现思路：新增 services/gateway-service，使用 Spring Cloud Gateway Server WebFlux，监听 8080，作为统一入口。通过 Nacos 和 Spring Cloud LoadBalancer 使用 lb://服务名 转发，不写死容器 IP。

路由：

| 路由 ID | 路径 | 目标 |
| :--- | :--- | :--- |
| user-auth | /api/auth/** | lb://user-service |
| user-members | /api/members/** | lb://user-service |
| user-admin | /api/admin/** | lb://user-service |
| course-service | /api/courses/** | lb://course-service |
| booking-service | /api/book/** | lb://booking-service |

运行命令：

```bash
docker compose up -d --build
docker compose ps
curl -s http://localhost:8080/api/health
```

运行结果：

```json
{
  "status": "UP",
  "service": "gateway-service",
  "timestamp": "2026-11-18T00:25:33.554901763Z",
  "routes": ["user-auth", "user-members", "user-admin", "course-service", "booking-service"]
}
```

docker compose ps 中 gateway-service 为 Up，8080 端口已映射。Nacos 控制台 PERSONAL_GROUP 分组中可见 gateway-service、user-service、course-service、booking-service。

## 2. 认证服务

实现思路：user-service 新增 user_accounts 表和 BCrypt 密码哈希。POST /api/auth/login 校验账号密码后签发 HS256 JWT。JWT 包含 iss、sub、uid、role、iat、nbf、exp、jti。角色为 ADMIN 和 MEMBER。

测试账号：

| 用户名 | 密码 | 角色 |
| :--- | :--- | :--- |
| admin | root | ADMIN |
| member | root | MEMBER |

运行命令：

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"root"}'

docker exec gym-mysql mysql -uroot -proot -D user_db \
  -e "SELECT id,username,role,LEFT(password_hash,7) AS prefix,LENGTH(password_hash) AS len FROM user_accounts;"
```

运行结果：登录返回 200，accessToken 非空，role 为 ADMIN，expiresIn 为 7200。数据库查询结果：

```text
id  username  role    prefix   len
1   admin     ADMIN   $2a$10$  60
2   member    MEMBER  $2a$10$  60
```

password_hash 以 $2a$10$ 开头、长度为 60，说明保存的是 BCrypt 哈希而不是明文。

## 3. 网关鉴权

实现思路：网关全局过滤器对非公开路径校验 Bearer Token 的 HS256 签名、iss、exp、nbf，未登录返回 401。/api/admin/** 额外要求 ADMIN 角色，MEMBER 访问返回 403。转发前删除客户端传入的 X-User-Id、X-User-Name、X-User-Role 等身份头，验签通过后再写入可信值。

运行命令：

```bash
curl -i http://localhost:8080/api/admin/members
curl -i -H "X-User-Role: ADMIN" http://localhost:8080/api/admin/members
curl -i -H "Authorization: Bearer <ADMIN_TOKEN>" http://localhost:8080/api/admin/members
curl -i -H "Authorization: Bearer <MEMBER_TOKEN>" http://localhost:8080/api/admin/members
```

运行结果：

| 场景 | 状态码 |
| :--- | :--- |
| 不带 Token 访问 /api/admin/members | 401 |
| 伪造 X-User-Role: ADMIN | 401 |
| ADMIN Token 访问 /api/admin/members | 200 |
| MEMBER Token 访问 /api/admin/members | 403 |
| MEMBER Token 访问 /api/members/1/check | 200 |
| 篡改 Token | 401 |

下游防护：user-service 的 /api/admin/** 再次校验 JWT 和 ADMIN 角色，不信任 X-User-* 请求头。生产部署只发布网关 8080 端口，下游服务位于内部网络。

## 4. 测试脚本

实现思路：scripts/auth-test.sh 使用 curl 发起真实 HTTP 请求，覆盖健康检查、未登录、伪造头、错误密码、管理员登录、管理员访问、会员登录、会员越权、受保护接口、无 Token、篡改 Token，共 15 项断言。失败时退出码为 1。

运行命令：

```bash
bash scripts/auth-test.sh
```

运行结果：

```text
==================================================
 passed: 15, failed: 0
==================================================
AUTH TEST PASSED
```

脚本每次重新登录获取 Token，不依赖历史数据，可以重复运行。
