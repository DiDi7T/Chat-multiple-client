import React from 'react';

const UserList = ({ users, onSelectUser }) => {
  return (
    <div className="user-list">
      <h3>Usuarios Conectados</h3>
      {users.map(user => (
        <div 
          key={user} 
          className="user-item"
          onClick={() => onSelectUser(user)}
        >
          👤 {user}
        </div>
      ))}
    </div>
  );
};

export default UserList;