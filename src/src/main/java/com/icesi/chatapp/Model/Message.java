package com.icesi.chatapp.Model;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID; // Para generar messageId únicos

public abstract class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    protected String messageId;
    protected String senderUsername;
    protected String recipientId; // Puede ser un username de usuario o un groupId
    protected MessageType type;
    protected long timestamp; // Unix timestamp en milisegundos

    // Constructor vacío para Gson
    public Message() {}

    public Message(String senderUsername, String recipientId, MessageType type) {
        this.messageId = UUID.randomUUID().toString(); // Genera un ID único para el mensaje
        this.senderUsername = senderUsername;
        this.recipientId = recipientId;
        this.type = type;
        this.timestamp = System.currentTimeMillis(); // Hora actual del sistema
    }

    // Constructor completo para cargar desde DB
    public Message(String messageId, String senderUsername, String recipientId, MessageType type, long timestamp) {
        this.messageId = messageId;
        this.senderUsername = senderUsername;
        this.recipientId = recipientId;
        this.type = type;
        this.timestamp = timestamp;
    }

    // Getters
    public String getMessageId() {
        return messageId;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public String getRecipientId() {
        return recipientId;
    }

    public MessageType getType() {
        return type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    // Setters (messageId y timestamp no se suelen cambiar después de la creación)
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }

    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message = (Message) o;
        return Objects.equals(messageId, message.messageId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageId);
    }

    @Override
    public String toString() {
        return "Message{" +
                "messageId='" + messageId + '\'' +
                ", senderUsername='" + senderUsername + '\'' +
                ", recipientId='" + recipientId + '\'' +
                ", type=" + type +
                ", timestamp=" + timestamp +
                '}';
    }
}