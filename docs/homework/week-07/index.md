# week-07 服务注册与发现

## 1. Nacos 部署与服务注册
- 镜像：nacos/nacos-server:v3.2.4，standalone 模式
- 端口映射：控制台 18080:8080，服务端 8848:8848，gRPC 9848:9848
- 鉴权：NACOS_AUTH_ENABLE=true，配置了 Token 密钥、Server Identity；首次启动后在控制台初始化 nacos 管理员密码
- 注册服务（分组 PERSONAL_GROUP）：user-service、course-service、booking-service

## 2. 跨服务发现调用
- 业务流程：POST /api/book?memberId=X&courseId=Y
- booking-service 每次调用均通过 DiscoveryClient.getInstances("user-service"/"course-service") 获取实例，再用实例 URI 发 HTTP 请求
- 发现的实例地址：（运行后填写，如 172.19.0.x:8081）
- 请求示例与结果：
    - POST http://localhost:8083/api/book?memberId=1&courseId=1 -> SUCCESS

## 3. 健康状态、下线与恢复
- 三个服务 /actuator/health 均为 UP
- docker stop user-service 后，Nacos 实例列表中 user-service 摘除，再次预约返回 503，且课程 bookedCount 未增加（未绕过校验写数据）
- docker start user-service 后，实例重新注册，预约恢复 SUCCESS
- 区别说明：Actuator /actuator/health 只反映本应用自身进程是否存活；SDK 连接是否存活是客户端到 Nacos 的连接状态；实例是否注册在 Nacos 列表中是注册中心视角。Actuator 返回 DOWN 不会自动让 Nacos 摘除实例。

## 4. 冒烟脚本
- 执行：bash scripts/smoke-test.sh
- 检查项：Nacos 控制台 18080、三个服务 /actuator/health，任一失败退出码非零
- 结果：全部通过，退出码 0
