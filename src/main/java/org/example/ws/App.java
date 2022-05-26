package org.example.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entrypoint for the application.
 */
public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        // Pull the developer ID and API Key from environment variables
        String devId = System.getenv("DEVELOPER_ID");
        String apiKey = System.getenv("API_KEY");

        // Ensure that both the developer ID and API Key were provided
        if (devId == null || apiKey == null) {
            logger.error("Could not fetch DEVELOPER_ID and API_KEY from environment variables. Aborting.");
            return;
        }
        // Create an instance of WebsocketClient and establish the websocket connection
        var client = new WebsocketClient(devId, apiKey);
        client.start();
        // Add a shutdown hook to cleanup the client on SIGINT
        Runtime.getRuntime().addShutdownHook(new ShutdownHook(client));
    }

    /**
     * Cleanup hook to ensure the websocket client is shutdown gracefully.
     */
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
