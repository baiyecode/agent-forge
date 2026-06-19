# Agent Forge

Agent Forge是一个Spring Boot后端，用于生成、管理、预览、部署和下载由AI创建的Web应用程序。它结合了LangChain4j、LangGraph4j、基于Redis的聊天内存、MyBatis-Flex持久化、Selenium截图、图像/资源收集以及服务器发送事件，因此前端可以将代码生成进度实时推送给用户。

## Key Features

- 单页HTML、原生多文件输出以及Vue项目的AI代码生成。
- 通过服务器发送事件（SSE）进行流式生成。
- LangGraph4j工作流程，用于提示增强、路由、图像收集、代码生成、代码质量检查以及可选的项目构建。
- 应用管理API，用于创建、更新、删除、列出、特色应用、部署、预览和ZIP下载。
- 由Redis提供支持的用户/会话支持。
- 通过MyBatis-Flex实现MySQL持久化。
- 使用Redisson进行速率限制。
- 可选腾讯COS集成用于对象存储。
- 可选Pexels和DashScope集成，用于图像搜索和图像生成。
- 在根级一体化应用程序旁边，使用Dubbo + Nacos进行并行微服务布局。

## Table of Contents

- [Tech Stack](#tech-stack)
- [Repository Layout](#repository-layout)
- [Runtime Modes](#runtime-modes)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Database](#database)
- [API Overview](#api-overview)
- [Architecture](#architecture)
- [Code Generation Workflow](#code-generation-workflow)
- [Generated Files and Deployment](#generated-files-and-deployment)
- [Microservice Layout](#microservice-layout)




## Tech Stack

| Area | Technology |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot 3.5.x |
| API | Spring MVC, Reactor Flux, Server-Sent Events |
| Persistence | MySQL, MyBatis-Flex |
| Cache/session | Redis, Spring Session, Caffeine |
| Rate limiting | Redisson |
| AI orchestration | LangChain4j, LangGraph4j |
| AI model clients | OpenAI-compatible LangChain4j chat models, DashScope SDK |
| API docs | springdoc-openapi, Knife4j |
| Screenshots | Selenium, WebDriverManager, bundled ChromeDriver |
| Object storage | Tencent Cloud COS SDK |
| Utilities | Hutool, Lombok |
| Build tool | Maven |
| Microservices | Dubbo triple protocol, Nacos registry |

## Repository Layout

```text
.
|-- pom.xml                         # Root Spring Boot application build
|-- README.md
|-- sql/
|   `-- create_table.sql            # MySQL schema bootstrap script
|-- src/
|   |-- main/
|   |   |-- java/com/baiye/agentforge/
|   |   |   |-- AgentForgeApplication.java
|   |   |   |-- ai/                # LangChain4j AI services, guards, tools, stream models
|   |   |   |-- annotation/        # Auth annotations
|   |   |   |-- aop/               # Auth interceptor
|   |   |   |-- common/            # Standard response/request wrappers
|   |   |   |-- config/            # AI, Redis, CORS, JSON, COS configuration
|   |   |   |-- constant/          # App and user constants
|   |   |   |-- controller/        # REST and SSE controllers
|   |   |   |-- core/              # Code parsing, saving, and Vue project building
|   |   |   |-- exception/         # Error codes and global exception handling
|   |   |   |-- langgraph4j/       # Workflows, nodes, state, and tools
|   |   |   |-- mapper/            # MyBatis-Flex mappers
|   |   |   |-- model/             # Entities, DTOs, VOs, enums
|   |   |   |-- ratelimit/         # Redisson rate-limit annotation/aspect/config
|   |   |   |-- service/           # App, user, chat history, screenshot, download services
|   |   |   `-- utils/             # Filesystem, cache-key, screenshot, Spring helpers
|   |   `-- resources/
|   |       |-- application.yml    # Root application config
|   |       |-- mapper/            # MyBatis XML mappings
|   |       |-- prompt/            # System prompts used by AI services
|   |       `-- web_drivers/       # Bundled chromedriver.exe
|   `-- test/java/com/baiye/agentforge/
|       |-- ai/                    # AI service tests
|       |-- core/                  # Parser/facade tests
|       |-- langgraph4j/           # Workflow and tool tests
|       `-- utils/                 # Screenshot utility tests
`-- agent-forge-microservice/
    |-- pom.xml                    # Multi-module microservice parent
    |-- agent-forge-common/        # Shared config, exceptions, utilities, constants
    |-- agent-forge-model/         # Shared entities, DTOs, VOs, enums
    |-- agent-forge-client/        # Dubbo inner-service interfaces
    |-- agent-forge-user/          # User service application
    |-- agent-forge-app/           # App/chat/code-generation service application
    |-- agent-forge-ai/            # AI services and LangChain4j model wiring
    `-- agent-forge-screenshot/    # Screenshot service application
```

## Runtime Modes

| Mode | Path | Best For | Entry Point |
| --- | --- | --- | --- |
| Root application | `.` | Local development, tests, all-in-one backend | `com.baiye.agentforge.AgentForgeApplication` |
| Microservice modules | `agent-forge-microservice/` | Dubbo/Nacos service split | `AgentForgeUserApplication`, `AgentForgeAppApplication`, `AgentForgeScreenshotApplication` |

Start with the root application unless you specifically need the Dubbo/Nacos split.

## Prerequisites

- JDK 21。
- Maven 3.9+。
  MySQL 8.x，或其他与MySQL兼容的数据库。
- Redis 6+版本。
- 如果您运行截图功能，请使用Chrome或Chromium。
- 可选：微服务模块使用Nacos 2.x。
- 可选：OpenAI兼容聊天模型、DashScope、Pexels和腾讯COS的API密钥。

## Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/baiyecode/agent-forge.git
cd agent-forge
```

### 2. Start MySQL and Redis

The checked-in local config expects:

| Service | Default |
| --- | --- |
| MySQL host | `localhost` |
| MySQL port | `3306` |
| Database | `agent_forge` |
| MySQL username | `root` |
| MySQL password | `1234` |
| Redis host | `localhost` |
| Redis port | `6379` |
| Redis database | `0` |

如果您的本地服务使用不同的配置，请调整`src/main/resources/application.yml`。

### 3. Initialize the Database

```bash
mysql -u root -p < sql/create_table.sql
```

该脚本会创建`agent_forge`数据库以及以下表格：

| Table | Purpose |
| --- | --- |
| `user` | 账户、密码、个人资料、角色和软删除元数据 |
| `app` | 生成的应用元数据、提示、生成类型、部署密钥、所有者 |
| `chat_history` | 应用程序中面向用户的聊天消息 |
| `chat_history_original` | 包含工具请求/结果消息类型的完整内存日志 |

### 4. Configure AI Providers

src/main/resources/application.yml 中的 AI 提供程序块默认已被注释掉。若要运行实际的代码生成，请启用相关的 langchain4j.open-ai.* 部分并设置 API 密钥。


## Database

该项目使用MyBatis-Flex实体和XML映射器。

| Concern | Files |
| --- | --- |
| Entities | `src/main/java/com/baiye/agentforge/model/entity/` |
| DTOs | `src/main/java/com/baiye/agentforge/model/dto/` |
| VOs | `src/main/java/com/baiye/agentforge/model/vo/` |
| Mapper interfaces | `src/main/java/com/baiye/agentforge/mapper/` |
| Mapper XML | `src/main/resources/mapper/` |
| Schema script | `sql/create_table.sql` |



## API Overview

All root-application endpoints are under `http://localhost:8123/api`.

### User APIs

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/user/register` | Register a user |
| `POST` | `/user/login` | Log in and establish a session |
| `GET` | `/user/get/login` | Get current logged-in user |
| `POST` | `/user/logout` | Log out |
| `POST` | `/user/add` | Admin-style user creation |
| `GET` | `/user/get` | Get raw user by id |
| `GET` | `/user/get/vo` | Get user view object by id |
| `POST` | `/user/delete` | Delete user |
| `POST` | `/user/update` | Update user |
| `POST` | `/user/list/page/vo` | Paginated user list |

### App APIs

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/app/add` | Create an app record |
| `POST` | `/app/update` | Update the current user's app |
| `POST` | `/app/delete` | Delete the current user's app |
| `GET` | `/app/get/vo` | Get app details |
| `POST` | `/app/my/list/page/vo` | List current user's apps |
| `POST` | `/app/good/list/page/vo` | List featured apps; cached for early pages |
| `GET` | `/app/chat/gen/code` | Stream AI code generation via SSE |
| `POST` | `/app/deploy` | Deploy generated code to a preview directory |
| `GET` | `/app/download/{appId}` | Download generated source as ZIP |
| `POST` | `/app/admin/delete` | Admin delete |
| `POST` | `/app/admin/update` | Admin update |
| `POST` | `/app/admin/list/page/vo` | Admin paginated list |
| `GET` | `/app/admin/get/vo` | Admin app detail |


### Chat History APIs

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/chatHistory/app/{appId}` | Get chat history for an app |
| `POST` | `/chatHistory/admin/list/page/vo` | Admin paginated history list |

### Static Preview APIs

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/static/{deployKey}/**` | Serve deployed generated app files from `tmp/code_output` |


### Workflow Demo APIs

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/workflow/execute` | Run the code-generation workflow synchronously |
| `GET` | `/workflow/execute-flux` | Stream workflow execution as Flux/SSE text |
| `GET` | `/workflow/execute-sse` | Stream workflow execution with `SseEmitter` |

## Architecture

### 请求生命周期


1. 客户端调用位于 /api 下的一个端点。 
2. Spring MVC 将请求路由到控制器。 
3. 会话/身份验证拦截器会在需要时解析当前用户。 
4. 控制器验证参数，并将任务委托给服务。 
5. 服务使用MyBatis-Flex映射器进行MySQL持久化，并使用Redis处理会话/缓存/限流问题。 
6. AI工作流程调用LangChain4j服务和LangGraph4j节点。 
7. 代码输出经过解析后，保存在tmp/code_output目录下，可选择进行部署，并流式传输回客户端。

### Core Layers

| Layer | Responsibility |
| --- | --- |
| `controller` | REST endpoints, SSE endpoints, request validation |
| `service` | Business logic, ownership checks, app generation/deployment/download |
| `mapper` | MyBatis-Flex persistence |
| `model` | Entity, DTO, VO, enum definitions |
| `ai` | LangChain4j service interfaces, model factories, tool calling, guardrails |
| `langgraph4j` | Workflow orchestration, node graph, workflow state |
| `core/parser` | Parse generated code from AI responses |
| `core/saver` | Persist generated HTML, multi-file, and Vue project outputs |
| `core/builder` | Build generated Vue projects |
| `ratelimit` | Redisson-backed rate limiting annotation and aspect |
| `utils` | Screenshot, directory, cache, and Spring helper utilities |

## Code Generation Workflow

Agent Forge支持三种生成类型：:

| Value | Meaning |
| --- | --- |
| `html` | Native single-file HTML mode |
| `multi_file` | Native multi-file mode |
| `vue_project` | Vue project mode |

`CodeGenWorkflow`中的标准工作流程为：

```text
START
  -> image_collector
  -> prompt_enhancer
  -> router
  -> code_generator
  -> code_quality_check
      -> project_builder -> END     # Vue project when quality passes
      -> END                         # HTML or multi-file when quality passes
      -> code_generator              # retry when quality fails
```

`CodeGenConcurrentWorkflow`中的并发工作流并行化资源收集：

```text
START
  -> image_plan
      -> content_image_collector
      -> illustration_collector
      -> diagram_collector
      -> logo_collector
  -> image_aggregator
  -> prompt_enhancer
  -> router
  -> code_generator
  -> code_quality_check
  -> project_builder or END
```

图状态保存在WorkflowContext中，该上下文跟踪原始提示、增强提示、生成类型、收集的资源、生成的代码、质量结果和输出路径。

## Generated Files and Deployment

生成的代码是相对于当前工作目录编写的：

| Constant | Path | Purpose |
| --- | --- | --- |
| `CODE_OUTPUT_ROOT_DIR` | `tmp/code_output` | Generated source files |
| `CODE_DEPLOY_ROOT_DIR` | `tmp/code_deploy` | Deployment destination |
| `CODE_DEPLOY_HOST` | `http://localhost` | Base host used when building deploy URLs |

生成的源目录根据生成类型和应用程序ID命名，例如：

```text
tmp/code_output/html_1
tmp/code_output/multi_file_2
tmp/code_output/vue_project_3
```

下载内容由 `/app/download/{appId}` 提供。ZIP构建器会过滤掉常见的生成内容或敏感内容：

```text
node_modules, .git, dist, build, .DS_Store, .env, target, .mvn, .idea, .vscode
*.log, *.tmp, *.cache
```

静态预览从 `/api/static/{deployKey}/` 路径提供。

## Microservice Layout

微服务拆分

| Module | Purpose |
| --- | --- |
| `agent-forge-common` | Shared response wrappers, exceptions, constants, utilities, config |
| `agent-forge-model` | Shared entities, DTOs, VOs, enums |
| `agent-forge-client` | Inner service interfaces used by Dubbo consumers/providers |
| `agent-forge-user` | User APIs and user Dubbo provider |
| `agent-forge-app` | App/chat/generation APIs and Dubbo consumers |
| `agent-forge-ai` | AI services, models, prompt handling, guardrails, tools |
| `agent-forge-screenshot` | Screenshot service and Dubbo provider |

### Microservice Ports

| Service | HTTP Port | Dubbo Protocol Port |
| --- | --- | --- |
| `agent-forge-user` | `8124` | `50051` |
| `agent-forge-screenshot` | `8127` | `50052` |
| `agent-forge-app` | `8123` | `50053` |

