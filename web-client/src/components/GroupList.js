import React, { useState } from 'react';

const GroupList = ({ groups, onCreateGroup, onSelectGroup }) => {
  const [newGroupName, setNewGroupName] = useState('');
  const [showMembersInput, setShowMembersInput] = useState(false);
  const [members, setMembers] = useState('');

  const handleCreateGroup = () => {
    if (newGroupName.trim()) {
      // Separar miembros por comas
      const membersList = members
        .split(',')
        .map(m => m.trim())
        .filter(m => m.length > 0);
      
      onCreateGroup(newGroupName.trim(), membersList);
      
      // Limpiar campos
      setNewGroupName('');
      setMembers('');
      setShowMembersInput(false);
    }
  };

  const handleNextStep = () => {
    if (newGroupName.trim()) {
      setShowMembersInput(true);
    } else {
      alert('Ingresa un nombre para el grupo');
    }
  };

  const handleCancel = () => {
    setShowMembersInput(false);
    setMembers('');
    setNewGroupName('');
  };

  return (
    <div className="group-list">
      <h3>Grupos</h3>
      
      {groups.length === 0 && (
        <div style={{ 
          padding: '10px', 
          color: '#666', 
          fontSize: '14px',
          fontStyle: 'italic' 
        }}>
          No tienes grupos aún
        </div>
      )}
      
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
        {!showMembersInput ? (
          <>
            <input
              type="text"
              placeholder="Nombre del grupo"
              value={newGroupName}
              onChange={(e) => setNewGroupName(e.target.value)}
              onKeyPress={(e) => e.key === 'Enter' && handleNextStep()}
            />
            <button onClick={handleNextStep}>
              Siguiente →
            </button>
          </>
        ) : (
          <>
            <div style={{ fontSize: '12px', color: '#666', marginBottom: '5px' }}>
              Grupo: <strong>{newGroupName}</strong>
            </div>
            <input
              type="text"
              placeholder="Miembros (separados por coma)"
              value={members}
              onChange={(e) => setMembers(e.target.value)}
              onKeyPress={(e) => e.key === 'Enter' && handleCreateGroup()}
            />
            <button onClick={handleCreateGroup}>
              ✅ Crear Grupo
            </button>
            <button 
              onClick={handleCancel}
              style={{ 
                marginTop: '5px', 
                backgroundColor: '#e74c3c',
                color: 'white'
              }}
            >
              ❌ Cancelar
            </button>
          </>
        )}
      </div>
    </div>
  );
};

export default GroupList;
