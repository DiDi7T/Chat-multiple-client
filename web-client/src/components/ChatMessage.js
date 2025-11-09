import React from 'react';

const ChatMessage = ({ message, isOwn }) => {
  return (
    <div className={`message ${isOwn ? 'own-message' : 'other-message'}`}>
      <div className="message-sender">{message.sender}</div>
      <div className="message-content">{message.content}</div>
      <div className="message-time">{message.timestamp}</div>
    </div>
  );
};

export default ChatMessage;