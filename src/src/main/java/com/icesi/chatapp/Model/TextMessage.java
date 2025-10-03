package com.icesi.chatapp.Model;

import java.io.Serializable;

public class TextMessage extends Message implements Serializable {
    private static final long serialVersionUID = 1L;

    private String content;

    // Constructor vacío para Gson
    public TextMessage() {
        super();
        this.type = MessageType.TEXT_MESSAGE; // Asegurarse de que el tipo sea correcto
    }

    public TextMessage(String senderUsername, String recipientId, String content) {
        super(senderUsername, recipientId, MessageType.TEXT_MESSAGE);
        this.content = content;
    }

    // Constructor completo para cargar desde DB
    public TextMessage(String messageId, String senderUsername, String recipientId, long timestamp, String content) {
        super(messageId, senderUsername, recipientId, MessageType.TEXT_MESSAGE, timestamp);
        this.content = content;
    }

    // Getter
    public String getContent() {
        return content;
    }

    // Setter
    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "TextMessage{" +
                "messageId='" + messageId + '\'' +
                ", senderUsername='" + senderUsername + '\'' +
                ", recipientId='" + recipientId + '\'' +
                ", timestamp=" + timestamp +
                ", content='" + content + '\'' +
                '}';
    }
}