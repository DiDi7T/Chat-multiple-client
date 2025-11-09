import React, { useState } from 'react';
import { chatAPI } from '../services/api';

const Login = ({ onLogin }) => {
  const [username, setUsername] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!username.trim()) return;

    setLoading(true);
    try {
      const result = await chatAPI.login(username.trim());
      console.log('🔍 Resultado del login:', result);
      
      // El proxy usa "ok" en lugar de "success"
      if (result.ok) {
        onLogin(username.trim());
      } else {
        alert('Error: ' + (result.error || 'No se pudo conectar'));
      }
    } catch (error) {
      console.error('💥 Error completo:', error);
      alert('Error de conexión: ' + error.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-page">
      <div className="login-container">
        <h1>💬 Chat Application</h1>
        <form onSubmit={handleSubmit}>
          <input
            type="text"
            placeholder="Ingresa tu nombre de usuario"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            required
            disabled={loading}
          />
          <button type="submit" disabled={loading}>
            {loading ? 'Conectando...' : 'Conectar'}
          </button>
        </form>
      </div>
    </div>
  );
};

export default Login;