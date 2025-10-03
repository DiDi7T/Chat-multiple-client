package com.icesi.chatapp.Server.repository;

import com.icesi.chatapp.Model.Group;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SQLiteGroupRepository implements GroupRepository {

    @Override
    public Optional<Group> findById(String groupId) {
        String sqlGroup = "SELECT group_id, name, creator_username FROM groups WHERE group_id = ?";
        String sqlMembers = "SELECT username FROM group_members WHERE group_id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmtGroup = conn.prepareStatement(sqlGroup);
             PreparedStatement pstmtMembers = conn.prepareStatement(sqlMembers)) {

            pstmtGroup.setString(1, groupId);
            ResultSet rsGroup = pstmtGroup.executeQuery();

            if (rsGroup.next()) {
                List<String> members = new ArrayList<>();
                pstmtMembers.setString(1, groupId);
                ResultSet rsMembers = pstmtMembers.executeQuery();
                while (rsMembers.next()) {
                    members.add(rsMembers.getString("username"));
                }
                return Optional.of(new Group(
                        rsGroup.getString("group_id"),
                        rsGroup.getString("name"),
                        rsGroup.getString("creator_username"),
                        members
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error finding group by ID: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Optional<Group> findByName(String groupName) {
        String sqlGroup = "SELECT group_id, name, creator_username FROM groups WHERE name = ?";
        String sqlMembers = "SELECT username FROM group_members WHERE group_id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmtGroup = conn.prepareStatement(sqlGroup);
             PreparedStatement pstmtMembers = conn.prepareStatement(sqlMembers)) {

            pstmtGroup.setString(1, groupName);
            ResultSet rsGroup = pstmtGroup.executeQuery();

            if (rsGroup.next()) {
                String groupId = rsGroup.getString("group_id");
                List<String> members = new ArrayList<>();
                pstmtMembers.setString(1, groupId);
                ResultSet rsMembers = pstmtMembers.executeQuery();
                while (rsMembers.next()) {
                    members.add(rsMembers.getString("username"));
                }
                return Optional.of(new Group(
                        groupId,
                        rsGroup.getString("name"),
                        rsGroup.getString("creator_username"),
                        members
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error finding group by name: " + e.getMessage());
        }
        return Optional.empty();
    }


    @Override
    public void save(Group group) {
        String sqlGroup = "INSERT OR REPLACE INTO groups (group_id, name, creator_username) VALUES (?, ?, ?)";
        String sqlMember = "INSERT OR IGNORE INTO group_members (group_id, username) VALUES (?, ?)"; // IGNORE para evitar duplicados
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmtGroup = conn.prepareStatement(sqlGroup);
             PreparedStatement pstmtMember = conn.prepareStatement(sqlMember)) {

            conn.setAutoCommit(false); // Iniciar transacción

            pstmtGroup.setString(1, group.getGroupId());
            pstmtGroup.setString(2, group.getName());
            pstmtGroup.setString(3, group.getCreatorUsername());
            pstmtGroup.executeUpdate();

            // Guardar miembros
            for (String username : group.getMembers()) {
                pstmtMember.setString(1, group.getGroupId());
                pstmtMember.setString(2, username);
                pstmtMember.executeUpdate();
            }

            conn.commit(); // Confirmar transacción
            System.out.println("Group saved/updated: " + group.getName());
        } catch (SQLException e) {
            try {
                // Rollback en caso de error
                DatabaseManager.getInstance().getConnection().rollback();
            } catch (SQLException rollbackEx) {
                System.err.println("Error during rollback: " + rollbackEx.getMessage());
            }
            System.err.println("Error saving group: " + e.getMessage());
        } finally {
            try {
                DatabaseManager.getInstance().getConnection().setAutoCommit(true); // Restaurar auto-commit
            } catch (SQLException e) {
                System.err.println("Error restoring auto-commit: " + e.getMessage());
            }
        }
    }

    @Override
    public void addMemberToGroup(String groupId, String username) {
        String sql = "INSERT OR IGNORE INTO group_members (group_id, username) VALUES (?, ?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, groupId);
            pstmt.setString(2, username);
            pstmt.executeUpdate();
            System.out.println("User " + username + " added to group " + groupId);
        } catch (SQLException e) {
            System.err.println("Error adding member to group: " + e.getMessage());
        }
    }

    @Override
    public void removeMemberFromGroup(String groupId, String username) {
        String sql = "DELETE FROM group_members WHERE group_id = ? AND username = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, groupId);
            pstmt.setString(2, username);
            pstmt.executeUpdate();
            System.out.println("User " + username + " removed from group " + groupId);
        } catch (SQLException e) {
            System.err.println("Error removing member from group: " + e.getMessage());
        }
    }

    @Override
    public List<Group> findGroupsByMember(String username) {
        List<Group> groups = new ArrayList<>();
        String sql = "SELECT g.group_id, g.name, g.creator_username " +
                "FROM groups g JOIN group_members gm ON g.group_id = gm.group_id " +
                "WHERE gm.username = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                // Para cada grupo encontrado, necesitaríamos cargar sus miembros.
                // Esto podría ser ineficiente si hay muchos grupos/miembros.
                // Para simplificar, podrías cargar solo los IDs de los grupos aquí y luego cargar los miembros
                // individualmente si se accede a un grupo específico, o hacer un JOIN más complejo.
                // Por ahora, solo cargamos los datos básicos del grupo.
                String groupId = rs.getString("group_id");
                // OPTIMIZACION: Aquí llamaríamos a findById(groupId) para obtener la lista completa de miembros.
                // Para evitar recursión infinita o consultas excesivas, puedes simplificar la clase Group
                // para que no cargue la lista completa de miembros automáticamente al inicio,
                // sino solo cuando sea necesario.
                // O, pasar una lista vacía y actualizarla después.
                groups.add(new Group(groupId, rs.getString("name"), rs.getString("creator_username"), new ArrayList<>()));
            }
        } catch (SQLException e) {
            System.err.println("Error finding groups by member: " + e.getMessage());
        }
        return groups;
    }

    @Override
    public void deleteGroup(String groupId) {
        String sqlGroup = "DELETE FROM groups WHERE group_id = ?";
        String sqlMembers = "DELETE FROM group_members WHERE group_id = ?"; // ON DELETE CASCADE también debería manejar esto

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmtGroup = conn.prepareStatement(sqlGroup);
             PreparedStatement pstmtMembers = conn.prepareStatement(sqlMembers)) { // Este es opcional si el CASCADE funciona

            conn.setAutoCommit(false); // Iniciar transacción

            pstmtMembers.setString(1, groupId);
            pstmtMembers.executeUpdate(); // Borrar miembros primero (o confiar en CASCADE)

            pstmtGroup.setString(1, groupId);
            pstmtGroup.executeUpdate(); // Borrar el grupo

            conn.commit(); // Confirmar transacción
            System.out.println("Group " + groupId + " deleted.");
        } catch (SQLException e) {
            try {
                DatabaseManager.getInstance().getConnection().rollback();
            } catch (SQLException rollbackEx) {
                System.err.println("Error during rollback: " + rollbackEx.getMessage());
            }
            System.err.println("Error deleting group: " + e.getMessage());
        } finally {
            try {
                DatabaseManager.getInstance().getConnection().setAutoCommit(true);
            } catch (SQLException e) {
                System.err.println("Error restoring auto-commit: " + e.getMessage());
            }
        }
    }
}