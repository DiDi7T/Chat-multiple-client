import React, { useState } from 'react';

const GroupList = ({ groups, onCreateGroup, onSelectGroup }) => {
  const [newGroupName, setNewGroupName] = useState('');

  const handleCreateGroup = () => {
    if (newGroupName.trim()) {
      onCreateGroup(newGroupName);
      setNewGroupName('');
    }
  };

  return (
    <div className="group-list">
      <h3>Grupos</h3>
      {groups.map(group => (
        <div 
          key={group} 
          className="group-item"
          onClick={() => onSelectGroup(group)}
        >
          👥 {group}
        </div>
      ))}
      <div className="create-group">
        <input
          type="text"
          placeholder="Nuevo grupo"
          value={newGroupName}
          onChange={(e) => setNewGroupName(e.target.value)}
          onKeyPress={(e) => e.key === 'Enter' && handleCreateGroup()}
        />
        <button onClick={handleCreateGroup}>Crear</button>
      </div>
    </div>
  );
};

export default GroupList;