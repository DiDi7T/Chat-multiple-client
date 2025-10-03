package com.icesi.chatapp.Server.repository;

import com.icesi.chatapp.Model.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SQLiteUserRepository implements UserRepository {

    @Override
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT username, hashed_password, status FROM users WHERE username = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return Optional.of(new User(
                        rs.getString("username"),
                        rs.getString("hashed_password"),
                        rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error finding user by username: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public void save(User user) {
        // Usamos INSERT OR REPLACE para actualizar si ya existe o insertar si no
        // O podrías tener un 'update' y un 'insert' separados
        String sql = "INSERT OR REPLACE INTO users (username, hashed_password, status) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getHashedPassword());
            pstmt.setString(3, user.getStatus());
            pstmt.executeUpdate();
            System.out.println("User saved/updated: " + user.getUsername());
        } catch (SQLException e) {
            System.err.println("Error saving user: " + e.getMessage());
        }
    }

    @Override
    public void updateStatus(String username, String status) {
        String sql = "UPDATE users SET status = ? WHERE username = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, status);
            pstmt.setString(2, username);
            pstmt.executeUpdate();
            System.out.println("User " + username + " status updated to " + status);
        } catch (SQLException e) {
            System.err.println("Error updating user status: " + e.getMessage());
        }
    }

    @Override
    public List<User> findAllOnlineUsers() {
        List<User> onlineUsers = new ArrayList<>();
        String sql = "SELECT username, status FROM users WHERE status = 'Online'";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                // Solo enviamos el username y el status al cliente, no la contraseña
                onlineUsers.add(new User(rs.getString("username"), rs.getString("status")));
            }
        } catch (SQLException e) {
            System.err.println("Error finding online users: " + e.getMessage());
        }
        return onlineUsers;
    }
}