package com.icesi.chatapp.Model;

import java.io.Serializable;

public class VoiceMessage extends Message implements Serializable {
    private static final long serialVersionUID = 1L;

    // Este será el nombre del archivo de audio en el servidor o una URL para descargarlo.
    // El contenido binario del audio no se envía directamente en este objeto de mensaje,
    // sino que se manejaría por separado como una subida de archivo.
    private String audioFileName;

    // Constructor vacío para Gson
    public VoiceMessage() {
        super();
        this.type = MessageType.VOICE_MESSAGE; // Asegurarse de que el tipo sea correcto
    }

    public VoiceMessage(String senderUsername, String recipientId, String audioFileName) {
        super(senderUsername, recipientId, MessageType.VOICE_MESSAGE);
        this.audioFileName = audioFileName;
    }

    // Constructor completo para cargar desde DB
    public VoiceMessage(String messageId, String senderUsername, String recipientId, long timestamp, String audioFileName) {
        super(messageId, senderUsername, recipientId, MessageType.VOICE_MESSAGE, timestamp);
        this.audioFileName = audioFileName;
    }

    // Getter
    public String getAudioFileName() {
        return audioFileName;
    }

    // Setter
    public void setAudioFileName(String audioFileName) {
        this.audioFileName = audioFileName;
    }

    @Override
    public String toString() {
        return "VoiceMessage{" +
                "messageId='" + messageId + '\'' +
                ", senderUsername='" + senderUsername + '\'' +
                ", recipientId='" + recipientId + '\'' +
                ", timestamp=" + timestamp +
                ", audioFileName='" + audioFileName + '\'' +
                '}';
    }
}