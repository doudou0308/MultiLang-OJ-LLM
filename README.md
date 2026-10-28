# MultiLang-OJ-LLM

基于 Spring Boot + Vue 3 的多语言在线判题系统（Online Judge），支持 Java 和 C 语言算法刷题。

> **部署提示：**判题涉及编译和运行用户提交的代码。示例账号仅用于本地测试；在接收不可信代码或对外部署前，请先验证并完善进程隔离、权限与资源限制，并更换测试凭据。

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Java 8 + Spring Boot 2.7 + MyBatisPlus + Druid + JWT |
| 前端 | Vue 3 + TypeScript + Vite + Arco Design + Monaco Editor |
| 数据库 | MySQL 8.0 + Redis |
| 判题引擎 | ProcessBuilder（Java/C 本地编译执行） |
| API 文档 | Knife4j |

## 项目结构

```
exam-oj/                       # 后端 Maven 多模块项目
├── exam-common/               # 公共模块（R<T>, JWT, 异常处理）
├── exam-pojo/                 # 实体/DTO/VO
├── exam-server/               # Spring Boot 主服务
│   ├── controller/            # REST API
│   ├── service/               # 业务逻辑 + 判题引擎
│   ├── mapper/                # MyBatisPlus Mapper
│   └── config/                # Security, JWT, CORS 配置
├── schema.sql                 # 数据库建表脚本
└── init_data.sql              # 初始测试数据

antares-oj-frontend-master/    # 前端项目
└── src/
    ├── views/                 # 页面组件
    ├── api/                   # API 接口封装
    ├── router/                # 路由配置
    └── config/                # 全局配置
```

## 快速开始

### 环境要求

- JDK 8+
- Maven 3.6+
- MySQL 8.0 / Redis 7 (可选)
- Node 20 + pnpm
- GCC (C 语言判题需要)

### 后端启动

```bash
cd exam-oj
mysql -u root -p < schema.sql
mysql -u root -p < init_data.sql
mvn clean install -DskipTests
cd exam-server
mvn spring-boot:run
```

### 前端启动

```bash
cd antares-oj-frontend-master/antares-oj-frontend-master
pnpm install
pnpm dev
```

访问 http://localhost:8002

### 测试账号

| 角色 | 邮箱 | 密码 |
|------|------|------|
| 管理员 | admin@exam.com | 123456 |
| 学生 | test@exam.com | 123456 |

## 判题语言

| 语言 | 编译 | 运行 |
|------|------|------|
| Java | `javac Main.java` | `java Main` |
| C | `gcc main.c -o main` | `./main` |

## License

MIT
