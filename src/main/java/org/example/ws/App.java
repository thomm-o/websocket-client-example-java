package org.example.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        String devId = System.getenv("DEVELOPER_ID");
        String apiKey = System.getenv("API_KEY");

        if (devId == null || apiKey == null) {
            logger.error("Could not fetch DEVELOPER_ID and API_KEY from environment variables. Aborting.");
            return;
        }

        var client = new WebsocketClient(devId, apiKey);
        client.start();

        Runtime.getRuntime().addShutdownHook(new ShutdownHook(client));
    }

    static class ShutdownHook extends Thread {
        private final Logger logger = LoggerFactory.getLogger(ShutdownHook.class);
        private final WebsocketClient client;

        public ShutdownHook(WebsocketClient client) {
            this.client = client;
        }

        @Override
        public void run() {
            logger.info("Shutting down");
            this.client.stop();
        }
    }
}
