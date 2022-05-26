package org.example.ws.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.ws.WebsocketClient;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WebsocketPayload {
    @JsonProperty("op")
    private Opcode opcode;
    @JsonProperty("d")
    private JsonNode data = null;
    @JsonProperty("t")
    private String eventType = null;
    @JsonProperty("uid")
    private String userId = null;
    @JsonProperty("seq")
    private Long sequenceNumber = null;

    public WebsocketPayload(Opcode opcode, JsonNode data) {
        this.opcode = opcode;
        this.data = data;
    }

    public static WebsocketPayload newIdentifyPayload(String token) {
        var identifyData = WebsocketClient.getObjectMapper().createObjectNode();
        identifyData.put("token", token);
        identifyData.put("type", 1);

        return new WebsocketPayload(Opcode.IDENTIFY, identifyData);
    }

    public static WebsocketPayload newHeartbeatPayload() {
        return new WebsocketPayload(Opcode.HEARTBEAT, null);
    }
}
