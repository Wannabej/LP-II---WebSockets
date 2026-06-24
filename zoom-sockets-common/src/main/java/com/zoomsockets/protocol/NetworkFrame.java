package com.zoomsockets.protocol;

public class NetworkFrame {
    private final String jsonHeader;
    private final byte[] binaryPayload;

    public NetworkFrame(String jsonHeader) {
        this(jsonHeader, null);
    }

    public NetworkFrame(String jsonHeader, byte[] binaryPayload) {
        this.jsonHeader = jsonHeader != null ? jsonHeader : "{}";
        this.binaryPayload = binaryPayload != null ? binaryPayload : new byte[0];
    }

    public String getJsonHeader() {
        return jsonHeader;
    }

    public byte[] getBinaryPayload() {
        return binaryPayload;
    }

    public boolean hasBinaryPayload() {
        return binaryPayload.length > 0;
    }
}
