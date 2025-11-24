# Backend (Spring Boot)

## 项目简介
为 Android 客户端提供用户注册/登录等 REST 接口，连接 MySQL。

## 主要技术
- Spring Boot 3 + Kotlin
- Spring Data JPA (MySQL)
- Spring Security + JWT
- Validation (jakarta)

## 快速开始
```bash
# 设置环境变量 (可选)
export DB_USER=app_user
export DB_PASS=StrongPass!123
export JWT_SECRET=ReplaceWithLongRandomString

# 构建与运行
./gradlew bootRun
```

## 测试接口
```bash
# 注册
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"u1","password":"123456"}'

# 登录
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"u1","password":"123456"}'
```

返回登录结果中的 token 后可在后续接口加 Header：
```
Authorization: Bearer <token>
```

## 下一步
- 添加其它实体 (课程/作业等) 与 CRUD 接口
- 将 Android Retrofit baseUrl 指向 http://<server>:8080/
- 保存 token 至客户端 DataStore 并添加 OkHttp 拦截器

## 注意
- `ddl-auto: none` 假设数据库表已手动导入。
- 修改 application.yml 中的 `your_db_name`。
- 生产环境不要使用默认 secret 和弱密码。
