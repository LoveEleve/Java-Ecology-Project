package com.loveeleve.ecology.openfeign;

import com.loveeleve.ecology.openfeign.client.DownstreamClient;
import com.loveeleve.ecology.openfeign.client.DownstreamClientFallbackFactory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FallbackFactoryTests {

    @Test
    void fallbackFactoryReturnsSafeValues() {
        DownstreamClient fallback = new DownstreamClientFallbackFactory()
                .create(new IllegalStateException("provider unavailable"));
        assertThat(fallback.hello("ecology")).startsWith("fallback:hello:");
        assertThat(fallback.echo("hello").message()).startsWith("fallback:");
        assertThat(fallback.notFound()).isEqualTo("fallback:not-found");
        assertThat(fallback.slow(100)).isEqualTo("fallback:slow:100");
    }
}
