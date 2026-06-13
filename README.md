<p align="center">
<img alt="logo" src="https://likede2-admin.itheima.net/img/logo.3673fab5.png" width="120">
</p>

<h1 align="center" style="margin: 30px 0 30px; font-weight: bold;">帝可得 - 智能售货机管理系统</h1>

<h4 align="center">基于 RuoYi-Vue（SpringBoot + Vue 前后端分离）的 Java 快速开发平台</h4>

---

## 背景介绍

智能售货机项目是随着互联网及物联网技术的普及及发展，运用现有技术对传统售货机进行改造升级，从 B 端角度来提升传统售货机的运营、运维效率，通过运营数据的采集和分析不断优化运营方案，降低运营、运维成本，缩短 B 端用户的盈利周期。针对不同的摆放点位及商业场景，匹配不同机型及不同商品供 B 端用户贴合自身特点来快速实现商业价值；针对 C 端用户的购物体验，将传统的纸币硬币购物流程替换成支付宝、微信、银联等线上扫码支付（或刷脸支付）等快捷支付方式。

业务模式分为：自营模式、加盟模式、点位主分成模式等。

## 业务介绍

随着售货机平台方运营售货机设备数量及点位数量越来越多，为了降低成本加快运营和运维效率将有限的资源迅速变现，平台方将系统做了切分：

- **运维客户端**：面向维修人员，跟踪和解决日常工作和设备故障
- **运营客户端**：提高日常运营效率，减少缺货设备，提高设备盈利能力
- **C 端用户客户端**：面向消费者，提高用户体验，缩短购物流程
- **平台管理端**：管理设备、货道、商品、工单及运营、运维数据
- **合作商后台**：针对有大量点位但没有运营能力的用户提供数据接入和销售分成管理

## 技术栈

| 技术 | 说明 | 版本 |
| --- | --- | --- |
| Spring Boot | 应用开发框架 | 2.5.15 |
| Spring Security | 安全认证与授权 | 2.5.15 |
| MyBatis | ORM 框架 | 3.5.x |
| Druid | 数据库连接池 | 1.2.20 |
| Redis | 缓存中间件 | - |
| JWT | Token 认证 | 0.9.1 |
| FastJSON2 | JSON 解析 | 2.0.43 |
| Swagger | API 文档 | 3.0.0 |
| PageHelper | MyBatis 分页插件 | 1.4.7 |
| POI | Excel 导入导出 | 4.1.2 |
| Velocity | 代码生成模板引擎 | 2.3 |
| Aliyun OSS | 阿里云对象存储 | - |
| Kaptcha | 验证码 | 2.3.3 |

## 模块说明

```
dkd-parent
├── dkd-admin        # 后台管理入口模块（Controller、配置文件）
├── dkd-framework    # 核心框架模块（安全配置、AOP、数据源、拦截器）
├── dkd-system       # 系统管理模块（用户、角色、菜单、部门、字典等）
├── dkd-manage       # 业务管理模块（售货机、货道、商品、工单、合作商等）
├── dkd-quartz       # 定时任务模块
├── dkd-generator    # 代码生成模块
└── dkd-common       # 通用工具模块（注解、常量、异常、工具类）
```

### dkd-manage 核心业务

| 业务 | 说明 |
| --- | --- |
| VendingMachine | 售货机管理 |
| Channel | 货道管理 |
| Sku / SkuClass | 商品 / 商品分类管理 |
| Node | 点位管理 |
| Region | 区域管理 |
| Partner | 合作商管理 |
| Emp | 员工管理 |
| Task / TaskDetails | 工单 / 工单详情管理 |
| TaskType | 工单类型管理 |
| Order | 订单管理 |
| Policy / Role | 策略 / 角色管理 |
| VmType | 售货机类型管理 |

## 运行环境

- JDK 11+
- MySQL 5.7+ / 8.0
- Redis 5.0+
- Maven 3.6+

## 快速开始

1. **导入数据库**：执行 SQL 脚本创建数据库及表结构
2. **修改配置**：编辑 `dkd-admin/src/main/resources/application-druid.yml`，配置数据库连接信息；编辑 `application.yml`，配置 Redis 及阿里云 OSS 等参数
3. **构建项目**：
   ```bash
   mvn clean package -DskipTests
   ```
4. **运行项目**：
   ```bash
   java -jar dkd-admin/target/dkd-admin.jar
   ```
5. **访问系统**：浏览器打开 `http://localhost:8080`

## 许可证

本项目基于 [LICENSE](LICENSE) 协议开源。
