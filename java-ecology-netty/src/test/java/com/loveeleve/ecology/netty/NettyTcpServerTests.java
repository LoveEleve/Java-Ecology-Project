package com.loveeleve.ecology.netty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loveeleve.ecology.netty.server.NettyServerProperties;
import com.loveeleve.ecology.netty.server.NettyTcpServer;
import org.junit.jupiter.api.Test;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class NettyTcpServerTests {

    @Test
    void startsOnEphemeralPortAndEchoesAFramedPacket() throws Exception {
        NettyServerProperties properties = new NettyServerProperties();
        properties.setPort(0);
        properties.setIdleSeconds(30);
        NettyTcpServer server = new NettyTcpServer(properties, new ObjectMapper());
        server.start();
        try (Socket socket = new Socket("127.0.0.1", server.getPort())) {
            socket.setSoTimeout(3000);
            byte[] body = "{\"type\":\"ECHO\",\"requestId\":\"it-1\",\"payload\":\"hello\"}"
                    .getBytes(StandardCharsets.UTF_8);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeInt(body.length);
            out.write(body);
            out.flush();

            DataInputStream in = new DataInputStream(socket.getInputStream());
            int length = in.readInt();
            byte[] response = new byte[length];
            in.readFully(response);
            assertThat(new String(response, StandardCharsets.UTF_8))
                    .contains("\"type\":\"ECHO\"")
                    .contains("\"requestId\":\"it-1\"")
                    .contains("\"payload\":\"hello\"");
        } finally {
            server.stop();
        }
    }
}
