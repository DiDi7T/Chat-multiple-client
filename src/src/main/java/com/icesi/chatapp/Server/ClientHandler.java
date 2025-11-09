package com.icesi.chatapp.Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ClientHandler implements Runnable {

    private Socket clientSocket;
    private String clientName;
    private BufferedReader in;
    private PrintWriter out;

    private static final Map<String, ClientHandler> users = Collections.synchronizedMap(new HashMap<>());
    private static final Map<String, Set<ClientHandler>> groups = Collections.synchronizedMap(new HashMap<>());

    public ClientHandler(Socket socket) throws IOException {
        this.clientSocket = socket;
        this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        this.out = new PrintWriter(clientSocket.getOutputStream(), true);
    }

    @Override
    public void run() {
        try {
            out.println("Ingresa tu nombre:");
            clientName = in.readLine();

            synchronized (users) {
                if (users.containsKey(clientName)) {
                    out.println("Nombre ya en uso. Conexión terminada.");
                    clientSocket.close();
                    return;
                }
                users.put(clientName, this);
            }

            // 💡 Enviar bienvenida + menú (lo que el server.js espera detectar)
            enviarMenuInicial();

            // Esperar comandos del cliente o del proxy
            String input;
            while ((input = in.readLine()) != null) {
                if (input.trim().equalsIgnoreCase("EXIT")) {
                    out.println("Desconectando...");
                    break;
                }

                switch (input.trim().toUpperCase()) {
                    case "1":
                    case "PRIVATE":
                        enviarPrivado();
                        break;
                    case "2":
                    case "GROUP":
                        crearGrupo();
                        break;
                    case "3":
                    case "GROUPMSG":
                        enviarAGrupo();
                        break;
                    case "7":
                    case "HISTORY_PRIVATE":
                        verHistorialPrivado();
                        break;
                    case "8":
                    case "HISTORY_GROUP":
                        verHistorialGrupo();
                        break;

                    case "9":
                    case "LIST_GROUPS":
                        listarMisGrupos();
                        break;
                    default:
                        out.println("Comando o número no válido.");
                        break;
                }

                enviarMenu();
            }

        } catch (IOException e) {
            System.err.println("Error con el cliente " + clientName + ": " + e.getMessage());
        } finally {
            desconectarUsuario();
        }
    }

    // -------------------- MENSAJES PRIVADOS --------------------
    private void enviarPrivado() throws IOException {
        out.println("Usuarios disponibles:");
        synchronized (users) {
            for (String u : users.keySet()) {
                if (!u.equals(clientName))
                    out.println(" - " + u);
            }
        }

        out.println("¿A qué usuario deseas enviar el mensaje?");
        String destino = in.readLine();

        out.println("Escribe tu mensaje:");
        String mensaje = in.readLine();

        ClientHandler receptor;
        synchronized (users) {
            receptor = users.get(destino);
        }

        if (receptor != null && !destino.equals(clientName)) {
            receptor.out.println("Mensaje privado de " + clientName + ": " + mensaje);
            MessageHistory.savePrivateMessage(clientName, destino, mensaje);
            out.println("Mensaje enviado correctamente.");
        } else {
            out.println("Usuario no encontrado o inválido.");
        }
    }

    // -------------------- GRUPOS --------------------
    private void crearGrupo() throws IOException {
        out.println("Nombre del grupo:");
        String nombreGrupo = in.readLine();

        synchronized (groups) {
            groups.putIfAbsent(nombreGrupo, Collections.synchronizedSet(new HashSet<>()));
            groups.get(nombreGrupo).add(this);
        }

        out.println("Grupo '" + nombreGrupo + "' creado.");
        out.println("Escribe los nombres de los usuarios a agregar (separados por coma):");
        String linea = in.readLine();
        if (linea == null || linea.trim().isEmpty())
            return;

        String[] nombres = linea.split(",");
        synchronized (groups) {
            for (String nombre : nombres) {
                String limpio = nombre.trim();
                if (!limpio.equals(clientName)) {
                    ClientHandler ch;
                    synchronized (users) {
                        ch = users.get(limpio);
                    }
                    if (ch != null) {
                        groups.get(nombreGrupo).add(ch);
                        ch.out.println("Has sido agregado al grupo '" + nombreGrupo + "' por " + clientName + ".");
                    } else {
                        out.println("No se pudo agregar a '" + limpio + "' (no existe).");
                    }
                }
            }
        }

        out.println("Miembros actuales del grupo '" + nombreGrupo + "':");
        synchronized (groups) {
            for (ClientHandler miembro : groups.get(nombreGrupo)) {
                out.println(" - " + miembro.clientName);
            }
        }
    }

    private void enviarAGrupo() throws IOException {
        out.println("Nombre del grupo al que deseas enviar mensaje:");
        String grupo = in.readLine();

        if (!groups.containsKey(grupo)) {
            out.println("Grupo no encontrado.");
            return;
        }

        out.println("Escribe tu mensaje:");
        String mensaje = in.readLine();

        synchronized (groups) {
            for (ClientHandler miembro : groups.get(grupo)) {
                if (!miembro.clientName.equals(this.clientName)) {
                    miembro.out.println("[" + grupo + "] " + clientName + ": " + mensaje);
                }
            }
        }

        MessageHistory.saveGroupMessage(clientName, grupo, mensaje);
        out.println("Mensaje enviado al grupo correctamente.");
    }

    // -------------------- HISTORIAL --------------------
    private void verHistorialPrivado() throws IOException {
        out.println("¿De qué usuario quieres ver el historial?");
        String usuario = in.readLine();

        List<String> historial = MessageHistory.getPrivateHistory(clientName, usuario);
        if (historial.isEmpty()) {
            out.println("No hay historial con " + usuario);
        } else {
            out.println("=== HISTORIAL CON " + usuario.toUpperCase() + " ===");
            for (String linea : historial)
                out.println(linea);
            out.println("=== FIN DEL HISTORIAL ===");
        }
    }

    private void verHistorialGrupo() throws IOException {
        out.println("¿De qué grupo quieres ver el historial?");
        String grupo = in.readLine();

        List<String> historial = MessageHistory.getGroupHistory(grupo);
        if (historial.isEmpty()) {
            out.println("No hay historial para el grupo " + grupo);
        } else {
            out.println("=== HISTORIAL DEL GRUPO " + grupo.toUpperCase() + " ===");
            for (String linea : historial)
                out.println(linea);
            out.println("=== FIN DEL HISTORIAL ===");
        }
    }

    // -------------------- LISTAR GRUPOS --------------------
    private void listarMisGrupos() {
        List<String> misGrupos = new ArrayList<>();

        synchronized (groups) {
            for (Map.Entry<String, Set<ClientHandler>> entry : groups.entrySet()) {
                String nombreGrupo = entry.getKey();
                Set<ClientHandler> miembros = entry.getValue();

                // Verificar si este usuario está en el grupo
                for (ClientHandler miembro : miembros) {
                    if (miembro.clientName != null && miembro.clientName.equals(this.clientName)) {
                        misGrupos.add(nombreGrupo);
                        break;
                    }
                }
            }
        }

        if (misGrupos.isEmpty()) {
            out.println("No perteneces a ningún grupo.");
        } else {
            out.println("Tus grupos:");
            for (String grupo : misGrupos) {
                out.println("- " + grupo);
            }
        }
        out.println("=== FIN LISTA GRUPOS ===");
    }

    // -------------------- MENÚ --------------------
    private void enviarMenuInicial() {
        out.println("¡Hola " + clientName + "! Bienvenido al chat.");
        enviarMenu();
    }

    private void enviarMenu() {
    out.println("\nMENU:");
    out.println("1. Enviar mensaje a usuario");
    out.println("2. Crear grupo");
    out.println("3. Enviar mensaje a grupo");
    out.println("4. Salir");
    out.println("7. Ver historial privado");
    out.println("8. Ver historial de grupo");
    out.println("9. Listar mis grupos");
    out.println("Elige opción:");
    out.flush();
}

    // -------------------- UTILIDAD --------------------
    private void desconectarUsuario() {
        try {
            synchronized (users) {
                users.remove(clientName);
            }
            synchronized (groups) {
                for (Set<ClientHandler> grupo : groups.values()) {
                    grupo.remove(this);
                }
            }
            clientSocket.close();
            System.out.println("Cliente desconectado: " + clientName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getClientName() {
        return clientName;
    }
}
