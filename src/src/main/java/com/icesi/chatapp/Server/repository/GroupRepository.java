package com.icesi.chatapp.Server.repository;

import com.icesi.chatapp.Model.Group;
import java.util.List;
import java.util.Optional;


public interface GroupRepository {
    Optional<Group> findById(String groupId);
    Optional<Group> findByName(String groupName); // Útil para crear y unirse por nombre
    void save(Group group);
    void addMemberToGroup(String groupId, String username);
    void removeMemberFromGroup(String groupId, String username);
    List<Group> findGroupsByMember(String username); // Para que un usuario vea sus grupos
    void deleteGroup(String groupId);

}
