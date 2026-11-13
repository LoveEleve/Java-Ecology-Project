package com.loveeleve.ecology.xxljob;

import com.loveeleve.ecology.xxljob.model.JobResult;
import com.loveeleve.ecology.xxljob.service.DemoJobService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DemoJobServiceTests {

    private final DemoJobService service = new DemoJobService();

    @Test
    void simpleJobReturnsSuccessOrFailure() {
        assertThat(service.simple("ok").success()).isTrue();
        JobResult failed = service.simple("fail");
        assertThat(failed.success()).isFalse();
        assertThat(failed.message()).contains("fail");
    }

    @Test
    void shardingJobProcessesOnlyItsOwnSlots() {
        assertThat(service.sharding(0, 3, 100).processed()).isEqualTo(34);
        assertThat(service.sharding(1, 3, 100).processed()).isEqualTo(33);
        assertThat(service.sharding(2, 3, 100).processed()).isEqualTo(33);
    }

    @Test
    void invalidShardIsRejected() {
        assertThat(service.sharding(-1, 3, 100).success()).isFalse();
        assertThat(service.sharding(3, 3, 100).success()).isFalse();
        assertThat(service.sharding(0, 0, 100).success()).isFalse();
    }
}
