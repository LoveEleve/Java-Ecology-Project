package com.loveeleve.ecology.sharding;

import com.loveeleve.ecology.sharding.model.Order;
import com.loveeleve.ecology.sharding.model.OrderItem;
import com.loveeleve.ecology.sharding.repository.OrderItemRepository;
import com.loveeleve.ecology.sharding.repository.OrderRepository;
import com.loveeleve.ecology.sharding.service.HintOrderService;
import com.loveeleve.ecology.sharding.service.ShardExpansionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ShardingOrderTests {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private HintOrderService hintOrderService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ShardExpansionService expansionService;

    @Autowired
    @Qualifier("ds0JdbcTemplate")
    private JdbcTemplate ds0;

    @Autowired
    @Qualifier("ds1JdbcTemplate")
    private JdbcTemplate ds1;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void cleanPhysicalTables() {
        for (String table : new String[]{"t_order_0", "t_order_1", "t_order_item_0", "t_order_item_1",
                "t_order_hint_0", "t_order_hint_1"}) {
            ds0.update("DELETE FROM " + table);
            ds1.update("DELETE FROM " + table);
        }
    }

    @Test
    void routesByUserIdAndOrderId() {
        orderRepository.insert(new Order(1, 1, 100, "CREATED"));
        orderRepository.insert(new Order(2, 2, 200, "CREATED"));
        orderRepository.insert(new Order(3, 1, 300, "CREATED"));
        orderRepository.insert(new Order(4, 2, 400, "CREATED"));

        assertThat(count(ds1, "t_order_1")).isEqualTo(2);
        assertThat(count(ds1, "t_order_0")).isZero();
        assertThat(count(ds0, "t_order_0")).isEqualTo(2);
        assertThat(count(ds0, "t_order_1")).isZero();
    }

    @Test
    void logicalQueriesAndUpdatesUseShardingKeys() {
        orderRepository.insert(new Order(11, 1, 1100, "CREATED"));
        orderRepository.insert(new Order(12, 1, 1200, "CREATED"));
        orderRepository.insert(new Order(21, 2, 2100, "CREATED"));

        assertThat(orderRepository.find(11, 1)).get()
                .extracting(Order::status).isEqualTo("CREATED");
        assertThat(orderRepository.findByUser(1))
                .extracting(Order::orderId)
                .containsExactly(11L, 12L);

        orderRepository.updateStatus(11, 1, "PAID");
        assertThat(orderRepository.find(11, 1)).get()
                .extracting(Order::status).isEqualTo("PAID");
    }

    @Test
    void bindingTableKeepsOrderAndItemOnTheSameRoute() {
        orderRepository.insert(new Order(61, 1, 6100, "CREATED"));
        orderItemRepository.insert(new OrderItem(601, 61, 1, "monitor", 1));

        assertThat(jdbcTemplate.queryForList("""
                        SELECT o.order_id, i.item_id
                        FROM t_order o
                        JOIN t_order_item i
                          ON o.order_id = i.order_id AND o.user_id = i.user_id
                        WHERE o.user_id = ?
                        """, 1))
                .hasSize(1)
                .allSatisfy(row -> {
                    assertThat(row.get("order_id")).isEqualTo(61L);
                    assertThat(row.get("item_id")).isEqualTo(601L);
                });
    }

    @Test
    void itemPrimaryKeyDiffersFromShardingKey() {
        orderItemRepository.insert(new OrderItem(101, 1, 1, "keyboard", 2));
        orderItemRepository.insert(new OrderItem(102, 2, 2, "mouse", 1));

        assertThat(count(ds1, "t_order_item_1")).isEqualTo(1);
        assertThat(count(ds0, "t_order_item_0")).isEqualTo(1);

        assertThat(orderItemRepository.findByOrderId(1, 1))
                .extracting(OrderItem::itemId)
                .containsExactly(101L);
        assertThat(orderItemRepository.findByItemId(101))
                .get().extracting(OrderItem::orderId).isEqualTo(1L);
    }

    @Test
    void hintForcesDatabaseAndTableRoute() {
        hintOrderService.insert(new Order(51, 99, 5100, "CREATED"), 0, 1);

        assertThat(count(ds0, "t_order_hint_1")).isEqualTo(1);
        assertThat(count(ds1, "t_order_hint_1")).isZero();
        assertThat(hintOrderService.find(51, 0, 1)).isPresent();
        assertThat(hintOrderService.find(51, 1, 1)).isEmpty();
    }

    @Test
    void expansionPlanComputesTargetAndShadowLocations() {
        assertThat(expansionService.plan(1, 31, 4, 4))
                .containsEntry("currentDatabase", "ds_1")
                .containsEntry("targetDatabase", "ds_1")
                .containsEntry("currentTable", "t_order_1")
                .containsEntry("targetTable", "t_order_3")
                .containsEntry("shadowTable", "t_order_shadow_3");
    }

    @Test
    void httpEndpointsUseLogicalTable() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":31,\"userId\":1,\"amount\":3100,\"status\":\"CREATED\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.orderId").value(31));

        mockMvc.perform(get("/api/orders/31").param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CREATED"));

        mockMvc.perform(put("/api/orders/31/status")
                        .param("userId", "1")
                        .param("status", "PAID"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAID"));

        mockMvc.perform(get("/api/orders").param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].orderId").value(31));

        mockMvc.perform(get("/api/sharding/plan").param("orderId", "31").param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.database").value("ds_1"))
                .andExpect(jsonPath("$.data.table").value("t_order_1"));
    }

    @Test
    void validatesOrderAndReturns404() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":0,\"userId\":0,\"amount\":0,\"status\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        mockMvc.perform(get("/api/orders/999999").param("userId", "1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
    }

    private int count(JdbcTemplate jdbcTemplate, String table) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
        return count == null ? 0 : count;
    }
}
