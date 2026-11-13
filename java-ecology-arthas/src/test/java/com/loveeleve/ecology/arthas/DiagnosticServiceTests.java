package com.loveeleve.ecology.arthas;

import com.loveeleve.ecology.arthas.service.DiagnosticService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DiagnosticServiceTests {

    private final DiagnosticService service = new DiagnosticService();

    @Test
    void slowHotAndNestedMethodsAreCallable() {
        assertThat(service.slow(1)).isEqualTo("slow:1");
        assertThat(service.hotLoop(10)).isEqualTo(285);
        assertThat(service.nested("abc")).isEqualTo("nested:cba");
    }

    @Test
    void failProducesObservedException() {
        assertThatThrownBy(() -> service.fail("boom"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("boom");
    }
}
