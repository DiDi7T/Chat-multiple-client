package com.icesi.chatapp.Model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID; // Para generar un groupId único

public class Group implements Serializable {
    private static final long serialVersionUID = 1L;

    private String groupId;
    private String name;
    private String creatorUsername; // Almacenar solo el username del creador
    private List<String> members; // Lista de usernames de los miembros

    // Constructor vacío para Gson
    public Group() {
        this.members = new ArrayList<>();
    }

    public Group(String name, String creatorUsername) {
        this.groupId = UUID.randomUUID().toString(); // Genera un ID único para el grupo
        this.name = name;
        this.creatorUsername = creatorUsername;
        this.members = new ArrayList<>();
        this.members.add(creatorUsername); // El creador es automáticamente un miembro
    }

    // Constructor completo para cargar desde DB
    public Group(String groupId, String name, String creatorUsername, List<String> members) {
        this.groupId = groupId;
        this.name = name;
        this.creatorUsername = creatorUsername;
        this.members = (members != null) ? new ArrayList<>(members) : new ArrayList<>();
    }

    // Getters
    public String getGroupId() {
        return groupId;
    }

    public String getName() {
        return name;
    }

    public String getCreatorUsername() {
        return creatorUsername;
    }

    public List<String> getMembers() {
        return new ArrayList<>(members); // Devolver una copia para evitar modificación externa
    }

    // Setters (groupId normalmente no se cambia después de la creación)
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCreatorUsername(String creatorUsername) {
        this.creatorUsername = creatorUsername;
    }

    public void setMembers(List<String> members) {
        this.members = (members != null) ? new ArrayList<>(members) : new ArrayList<>();
    }

    // Métodos de utilidad
    public void addMember(String username) {
        if (!members.contains(username)) {
            members.add(username);
        }
    }

    public void removeMember(String username) {
        members.remove(username);
    }

    public boolean isMember(String username) {
        return members.contains(username);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Group group = (Group) o;
        return Objects.equals(groupId, group.groupId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId);
    }

    @Override
    public String toString() {
        return "Group{" +
                "groupId='" + groupId + '\'' +
                ", name='" + name + '\'' +
                ", creatorUsername='" + creatorUsername + '\'' +
                ", members=" + members +
                '}';
    }
}