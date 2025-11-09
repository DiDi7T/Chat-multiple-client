import React, { useState, useEffect, useRef } from 'react';
import UserList from '../components/UserList';
import GroupList from '../components/GroupList';
import ChatMessage from '../components/ChatMessage';
import { chatAPI } from '../services/api';

const ChatRoom = ({ currentUser, onLogout }) => {
  const [messages, setMessages] = useState([]);
  const [newMessage, setNewMessage] = useState('');
  const [currentChat, setCurrentChat] = useState(null);
  const [users, setUsers] = useState([]);
  const [groups, setGroups] = useState([]);
  const [loading, setLoading] = useState(true);
  const [logoutLoading, setLogoutLoading] = useState(false);
  const [messageLoading, setMessageLoading] = useState(false);
  
  const currentChatRef = useRef(currentChat);
  const messagesEndRef = useRef(null);
  const lastMessageCountRef = useRef(0);

  const parseHistory = (historyOutput, currentUser) => {
    if (!historyOutput) return [];
    
    const messages = [];
    const lines = historyOutput.split('\n');
    const seenMessages = new Set();

    for (const line of lines) {
      if (line.includes('===') || line.includes('MENU') || 
          line.includes('Hola') || line.includes('No hay historial') || 
          line.trim() === '' || line.includes('Elige opción') ||
          line.includes('¿De qué usuario') || line.includes('FIN DEL HISTORIAL') ||
          line.includes('Usuarios disponibles')) {
        continue;
      }

      const privateMatch = line.match(/\[(.*?)\]\s*(.*?)\s*->\s*(.*?):\s*(.*)/);
      const groupMatch = line.match(/\[(.*?)\]\s*(.*?)\s*en\s*(.*?):\s*(.*)/);
      
      if (privateMatch) {
        const [, timestamp, sender, , content] = privateMatch;
        const messageKey = `${timestamp}-${sender}-${content}`;
        
        if (!seenMessages.has(messageKey)) {
          seenMessages.add(messageKey);
          messages.push({
            sender: sender.trim(),
            content: content.trim(),
            timestamp: timestamp,
            isOwn: sender.trim() === currentUser
          });
        }
      } else if (groupMatch) {
        const [, timestamp, sender, groupName, content] = groupMatch;
        const messageKey = `${timestamp}-${sender}-${content}`;
        
        if (!seenMessages.has(messageKey)) {
          seenMessages.add(messageKey);
          messages.push({
            sender: sender.trim(),
            content: content.trim(),
            timestamp: timestamp,
            isOwn: sender.trim() === currentUser,
            group: groupName.trim()
          });
        }
      }
    }

    return messages;
  };

  useEffect(() => {
    loadConnectedUsers();
    loadAllGroups();
    setLoading(false);
  }, [currentUser]);

  const loadConnectedUsers = async () => {
    try {
      const result = await chatAPI.getConnectedUsers(currentUser);
      if (result.success) {
        setUsers(result.data);
      } else {
        setUsers(['maria', 'carlos', 'ana', 'pedro']);
      }
    } catch (error) {
      console.error('Error loading users:', error);
      setUsers([]);
    }
  };

  const loadAllGroups = async () => {
    try {
      const result = await chatAPI.getUserGroups(currentUser);
      if (result.success) {
        setGroups(result.data);
        console.log('✅ Grupos cargados:', result.data);
      } else {
        setGroups([]);
      }
    } catch (error) {
      console.error('Error loading groups:', error);
      setGroups([]);
    }
  };

  const loadChatHistory = async (chat, isAutoRefresh = false) => {
    if (!chat) return;
    
    if (!isAutoRefresh) {
      setMessageLoading(true);
    }
    
    try {
      let result;
      if (chat.type === 'user') {
        result = await chatAPI.getPrivateHistory(currentUser, chat.name);
      } else {
        result = await chatAPI.getGroupHistory(currentUser, chat.name);
      }

      if (result.ok) {
        const parsedMessages = parseHistory(result.output, currentUser);
        
        if (parsedMessages.length !== lastMessageCountRef.current || 
            !isAutoRefresh || 
            JSON.stringify(parsedMessages) !== JSON.stringify(messages)) {
          
          setMessages(parsedMessages);
          lastMessageCountRef.current = parsedMessages.length;
          
          if (isAutoRefresh && parsedMessages.length > lastMessageCountRef.current) {
            console.log('🆕 Nuevos mensajes detectados!');
          }
        }
      }
    } catch (error) {
      console.error('Error cargando historial:', error);
      if (!isAutoRefresh) {
        setMessages([]);
      }
    } finally {
      if (!isAutoRefresh) {
        setMessageLoading(false);
      }
    }
  };

  useEffect(() => {
    currentChatRef.current = currentChat;
    lastMessageCountRef.current = messages.length;
  }, [currentChat, messages.length]);

  useEffect(() => {
    if (currentChat) {
      loadChatHistory(currentChat);
    } else {
      setMessages([]);
    }
  }, [currentChat]);

  useEffect(() => {
    let intervalId;
    
    if (currentChat) {
      intervalId = setInterval(() => {
        console.log('🔄 Auto-refresh del historial...');
        loadChatHistory(currentChat, true);
      }, 3000);
    }
    
    return () => {
      if (intervalId) {
        clearInterval(intervalId);
      }
    };
  }, [currentChat]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const sendMessage = async () => {
    if (!newMessage.trim() || !currentChat || messageLoading) return;

    const messageToSend = newMessage.trim();
    setNewMessage('');
    setMessageLoading(true);

    try {
      let result;
      if (currentChat.type === 'user') {
        result = await chatAPI.sendPrivateMessage(currentUser, currentChat.name, messageToSend);
      } else {
        result = await chatAPI.sendGroupMessage(currentUser, currentChat.name, messageToSend);
      }

      if (result.ok) {
        console.log('✅ Mensaje enviado');
        setTimeout(() => {
          if (currentChatRef.current) {
            loadChatHistory(currentChatRef.current);
          }
        }, 500);
      } else {
        alert('Error al enviar mensaje. El usuario puede no existir.');
        setNewMessage(messageToSend);
      }
    } catch (error) {
      alert('Error de conexión: ' + error.message);
      setNewMessage(messageToSend);
    } finally {
      setMessageLoading(false);
    }
  };

  const handleKeyPress = (e) => {
    if (e.key === 'Enter' && !messageLoading) {
      sendMessage();
    }
  };

  const createGroup = async (groupName, membersList = []) => {
    try {
      const result = await chatAPI.createGroup(currentUser, groupName, membersList);
      if (result.ok) {
        setTimeout(() => {
          loadAllGroups();
        }, 500);
        
        setCurrentChat({ type: 'group', name: groupName });
        
        if (membersList.length > 0) {
          alert(`Grupo "${groupName}" creado con ${membersList.length} miembros`);
        } else {
          alert('Grupo creado exitosamente');
        }
      } else {
        alert('Error creando grupo');
      }
    } catch (error) {
      alert('Error de conexión: ' + error.message);
    }
  };

  const handleLogout = async () => {
    if (logoutLoading) return;
    setLogoutLoading(true);
    
    try {
      await chatAPI.logout(currentUser);
    } catch (error) {
      console.error('Error en logout:', error);
    } finally {
      onLogout();
      setLogoutLoading(false);
    }
  };

  useEffect(() => {
    const interval = setInterval(() => {
      if (currentUser) {
        loadConnectedUsers();
        loadAllGroups();
      }
    }, 10000);

    return () => clearInterval(interval);
  }, [currentUser]);

  if (loading) {
    return <div className="chat-room">Cargando...</div>;
  }

  return (
    <div className="chat-room">
      <header className="chat-header">
        <h1>Chat - {currentUser}</h1>
        <div className="chat-info">
          {currentChat && (
            <span className="current-chat">
              Conversando con: {currentChat.name} ({currentChat.type})
              <span className="auto-refresh-indicator"> 🔄 Auto-actualizando</span>
            </span>
          )}
        </div>
        <button
          onClick={handleLogout}
          disabled={logoutLoading}
          className="logout-btn"
        >
          {logoutLoading ? 'Cerrando...' : '🔴 Salir'}
        </button>
      </header>

      <div className="chat-layout">
        <aside className="sidebar">
          <UserList
            users={users}
            currentUser={currentUser}
            onSelectUser={(user) => setCurrentChat({ type: 'user', name: user })}
          />
          <GroupList
            groups={groups}
            onCreateGroup={createGroup}
            onSelectGroup={(group) => setCurrentChat({ type: 'group', name: group })}
          />
        </aside>

        <main className="chat-main">
          <div className="chat-messages">
            {messageLoading && <div className="loading">Cargando mensajes...</div>}
            
            {messages.length === 0 && !messageLoading && currentChat && (
              <div className="no-messages">
                No hay mensajes aún. ¡Envía el primero!
              </div>
            )}
            
            {messages.length === 0 && !messageLoading && !currentChat && (
              <div className="no-chat-selected">
                Selecciona un chat para comenzar a conversar
              </div>
            )}
            
            {messages.map((msg, index) => (
              <ChatMessage
                key={`${msg.timestamp}-${msg.sender}-${index}`}
                message={msg}
                isOwn={msg.isOwn}
              />
            ))}
            <div ref={messagesEndRef} />
          </div>

          <div className="message-input">
            <input
              type="text"
              placeholder={
                currentChat
                  ? `Escribe un mensaje para ${currentChat.name}...`
                  : "Selecciona un chat para enviar mensajes"
              }
              value={newMessage}
              onChange={(e) => setNewMessage(e.target.value)}
              onKeyPress={handleKeyPress}
              disabled={!currentChat || messageLoading}
            />
            <button
              onClick={sendMessage}
              disabled={!currentChat || !newMessage.trim() || messageLoading}
            >
              {messageLoading ? 'Enviando...' : 'Enviar'}
            </button>
          </div>
        </main>
      </div>
    </div>
  );
};

export default ChatRoom;