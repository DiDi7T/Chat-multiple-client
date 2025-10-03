package com.icesi.chatapp.Server.repository;

import com.icesi.chatapp.Model.User;
import java.util.Optional;
import java.util.List; // Opcional, si necesitas listar todos los usuarios

public interface UserRepository {
    Optional<User> findByUsername(String username); // Usa Optional para manejar el caso de no encontrar el usuario
    void save(User user);
    void updateStatus(String username, String status);
    List<User> findAllOnlineUsers(); // Para saber quién está online
    // Puedes añadir más métodos según sea necesario: delete, updatePassword, etc.
}