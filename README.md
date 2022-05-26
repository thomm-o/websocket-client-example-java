# Websocket Connection Example

This repository contains a basic implementation of the
[Terra websocket API](https://docs.tryterra.co/reference/using-the-websocket-api) for a consumer
(developer) connection to allow the receiving of real-time data from user devices.

This example is written using Java 11.

# Running the Example

Ensure that you have Java 11 installed before attempting to run the example.

Once you have cloned this repository locally, you can run the below command in
order to compile the source code with dependencies.

```shell
$ ./mvnw clean package
```

Once this step has completed, and you have set the `DEVELOPER_ID` and `API_KEY` environment variables
to contain your developer ID and API Key then you can execute the below command in order to run the example:

```shell
$ java -jar target/app-jar-with-dependencies.jar
```

If this was successfull, you should see something similar to the below logged in your console:

```
2022-05-26 18:20:28.496 INFO  org.example.ws.WebsocketClient - Fetching authentication token
2022-05-26 18:20:29.117 INFO  org.example.ws.WebsocketClient - Creating websocket connection
2022-05-26 18:20:29.265 INFO  org.example.ws.WebsocketClient - Websocket connection established
2022-05-26 18:20:29.290 DEBUG org.example.ws.WebsocketClient - Raw message received: {"op":2,"d":{"heartbeat_interval":40000}}
2022-05-26 18:20:29.303 INFO  org.example.ws.WebsocketClient - Sending IDENTIFY and scheduling heartbeats
2022-05-26 18:20:29.348 DEBUG org.example.ws.WebsocketClient - Raw message received: {"op":4}
2022-05-26 18:20:29.348 INFO  org.example.ws.WebsocketClient - Connection is READY
```
