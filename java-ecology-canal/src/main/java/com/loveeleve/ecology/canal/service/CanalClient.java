package com.loveeleve.ecology.canal.service;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.protocol.Message;
import com.loveeleve.ecology.canal.config.CanalProperties;
import com.loveeleve.ecology.canal.handler.CanalEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Component
@EnableConfigurationProperties(CanalProperties.class)
public class CanalClient implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(CanalClient.class);

    private final CanalProperties properties;
    private final CanalEventHandler handler;
    private final AtomicLong processedEvents = new AtomicLong();
    private final AtomicLong lastBatchId = new AtomicLong(-1);
    private final AtomicReference<String> lastError = new AtomicReference<>();
    private volatile boolean running;
    private Thread worker;

    public CanalClient(CanalProperties properties, CanalEventHandler handler) {
        this.properties = properties;
        this.handler = handler;
    }

    @Override
    public void start() {
        if (!properties.isEnabled() || running) {
            return;
        }
        running = true;
        worker = new Thread(this::runLoop, "canal-client-" + properties.getDestination());
        worker.start();
    }

    private void runLoop() {
        while (running) {
            CanalConnector connector = null;
            try {
                connector = createConnector();
                connector.connect();
                connector.subscribe(properties.getFilter());
                log.info("canal client connected destination={} filter={}",
                        properties.getDestination(), properties.getFilter());
                while (running) {
                    Message message = connector.getWithoutAck(properties.getBatchSize(),
                            properties.getGetTimeoutMillis(), TimeUnit.MILLISECONDS);
                    long batchId = message.getId();
                    if (batchId == -1) {
                        continue;
                    }
                    try {
                        int handled = handler.handle(message, properties.getDestination());
                        connector.ack(batchId);
                        lastBatchId.set(batchId);
                        processedEvents.addAndGet(handled);
                        lastError.set(null);
                    } catch (Exception ex) {
                        connector.rollback(batchId);
                        throw ex;
                    }
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception ex) {
                lastError.set(ex.getClass().getSimpleName() + ": " + ex.getMessage());
                log.warn("canal client loop failed destination={}, reconnecting in {}ms",
                        properties.getDestination(), properties.getReconnectBackoffMillis(), ex);
                sleepBackoff();
            } finally {
                if (connector != null) {
                    connector.disconnect();
                }
            }
        }
    }

    private CanalConnector createConnector() {
        if (properties.isClusterEnabled()) {
            return CanalConnectors.newClusterConnector(properties.getZookeeperServers(),
                    properties.getDestination(), properties.getUsername(), properties.getPassword());
        }
        return CanalConnectors.newSingleConnector(
                new InetSocketAddress(properties.getHost(), properties.getPort()),
                properties.getDestination(), properties.getUsername(), properties.getPassword());
    }

    private void sleepBackoff() {
        try {
            Thread.sleep(properties.getReconnectBackoffMillis());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void stop() {
        running = false;
        if (worker != null) {
            worker.interrupt();
            try {
                worker.join(5000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            worker = null;
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    public long processedEvents() {
        return processedEvents.get();
    }

    public long lastBatchId() {
        return lastBatchId.get();
    }

    public String lastError() {
        return lastError.get();
    }
}
