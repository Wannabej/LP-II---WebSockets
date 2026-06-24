package com.zoomsockets.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ProtocolStreamer {
    
    // Límites de seguridad para evitar saturación de memoria por tramas corruptas
    private static final int MAX_JSON_SIZE = 1024 * 1024;      // 1 MB
    private static final int MAX_BINARY_SIZE = 16 * 1024 * 1024; // 16 MB

    /**
     * Escribe un NetworkFrame en el stream de salida.
     */
    public static synchronized void writeFrame(DataOutputStream out, NetworkFrame frame) throws IOException {
        byte[] jsonBytes = frame.getJsonHeader().getBytes(StandardCharsets.UTF_8);
        byte[] binaryBytes = frame.getBinaryPayload();

        // 1. Escribir tamaño de la cabecera JSON (4 bytes)
        out.writeInt(jsonBytes.length);
        // 2. Escribir cabecera JSON
        out.write(jsonBytes);

        // 3. Escribir tamaño del cuerpo binario (4 bytes)
        out.writeInt(binaryBytes.length);
        // 4. Escribir cuerpo binario si existe
        if (binaryBytes.length > 0) {
            out.write(binaryBytes);
        }
        
        out.flush();
    }

    /**
     * Lee un NetworkFrame desde el stream de entrada de forma síncrona.
     */
    public static NetworkFrame readFrame(DataInputStream in) throws IOException {
        // 1. Leer tamaño de la cabecera JSON (4 bytes)
        int jsonLength = in.readInt();
        if (jsonLength < 0 || jsonLength > MAX_JSON_SIZE) {
            throw new IOException("Tamaño de cabecera JSON inválido o sospechoso: " + jsonLength);
        }

        // 2. Leer bytes de la cabecera JSON
        byte[] jsonBytes = new byte[jsonLength];
        in.readFully(jsonBytes);
        String jsonHeader = new String(jsonBytes, StandardCharsets.UTF_8);

        // 3. Leer tamaño del cuerpo binario (4 bytes)
        int binaryLength = in.readInt();
        if (binaryLength < 0 || binaryLength > MAX_BINARY_SIZE) {
            throw new IOException("Tamaño de payload binario inválido o sospechoso: " + binaryLength);
        }

        // 4. Leer bytes del cuerpo binario si existe
        byte[] binaryBytes = new byte[binaryLength];
        if (binaryLength > 0) {
            in.readFully(binaryBytes);
        }

        return new NetworkFrame(jsonHeader, binaryBytes);
    }
}
