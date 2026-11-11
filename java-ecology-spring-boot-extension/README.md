# java-ecology-spring-boot-extension

Spring Boot 扩展点示例库。它不是业务 starter，而是演示“一个框架/组件如何扩展 Spring Boot”，供后续 Nacos、Dubbo、Redis、Sentinel 等 starter 参考。

## 定位

覆盖 Spring Boot 主要扩展点：

| 扩展点 | 实现 |
|---|---|
| 自动配置 | `EcologyExtensionAutoConfiguration` + `AutoConfiguration.imports` |
| 自定义条件 | `@ConditionalOnEcologyEnabled` + `SpringBootCondition` |
| 环境后处理 | `EcologyEnvironmentPostProcessor` + `ecology-defaults.properties` |
| 容器初始化 | `EcologyApplicationContextInitializer` |
| 启动监听 | `EcologyRunListener` |
| 启动 Runner | `ApplicationRunner` |
| 失败诊断 | `EcologyFailureAnalyzer` |
| BeanDefinition 注册 | `EcologyBeanDefinitionRegistryPostProcessor` |
| Bean 生命周期 | `EcologyBeanPostProcessor` |
| 模块导入 | `EcologyImportSelector` / `EcologyDeferredImportSelector` / `EcologyImportBeanDefinitionRegistrar` |
| 生命周期 | `EcologySmartLifecycle` |
| 应用事件 | `EcologyReadyListener` |
| Actuator | `EcologyEndpoint` / `EcologyHealthIndicator` / `EcologyInfoContributor` |
| 配置绑定 | `EcologyBindHandlerAdvisor` |
| 自动配置过滤 | `EcologyAutoConfigurationImportFilter` |
| 自动配置导入监听 | `EcologyAutoConfigurationImportListener` |

## 目录

```text
src/main/java/com/loveeleve/ecology/extension
├─ autoconfigure/  自动配置、ImportFilter
├─ actuator/       Endpoint、HealthIndicator、InfoContributor
├─ bind/           ConfigurationPropertiesBindHandlerAdvisor
├─ condition/      自定义条件注解与 SpringBootCondition
├─ env/            EnvironmentPostProcessor
├─ failure/        FailureAnalyzer
├─ imports/        ImportSelector、ImportBeanDefinitionRegistrar
├─ lifecycle/      Initializer、RunListener、SmartLifecycle、ApplicationListener
├─ processor/      BeanDefinitionRegistryPostProcessor、BeanPostProcessor
└─ properties/     @ConfigurationProperties
```

## 注册方式

Boot 3 的自动配置候选放在：

```text
META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

其余启动期扩展点仍通过 `META-INF/spring.factories` 注册：

- `org.springframework.boot.env.EnvironmentPostProcessor`
- `org.springframework.context.ApplicationContextInitializer`
- `org.springframework.boot.SpringApplicationRunListener`
- `org.springframework.boot.diagnostics.FailureAnalyzer`
- `org.springframework.boot.autoconfigure.AutoConfigurationImportFilter`

## 配置

```yaml
ecology:
  extension:
    enabled: true
    message: Spring Boot extension points are active
    bind-listener-enabled: true
```

默认值由 `ecology-defaults.properties` 通过 `EnvironmentPostProcessor` 注入；`application.yml` 中的显式配置优先级更高。

## 接入方式

Web 模块已经依赖本模块，启动后自动生效。其他应用只需引入：

```xml
<dependency>
    <groupId>com.loveeleve.ecology</groupId>
    <artifactId>java-ecology-spring-boot-extension</artifactId>
    <version>${project.version}</version>
</dependency>
```

如需暴露自定义 Actuator 端点：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,ecology
```

## 验证

```bash
export JAVA_HOME=/env/javaEnv/jdk17

./mvnw -pl java-ecology-spring-boot-extension -am test
./mvnw verify
```

Web 模块的 `dev` profile 已暴露 `/actuator/ecology`。

## 设计约束

- 扩展模块不写业务逻辑，不放 Controller。
- 扩展点必须可关闭、可回退，不默认改变业务行为。
- 自动配置使用 `@ConditionalOnMissingBean` / `@ConditionalOnProperty` 退让，不覆盖用户 Bean。
- `EnvironmentPostProcessor` 只追加最低优先级默认值，不覆盖应用配置。
- `RunListener` / `Initializer` / `FailureAnalyzer` 不能做重活，不能改变启动结果。
- `FailureAnalyzer` 只做异常翻译，不吞异常。
- Actuator 端点默认不暴露，由使用方显式配置。
- 不包含 banner、port-shift、对象存储等边角料 starter。
