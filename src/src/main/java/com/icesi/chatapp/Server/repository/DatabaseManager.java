package com.icesi.chatapp.Server.repository; // Asegúrate de que el paquete sea el correcto

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    // URL de conexión a la base de datos SQLite. "chat.db" es el nombre del archivo.
    private static final String DB_URL = "jdbc:sqlite:chat.db";
    private static DatabaseManager instance; // Instancia única del Singleton

    // Constructor privado para evitar instanciación externa
    private DatabaseManager() {
        initializeDatabase();
    }

    // Método para obtener la única instancia del DatabaseManager
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    // Método para inicializar la base de datos y crear tablas si no existen
    private void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {

            // Habilitar la clave foránea para asegurar la integridad referencial
            stmt.execute("PRAGMA foreign_keys = ON;");

            // Tabla de usuarios
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "username TEXT PRIMARY KEY," +
                    "hashed_password TEXT NOT NULL," +
                    "status TEXT NOT NULL DEFAULT 'Offline')");

            // Tabla de grupos
            stmt.execute("CREATE TABLE IF NOT EXISTS groups (" +
                    "group_id TEXT PRIMARY KEY," +
                    "name TEXT NOT NULL," +
                    "creator_username TEXT NOT NULL," +
                    "FOREIGN KEY (creator_username) REFERENCES users(username))");

            // Tabla intermedia para miembros de grupos (Relación N:M)
            stmt.execute("CREATE TABLE IF NOT EXISTS group_members (" +
                    "group_id TEXT NOT NULL," +
                    "username TEXT NOT NULL," +
                    "PRIMARY KEY (group_id, username)," +
                    "FOREIGN KEY (group_id) REFERENCES groups(group_id) ON DELETE CASCADE," +
                    "FOREIGN KEY (username) REFERENCES users(username) ON DELETE CASCADE)");

            // Tabla de mensajes
            // El campo 'content' es para TextMessage, 'audio_file_name' para VoiceMessage
            // Ambos pueden ser NULL, solo uno tendrá valor dependiendo del tipo de mensaje.
            stmt.execute("CREATE TABLE IF NOT EXISTS messages (" +
                    "message_id TEXT PRIMARY KEY," +
                    "sender_username TEXT NOT NULL," +
                    "recipient_id TEXT NOT NULL," + // Puede ser un username de usuario o un group_id
                    "message_type TEXT NOT NULL," + // Corresponde al nombre del enum MessageType
                    "timestamp INTEGER NOT NULL," +
                    "content TEXT," +
                    "audio_file_name TEXT," +
                    "FOREIGN KEY (sender_username) REFERENCES users(username) ON DELETE CASCADE)");

            System.out.println("Base de datos inicializada o tablas verificadas correctamente.");

        } catch (SQLException e) {
            System.err.println("Error al inicializar la base de datos: " + e.getMessage());
            e.printStackTrace(); // Para ver el stack trace completo del error
        }
    }

    // Método para obtener una conexión a la base de datos
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

}