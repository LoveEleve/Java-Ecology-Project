package com.loveeleve.ecology.dubbo.provider;

import com.loveeleve.ecology.dubbo.api.OrderRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DubboProviderTests {

    @Autowired
    private OrderServiceImpl orderService;

    @Test
    void providerBeanImplementsBusinessContract() {
        var created = orderService.create(new OrderRequest(1, 1, 100));
        assertThat(created.getStatus()).isEqualTo("CREATED");
        assertThat(orderService.get(1).getAmount()).isEqualTo(100);
        assertThat(orderService.ping("dubbo")).isEqualTo("pong:dubbo");
        assertThat(orderService.createAsync(new OrderRequest(2, 1, 200)))
                .isCompletedWithValueMatching(order -> order.getOrderId() == 2);
        assertThat(orderService.echoAttachment()).isEqualTo("missing");
    }
}
