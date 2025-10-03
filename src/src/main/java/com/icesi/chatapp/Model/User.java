package com.icesi.chatapp.Model;

import java.io.Serializable; // Útil si consideras serializar objetos directamente (aunque JSON es preferible)
import java.util.Objects;

public class User implements Serializable {
    private static final long serialVersionUID = 1L; // Para serialización

    private String username;
    private String hashedPassword; // Solo en el servidor para seguridad
    private String status; // "Online", "Offline", "Away"

    // Constructor vacío para Gson
    public User() {}

    public User(String username, String hashedPassword, String status) {
        this.username = username;
        this.hashedPassword = hashedPassword;
        this.status = status;
    }

    // Constructor para cuando se envía al cliente (sin password)
    public User(String username, String status) {
        this.username = username;
        this.status = status;
        this.hashedPassword = null; // Asegurarse de que no se envíe la contraseña
    }

    // Getters
    public String getUsername() {
        return username;
    }

    public String getHashedPassword() {
        return hashedPassword;
    }

    public String getStatus() {
        return status;
    }

    // Setters
    public void setUsername(String username) {
        this.username = username;
    }

    public void setHashedPassword(String hashedPassword) {
        this.hashedPassword = hashedPassword;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(username, user.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }

    @Override
    public String toString() {
        return "User{" +
                "username='" + username + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}