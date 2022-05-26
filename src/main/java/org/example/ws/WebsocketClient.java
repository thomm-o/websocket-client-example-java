package org.example.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import okhttp3.*;
import org.example.ws.models.Opcode;
import org.example.ws.models.TokenResponseBody;
import org.example.ws.models.WebsocketPayload;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class WebsocketClient extends WebSocketListener {
    private static final Logger logger = LoggerFactory.getLogger(WebsocketClient.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final OkHttpClient httpClient = new OkHttpClient();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final String wsConnectUrl = "wss://ws.tryterra.co/connect";
    private static final String wsAuthUrl = "https://ws.tryterra.co/auth/developer";

    private final AtomicBoolean expectingAck = new AtomicBoolean();

    private final String devId;
    private final String apiKey;

    private String identifyToken;
    private ScheduledFuture<?> heartbeatFuture = null;

    public WebsocketClient(String devId, String apiKey) {
        this.devId = devId;
        this.apiKey = apiKey;
    }

    static class HeartbeatRunnable implements Runnable {
        private static final Logger logger = LoggerFactory.getLogger(HeartbeatRunnable.class);

        private final WebSocket socket;
        private final AtomicBoolean expectingAck;

        public HeartbeatRunnable(WebSocket socket, AtomicBoolean expectingAck) {
            this.socket = socket;
            this.expectingAck = expectingAck;
        }

        @Override
        public void run() {
            if (expectingAck.get()) {
                logger.warn("Heartbeat window elapsed but previous ACK was not received");
            }

            expectingAck.set(true);
            sendPayloadAsJson(socket, WebsocketPayload.newHeartbeatPayload());
        }
    }

    public static ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    private String fetchAuthToken(){
        var request = new Request.Builder()
                .url(wsAuthUrl)
                .header("dev-id", devId)
                .header("x-api-key", apiKey)
                .post(RequestBody.create("", null))
                .build();

        try {
            Response response = httpClient.newCall(request).execute();
            if (response.body() == null) {
                throw new RuntimeException("Auth request body was empty at runtime");
            }
            TokenResponseBody body = objectMapper.readValue(response.body().string(), TokenResponseBody.class);
            return body.getToken();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SneakyThrows
    private static void sendPayloadAsJson(WebSocket socket, WebsocketPayload payload) {
        var stringPayload = objectMapper.writeValueAsString(payload);
        socket.send(stringPayload);
    }

    @Override
    public void onOpen(@NotNull WebSocket socket, @NotNull Response response) {
        logger.info("Websocket connection established");
    }

    @Override
    public void onClosed(@NotNull WebSocket socket, int code, @NotNull String reason) {
        logger.info("Websocket connection closed with code {} ({})", code, reason);
        this.stop();
    }

    @Override
    @SneakyThrows
    public void onMessage(@NotNull WebSocket socket, @NotNull String text) {
        logger.debug("Raw message received: {}", text);
        WebsocketPayload parsedPayload = objectMapper.readValue(text, WebsocketPayload.class);

        if (parsedPayload.getOpcode().equals(Opcode.HELLO)) {
            // Identify
            logger.info("Sending IDENTIFY and scheduling heartbeats");
            sendPayloadAsJson(socket, WebsocketPayload.newIdentifyPayload(this.identifyToken));
            // Begin heartbeating
            var heartbeatInterval = parsedPayload.getData().get("heartbeat_interval").asLong();
            heartbeatFuture = scheduler.scheduleAtFixedRate(
                    new HeartbeatRunnable(socket, expectingAck),
                    (int) (new Random().nextFloat() * heartbeatInterval), heartbeatInterval, TimeUnit.MILLISECONDS
            );
        } else if (parsedPayload.getOpcode().equals(Opcode.HEARTBEAT_ACK)) {
            expectingAck.set(false);
        } else if (parsedPayload.getOpcode().equals(Opcode.READY)) {
            logger.info("Connection is READY");
        } else if (parsedPayload.getOpcode().equals(Opcode.DISPATCH)) {
            logger.info("Received DISPATCH payload for user ID {} of type {}. Data is: {}",
                    parsedPayload.getUserId(), parsedPayload.getEventType(), parsedPayload.getData().toString()
            );
        }
    }

    public void start() {
        logger.info("Fetching authentication token");
        this.identifyToken = fetchAuthToken();
        logger.info("Creating websocket connection");
        var request = new Request.Builder()
                .url(wsConnectUrl)
                .build();
        httpClient.newWebSocket(request, this);
    }

    public void stop() {
        if (heartbeatFuture != null) {
            heartbeatFuture.cancel(true);
        }

        httpClient.dispatcher().executorService().shutdown();
        scheduler.shutdown();
    }
}
