package com.loveeleve.ecology.jraft.service;

import com.loveeleve.ecology.jraft.config.JraftProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties(JraftProperties.class)
public class JraftNodeLifecycle implements SmartLifecycle {

    private final JraftProperties properties;
    private JraftNode node;
    private volatile boolean running;

    public JraftNodeLifecycle(JraftProperties properties) {
        this.properties = properties;
    }

    @Override
    public void start() {
        if (!properties.isEnabled() || running) {
            return;
        }
        try {
            node = new JraftNode(properties.getDataPath(), properties.getGroupId(),
                    properties.getNodeId(), properties.getPeers());
            running = true;
        } catch (Exception ex) {
            throw new IllegalStateException("failed to start jraft node", ex);
        }
    }

    @Override
    public void stop() {
        running = false;
        if (node != null) {
            node.close();
            node = null;
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    public JraftNode node() {
        return node;
    }
}
