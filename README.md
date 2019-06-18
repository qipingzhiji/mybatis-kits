MyBatis-Kits
==========
MyBatis 增强工具包，实现了便捷的单表 CRUD，各种自定义条件查询，以及物理分页查询，单表查询甚至可以做到无 XML，像使用 PHP 操作数据库一样简单。

--------------------------------------------------

## 仓库地址
* 码云：https://gitee.com/blackfox/mybats-kits
* GitHub: https://github.com/yangjian102621/mybatis-kits
> Note: 会优先在码云更新，GitHub 只有发布版本的时候才会更新

## 最新版本

```xml
<dependency>
  <groupId>org.rockyang</groupId>
  <artifactId>mybatis-kits-core</artifactId>
  <version>1.5.1</version>
</dependency>
```

SpringBoot 项目接入方式
```xml
<dependency>
  <groupId>org.rockyang</groupId>
  <artifactId>mybatis-kits-spring-boot-starter</artifactId>
  <version>1.5.1</version>
</dependency>
```
> Note: SpringBoot 项目不需要再引入 mybatis-kits-core 了，只导入 mybatis-kits-spring-boot-starter 一个构件就 OK 了。

## 为什么使用 Mybatis-Kits?
1. 无侵入：Mybatis-Kits 在 Mybatis 的基础上进行扩展，只是内置了简单的CRUD, 并增强了查询功能，并未修改原生 API，支持所有 Mybatis 原生的特性，不会影响项目现有的 Mybatis 架构。
2. 功能精简：简单到只有你需要的功能，又强大到刚好能满足你的绝大多数业务需求。她不像其他 Mybatis 中间件那么强大，每种功能都提供了各种实现 API，她每种功能都只提供了一种（最优）实现，让你不用还去考虑使用哪个 
API 来实现当前的功能，体验不用选择的幸福。如果你刚好像我一样也有选择困难症的话，那她简直是为你量身订做。
3. 内置分页插件：基于Mybatis物理分页，开发者无需关心具体操作，配置好插件之后，写分页等同于写基本List查询
4. 接入简单：内置提供了 spring-boot-starter，让你 1 分钟接入 SpringBoot 项目。

## 文档 
暂时没有来得及编写，后期会补上，不过里面有 demo 项目，以及大量的单元测试，能够很快上手。

