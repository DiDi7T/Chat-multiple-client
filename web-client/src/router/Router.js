import React from 'react';
import Login from '../pages/Login';
import ChatRoom from '../pages/ChatRoom';

const Router = () => {
  const [currentUser, setCurrentUser] = React.useState(null);

  if (!currentUser) {
    return <Login onLogin={setCurrentUser} />;
  }

  return <ChatRoom currentUser={currentUser} onLogout={() => setCurrentUser(null)} />;
};

export default Router;