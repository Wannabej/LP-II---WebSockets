package com.zoomsockets.server.command;

import java.io.File;
import java.io.FileOutputStream;

public class FileTransferContext {
    private FileOutputStream fileOutputStream;
    private String nombreArchivoRecibiendo;
    private File archivoTemporal;

    public FileOutputStream getFileOutputStream() {
        return fileOutputStream;
    }

    public void setFileOutputStream(FileOutputStream fileOutputStream) {
        this.fileOutputStream = fileOutputStream;
    }

    public String getNombreArchivoRecibiendo() {
        return nombreArchivoRecibiendo;
    }

    public void setNombreArchivoRecibiendo(String nombreArchivoRecibiendo) {
        this.nombreArchivoRecibiendo = nombreArchivoRecibiendo;
    }

    public File getArchivoTemporal() {
        return archivoTemporal;
    }

    public void setArchivoTemporal(File archivoTemporal) {
        this.archivoTemporal = archivoTemporal;
    }

    public void cleanup() {
        if (fileOutputStream != null) {
            try {
                fileOutputStream.close();
            } catch (Exception e) {
                // ignored
            }
            fileOutputStream = null;
        }
        nombreArchivoRecibiendo = null;
        archivoTemporal = null;
    }
}
