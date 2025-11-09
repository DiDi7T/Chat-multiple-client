const API_BASE = 'http://localhost:5000/api';

export const chatAPI = {
  async login(username) {
    const response = await fetch(`${API_BASE}/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username })
    });
    return response.json();
  },

  async sendPrivateMessage(username, to, message) {
    const response = await fetch(`${API_BASE}/message/private`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, to, message })
    });
    return response.json();
  },

  async sendGroupMessage(username, group, message) {
    const response = await fetch(`${API_BASE}/message/group`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, group, message })
    });
    return response.json();
  },

  async createGroup(username, group, members = []) {
    const response = await fetch(`${API_BASE}/group`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, group, members })
    });
    return response.json();
  },

  async getPrivateHistory(username, withUser) {
    try {
      const response = await fetch(`${API_BASE}/history/private?username=${username}&with=${withUser}`);
      return response.json();
    } catch (error) {
      console.error('Error getting private history:', error);
      return { ok: false, output: '' };
    }
  },

  async getGroupHistory(username, group) {
    try {
      const response = await fetch(`${API_BASE}/history/group?username=${username}&group=${group}`);
      return response.json();
    } catch (error) {
      console.error('Error getting group history:', error);
      return { ok: false, output: '' };
    }
  },

  async getConnectedUsers(username) {
    try {
      const response = await fetch(`${API_BASE}/connected-users?username=${username}`);
      const data = await response.json();
      return data.ok ? { success: true, data: data.users } : { success: false, data: [] };
    } catch (error) {
      console.error('Error en getConnectedUsers:', error);
      return { success: false, data: [] };
    }
  },

  async getAllGroups() {
    try {
      const response = await fetch(`${API_BASE}/all-groups`);
      const data = await response.json();
      return data.ok ? { success: true, data: data.groups } : { success: false, data: [] };
    } catch (error) {
      console.error('Error en getAllGroups:', error);
      return { success: false, data: [] };
    }
  },

  async logout(username) {
    try {
      const response = await fetch(`${API_BASE}/logout`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username })
      });
      return response.json();
    } catch (error) {
      console.error('Error en logout:', error);
      return { ok: false, error: error.message };
    }
  }
};