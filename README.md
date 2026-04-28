# Exam System

多题库考试系统，当前版本内置“区块链应用操作员三级”题库，支持从指定 PDF 一次性导入题目。

## 技术栈

- 后端：Spring Boot 3、JDK 17、Spring Data JPA、Spring Security、JWT、MySQL、Flyway
- 前端：Vue 3、Vite、Element Plus、Pinia、Vue Router

## 默认账号

- 管理员用户名：`admin`
- 初始密码：`Admin@123`
- 首次登录必须修改密码，改密前不能访问管理功能。

## 启动

后端默认连接 `jdbc:mysql://localhost:3306/exam_system`，可通过环境变量覆盖：

```bash
cd backend
DB_URL='jdbc:mysql://localhost:3306/exam_system?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai' \
DB_USERNAME=root \
DB_PASSWORD=root \
mvn spring-boot:run
```

前端：

```bash
cd frontend
npm install
npm run dev
```

## 导入

管理员登录后，在管理页点击“导入当前 PDF”。系统会记录题库和文件指纹，重复导入同一文件会跳过。
