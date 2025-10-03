package com.icesi.chatapp.Server.repository;

import com.icesi.chatapp.Model.Message;
import com.icesi.chatapp.Model.MessageType;
import com.icesi.chatapp.Model.TextMessage;
import com.icesi.chatapp.Model.VoiceMessage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SQLiteMessageRepository implements MessageRepository {

    @Override
    public void save(Message message) {
        String sql = "INSERT INTO messages (message_id, sender_username, recipient_id, message_type, timestamp, content, audio_file_name) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, message.getMessageId());
            pstmt.setString(2, message.getSenderUsername());
            pstmt.setString(3, message.getRecipientId());
            pstmt.setString(4, message.getType().name()); // Guarda el nombre del enum
            pstmt.setLong(5, message.getTimestamp());

            if (message instanceof TextMessage) {
                TextMessage textMsg = (TextMessage) message;
                pstmt.setString(6, textMsg.getContent()); // Contenido de texto
                pstmt.setNull(7, java.sql.Types.VARCHAR); // audio_file_name es NULL
            } else if (message instanceof VoiceMessage) {
                VoiceMessage voiceMsg = (VoiceMessage) message;
                pstmt.setNull(6, java.sql.Types.VARCHAR); // content es NULL
                pstmt.setString(7, voiceMsg.getAudioFileName()); // Nombre del archivo de audio
            } else {
                pstmt.setNull(6, java.sql.Types.VARCHAR);
                pstmt.setNull(7, java.sql.Types.VARCHAR);
            }

            pstmt.executeUpdate();
            System.out.println("Message saved: " + message.getMessageId() + " type: " + message.getType());
        } catch (SQLException e) {
            System.err.println("Error saving message: " + e.getMessage());
        }
    }

    @Override
    public List<Message> findByRecipientId(String recipientId, int limit) {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT message_id, sender_username, recipient_id, message_type, timestamp, content, audio_file_name " +
                "FROM messages WHERE recipient_id = ? OR sender_username = ? ORDER BY timestamp DESC LIMIT ?"; // También incluye mensajes enviados POR el recipientId
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, recipientId);
            pstmt.setString(2, recipientId); // Para chats 1-1, queremos ver lo que envió y lo que recibió
            pstmt.setInt(3, limit);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String messageId = rs.getString("message_id");
                String senderUsername = rs.getString("sender_username");
                String retrievedRecipientId = rs.getString("recipient_id");
                MessageType type = MessageType.valueOf(rs.getString("message_type"));
                long timestamp = rs.getLong("timestamp");

                Message message = null;
                switch (type) {
                    case TEXT_MESSAGE:
                        message = new TextMessage(messageId, senderUsername, retrievedRecipientId, timestamp, rs.getString("content"));
                        break;
                    case VOICE_MESSAGE:
                        message = new VoiceMessage(messageId, senderUsername, retrievedRecipientId, timestamp, rs.getString("audio_file_name"));
                        break;
                    // Aquí puedes añadir otros tipos de mensaje si los persistes
                }
                if (message != null) {
                    messages.add(message);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding messages by recipient ID: " + e.getMessage());
        }
        return messages;
    }

    @Override
    public Optional<Message> findById(String messageId) {
        String sql = "SELECT message_id, sender_username, recipient_id, message_type, timestamp, content, audio_file_name FROM messages WHERE message_id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, messageId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String senderUsername = rs.getString("sender_username");
                String retrievedRecipientId = rs.getString("recipient_id");
                MessageType type = MessageType.valueOf(rs.getString("message_type"));
                long timestamp = rs.getLong("timestamp");

                switch (type) {
                    case TEXT_MESSAGE:
                        return Optional.of(new TextMessage(messageId, senderUsername, retrievedRecipientId, timestamp, rs.getString("content")));
                    case VOICE_MESSAGE:
                        return Optional.of(new VoiceMessage(messageId, senderUsername, retrievedRecipientId, timestamp, rs.getString("audio_file_name")));
                    default:
                        // Podrías manejar otros tipos aquí o lanzar una excepción si el tipo es inesperado
                        return Optional.empty();
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding message by ID: " + e.getMessage());
        }
        return Optional.empty();
    }
}