package com.icesi.chatapp.Server.repository;

import com.icesi.chatapp.Model.Message;
import com.icesi.chatapp.Model.TextMessage;
import com.icesi.chatapp.Model.VoiceMessage;
import java.util.List;
import java.util.Optional;

public interface MessageRepository {
    void save(Message message);
    List<Message> findByRecipientId(String recipientId, int limit);
    Optional<Message> findById(String messageId);
}