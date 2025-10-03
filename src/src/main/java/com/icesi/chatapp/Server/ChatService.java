package com.icesi.chatapp.Server;

import com.icesi.chatapp.Model.Group;
import com.icesi.chatapp.Model.Message;
import com.icesi.chatapp.Model.MessageType;
import com.icesi.chatapp.Model.TextMessage;
import com.icesi.chatapp.Model.User;
import com.icesi.chatapp.Model.VoiceMessage;
import com.icesi.chatapp.Server.repository.GroupRepository;
import com.icesi.chatapp.Server.repository.MessageRepository;
import com.icesi.chatapp.Server.repository.SQLiteGroupRepository;
import com.icesi.chatapp.Server.repository.SQLiteMessageRepository;
import com.icesi.chatapp.Server.repository.SQLiteUserRepository;
import com.icesi.chatapp.Server.repository.UserRepository;
import com.icesi.chatapp.Server.ChatServer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ChatService {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final MessageRepository messageRepository;
    private final ChatServer chatServer; // Para enviar mensajes a clientes conectados

    private final Gson gson;

    // Mapa para gestionar los usuarios conectados y sus ClientHandlers
    // Key: username, Value: ClientHandler
    private final ConcurrentHashMap<String, ClientHandler> connectedClients;

    public ChatService(ChatServer chatServer) {
        this.chatServer = chatServer;
        this.userRepository = new SQLiteUserRepository();
        this.groupRepository = new SQLiteGroupRepository();
        this.messageRepository = new SQLiteMessageRepository();
        this.gson = new GsonBuilder().setPrettyPrinting().create(); // Pretty printing para depuración
        this.connectedClients = new ConcurrentHashMap<>();
    }

    /**
     * Registra un nuevo usuario en el sistema.
     * @param username El nombre de usuario.
     * @param password La contraseña en texto plano (se hashea internamente).
     * @return true si el registro fue exitoso, false si el usuario ya existe.
     */
    public boolean registerUser(String username, String password) {
        if (userRepository.findByUsername(username).isPresent()) {
            System.out.println("Intento de registro fallido: el usuario " + username + " ya existe.");
            return false;
        }
        String hashedPassword = hashPassword(password);
        User newUser = new User(username, hashedPassword, "Offline");
        userRepository.save(newUser);
        System.out.println("Usuario registrado exitosamente: " + username);
        return true;
    }

    /**
     * Autentica un usuario y lo marca como conectado.
     * @param username El nombre de usuario.
     * @param password La contraseña en texto plano.
     * @param handler El ClientHandler asociado a esta conexión.
     * @return true si el login fue exitoso, false en caso contrario.
     */
    public boolean loginUser(String username, String password, ClientHandler handler) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getHashedPassword().equals(hashPassword(password))) {
                if (connectedClients.containsKey(username)) {
                    // El usuario ya está conectado en otra sesión. Puedes decidir si permitir o no.
                    // Por ahora, lo marcamos como fallido para una única sesión.
                    System.out.println("Login fallido: El usuario " + username + " ya está conectado.");
                    return false;
                }
                user.setStatus("Online");
                userRepository.updateStatus(username, "Online");
                connectedClients.put(username, handler);
                handler.setUsername(username); // Asigna el username al handler
                System.out.println("Usuario " + username + " ha iniciado sesión.");

                // Notificar a otros clientes sobre el cambio de estado (si ya estaban online)
                notifyUserStatusUpdate(username, "Online");
                return true;
            }
        }
        System.out.println("Login fallido: Credenciales inválidas para " + username);
        return false;
    }

    /**
     * Desconecta un usuario y lo marca como offline.
     * @param username El nombre de usuario que se desconecta.
     */
    public void logoutUser(String username) {
        if (username != null && connectedClients.containsKey(username)) {
            connectedClients.remove(username);
            userRepository.updateStatus(username, "Offline");
            System.out.println("Usuario " + username + " ha cerrado sesión.");

            // Notificar a otros clientes sobre el cambio de estado
            notifyUserStatusUpdate(username, "Offline");
        }
    }

    /**
     * Notifica a todos los clientes conectados sobre un cambio de estado de un usuario.
     * @param username El usuario cuyo estado cambió.
     * @param status El nuevo estado ("Online" o "Offline").
     */
    private void notifyUserStatusUpdate(String username, String status) {
        String messageJson = gson.toJson(createStatusUpdateMessage(username, status));
        // Enviar la notificación a todos los clientes excepto al que acaba de cambiar de estado
        connectedClients.values().forEach(h -> {
            if (!h.getUsername().equals(username)) {
                h.sendMessage(messageJson);
            }
        });
    }

    /**
     * Crea un mensaje de tipo SERVER_RESPONSE con información de estado de usuario.
     */
    private ServerResponse createStatusUpdateMessage(String username, String status) {
        // Podrías crear una clase específica para StatusUpdate si quieres,
        // o usar un mapa simple para el payload.
        ConcurrentHashMap<String, String> payload = new ConcurrentHashMap<>();
        payload.put("type", "user_status");
        payload.put("username", username);
        payload.put("status", status);
        return new ServerResponse(MessageType.USER_STATUS_UPDATE, true, "User status updated", payload);
    }


    /**
     * Crea un nuevo grupo de chat.
     * @param groupName El nombre del grupo.
     * @param creatorUsername El nombre de usuario del creador.
     * @return El objeto Group creado, o null si ya existe un grupo con ese nombre.
     */
    public Group createGroup(String groupName, String creatorUsername) {
        if (groupRepository.findByName(groupName).isPresent()) {
            System.out.println("Intento de creación de grupo fallido: el grupo " + groupName + " ya existe.");
            return null;
        }
        Group newGroup = new Group(groupName, creatorUsername);
        groupRepository.save(newGroup);
        System.out.println("Grupo creado exitosamente: " + groupName + " por " + creatorUsername);

        // Notificar a todos los miembros sobre el nuevo grupo (en este caso, solo el creador)
        notifyGroupUpdate(newGroup, "created");
        return newGroup;
    }

    /**
     * Añade un miembro a un grupo existente.
     * @param groupId ID del grupo.
     * @param usernameToAdd Nombre de usuario a añadir.
     * @return true si se añadió, false si el grupo no existe o el usuario ya es miembro.
     */
    public boolean addMemberToGroup(String groupId, String usernameToAdd) {
        Optional<Group> groupOpt = groupRepository.findById(groupId);
        if (groupOpt.isEmpty()) {
            System.out.println("Error al añadir miembro: Grupo " + groupId + " no encontrado.");
            return false;
        }
        Group group = groupOpt.get();
        if (group.isMember(usernameToAdd)) {
            System.out.println("Error al añadir miembro: " + usernameToAdd + " ya es miembro del grupo " + group.getName());
            return false;
        }
        if (userRepository.findByUsername(usernameToAdd).isEmpty()) {
            System.out.println("Error al añadir miembro: Usuario " + usernameToAdd + " no encontrado.");
            return false;
        }

        groupRepository.addMemberToGroup(groupId, usernameToAdd);
        group.addMember(usernameToAdd); // Actualiza el objeto en memoria
        System.out.println("Usuario " + usernameToAdd + " añadido al grupo " + group.getName());

        // Notificar a todos los miembros del grupo (incluido el nuevo) sobre el cambio
        notifyGroupUpdate(group, "member_added", usernameToAdd);
        return true;
    }

    /**
     * Envía un mensaje de texto a un usuario o grupo.
     * @param message El objeto TextMessage.
     */
    public void processTextMessage(TextMessage message) {
        messageRepository.save(message);
        sendMessageToRecipients(message);
        System.out.println("Mensaje de texto guardado y procesado: " + message.getMessageId());
    }

    /**
     * Procesa una nota de voz.
     * @param message El objeto VoiceMessage.
     */
    public void processVoiceMessage(VoiceMessage message) {
        messageRepository.save(message);
        sendMessageToRecipients(message);
        System.out.println("Mensaje de voz guardado y procesado: " + message.getMessageId());
    }

    /**
     * Enruta un mensaje (texto o voz) a sus destinatarios.
     * @param message El mensaje a enrutar.
     */
    private void sendMessageToRecipients(Message message) {
        String recipientId = message.getRecipientId();
        String messageJson = gson.toJson(new ServerResponse(MessageType.NEW_MESSAGE_NOTIFICATION, true, "New message", message));

        // Determinar si es un chat individual o de grupo
        // Si el recipientId es el username de un usuario existente, es chat individual
        // Si el recipientId es un groupId, es chat de grupo
        Optional<User> recipientUserOpt = userRepository.findByUsername(recipientId);
        if (recipientUserOpt.isPresent()) { // Mensaje privado
            ClientHandler recipientHandler = connectedClients.get(recipientId);
            if (recipientHandler != null) {
                recipientHandler.sendMessage(messageJson);
                System.out.println("Mensaje enviado a " + recipientId + " (online).");
            } else {
                System.out.println("Mensaje guardado para " + recipientId + " (offline).");
            }
        } else { // Mensaje de grupo
            Optional<Group> groupOpt = groupRepository.findById(recipientId);
            if (groupOpt.isPresent()) {
                Group group = groupOpt.get();
                for (String memberUsername : group.getMembers()) {
                    ClientHandler memberHandler = connectedClients.get(memberUsername);
                    if (memberHandler != null) {
                        memberHandler.sendMessage(messageJson);
                        System.out.println("Mensaje de grupo enviado a " + memberUsername + " del grupo " + group.getName() + " (online).");
                    } else {
                        System.out.println("Mensaje de grupo guardado para " + memberUsername + " del grupo " + group.getName() + " (offline).");
                    }
                }
            } else {
                System.err.println("Error: Destinatario (grupo o usuario) no encontrado para el ID: " + recipientId);
            }
        }
    }

    /**
     * Recupera el historial de chat para un destinatario (usuario o grupo).
     * @param username El usuario que solicita el historial.
     * @param recipientId El ID del chat (username del otro usuario o groupId).
     * @param limit El número máximo de mensajes a recuperar.
     * @return Una lista de mensajes.
     */
    public List<Message> getChatHistory(String username, String recipientId, int limit) {
        // En un chat 1-1, el historial incluye mensajes donde `username` es sender o `recipientId` es sender,
        // y `recipientId` es receiver o `username` es receiver.
        // En un chat de grupo, `recipientId` es el groupId y los mensajes son enviados a ese group_id.
        // La implementación actual de findByRecipientId ya busca por recipient_id o sender_username,
        // lo cual es una simplificación. Para un chat 1-1 estricto, necesitarías una consulta más compleja
        // que busque mensajes entre los dos usuarios específicos.
        // Por ahora, findByRecipientId(recipientId) funcionará para grupos y como base para 1-1.

        List<Message> history = messageRepository.findByRecipientId(recipientId, limit);
        // Si es un chat 1-1, también querrás los mensajes donde el sender es recipientId y el receiver es username
        // Y donde el sender es username y el receiver es recipientId
        // La consulta findByRecipientId necesita ser más inteligente para esto.
        // Una versión simple podría ser: SELECT ... WHERE (sender_username = ? AND recipient_id = ?) OR (sender_username = ? AND recipient_id = ?)
        // Pero para la simplicidad inicial, la consulta actual se enfoca en el `recipientId` directo.

        // Ordenar los mensajes cronológicamente (findByRecipientId los devuelve DESC, así que invertimos)
        history.sort((m1, m2) -> Long.compare(m1.getTimestamp(), m2.getTimestamp()));
        return history;
    }

    /**
     * Genera un hash SHA-256 de una contraseña.
     * @param password La contraseña en texto plano.
     * @return El hash de la contraseña en formato Base64.
     */
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    /**
     * Notifica a los miembros de un grupo sobre una actualización (creación, miembro añadido/removido).
     * @param group El grupo actualizado.
     * @param updateType El tipo de actualización ("created", "member_added", "member_removed", etc.).
     * @param additionalInfo Información extra (ej. el miembro añadido/removido).
     */
    private void notifyGroupUpdate(Group group, String updateType, String... additionalInfo) {
        // Podrías crear una clase específica para GroupUpdateMessage
        ConcurrentHashMap<String, Object> payload = new ConcurrentHashMap<>();
        payload.put("type", "group_update");
        payload.put("groupId", group.getGroupId());
        payload.put("groupName", group.getName());
        payload.put("members", group.getMembers()); // Enviar la lista actualizada de miembros
        payload.put("updateType", updateType);
        if (additionalInfo.length > 0) {
            payload.put("info", additionalInfo[0]);
        }

        ServerResponse response = new ServerResponse(MessageType.GROUP_UPDATE_NOTIFICATION, true, "Group updated", payload);
        String messageJson = gson.toJson(response);

        for (String memberUsername : group.getMembers()) {
            ClientHandler memberHandler = connectedClients.get(memberUsername);
            if (memberHandler != null) {
                memberHandler.sendMessage(messageJson);
                System.out.println("Notificación de grupo para " + memberUsername + ": " + updateType);
            }
        }
    }

    /**
     * Clase interna para las respuestas genéricas del servidor al cliente.
     */
    public static class ServerResponse implements Serializable {
        private static final long serialVersionUID = 1L;
        public MessageType type;
        public boolean success;
        public String message;
        public Object payload; // Puede ser un Message, Group, User, o un mapa de datos

        public ServerResponse(MessageType type, boolean success, String message, Object payload) {
            this.type = type;
            this.success = success;
            this.message = message;
            this.payload = payload;
        }

        @Override
        public String toString() {
            return "ServerResponse{" +
                    "type=" + type +
                    ", success=" + success +
                    ", message='" + message + '\'' +
                    ", payload=" + payload +
                    '}';
        }
    }
}