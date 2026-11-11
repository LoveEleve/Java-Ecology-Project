package com.loveeleve.ecology.extension;

import com.loveeleve.ecology.extension.autoconfigure.EcologyAutoConfigurationImportFilter;
import com.loveeleve.ecology.extension.autoconfigure.EcologyExtensionAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurationMetadata;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class EcologyAutoConfigurationImportFilterTests {

    @Test
    void filtersExtensionAutoConfigurationWhenDisabled() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("ecology.extension.enabled", "false");
        EcologyAutoConfigurationImportFilter filter = new EcologyAutoConfigurationImportFilter();
        filter.setEnvironment(environment);

        boolean[] matches = filter.match(
                new String[]{EcologyExtensionAutoConfiguration.class.getName(), "example.OtherAutoConfiguration"},
                mock(AutoConfigurationMetadata.class));

        assertThat(matches).containsExactly(false, true);
    }

    @Test
    void keepsExtensionAutoConfigurationWhenEnabled() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("ecology.extension.enabled", "true");
        EcologyAutoConfigurationImportFilter filter = new EcologyAutoConfigurationImportFilter();
        filter.setEnvironment(environment);

        boolean[] matches = filter.match(
                new String[]{EcologyExtensionAutoConfiguration.class.getName()},
                mock(AutoConfigurationMetadata.class));

        assertThat(matches).containsExactly(true);
    }
}
