package com.loveeleve.ecology.netty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loveeleve.ecology.netty.client.NettyClientProperties;
import com.loveeleve.ecology.netty.client.NettyTcpClient;
import com.loveeleve.ecology.netty.codec.MessagePacket;
import com.loveeleve.ecology.netty.server.NettyServerProperties;
import com.loveeleve.ecology.netty.server.NettyTcpServer;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class NettyTcpClientTests {

    @Test
    void connectsAndCompletesRequestResponse() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        NettyServerProperties serverProperties = new NettyServerProperties();
        serverProperties.setPort(0);
        serverProperties.setIdleSeconds(30);
        NettyTcpServer server = new NettyTcpServer(serverProperties, objectMapper);
        server.start();

        NettyClientProperties clientProperties = new NettyClientProperties();
        clientProperties.setEnabled(true);
        clientProperties.setHost("127.0.0.1");
        clientProperties.setPort(server.getPort());
        clientProperties.setRequestTimeoutSeconds(3);
        NettyTcpClient client = new NettyTcpClient(clientProperties, objectMapper);
        try {
            client.start();
            for (int i = 0; i < 100 && !client.isConnected(); i++) {
                Thread.sleep(20);
            }
            assertThat(client.isConnected()).isTrue();

            MessagePacket response = client.send(new MessagePacket("ECHO", "client-1", "hello"))
                    .get(3, TimeUnit.SECONDS);
            assertThat(response).isEqualTo(new MessagePacket("ECHO", "client-1", "hello"));
        } finally {
            client.close();
            server.stop();
        }
    }
}
