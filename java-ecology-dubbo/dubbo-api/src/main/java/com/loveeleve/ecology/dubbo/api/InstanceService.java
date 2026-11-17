package com.loveeleve.ecology.dubbo.api;

/**
 * 用于验证多实例负载均衡的轻量诊断契约。
 *
 * <p>生产环境建议只在内网管理面暴露，并加鉴权/访问控制。</p>
 */
public interface InstanceService {

    String instance();
}
