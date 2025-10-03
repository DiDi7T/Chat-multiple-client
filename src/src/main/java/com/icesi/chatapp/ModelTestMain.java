
package com.icesi.chatapp;
import com.icesi.chatapp.Model.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Arrays;
import java.util.List;

public class ModelTestMain {

    public static void main(String[] args) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create(); // Para JSON legible

        System.out.println("--- Probando User ---");
        User user1 = new User("alice", "hashed_pass_alice", "Online");
        System.out.println("Usuario creado: " + user1);
        String userJson = gson.toJson(user1);
        System.out.println("JSON de Usuario: " + userJson);
        User user2 = gson.fromJson(userJson, User.class);
        System.out.println("Usuario deserializado: " + user2);
        System.out.println("Equals (user1 == user2)? " + user1.equals(user2)); // Debería ser true por username
        System.out.println("---------------------\n");


        System.out.println("--- Probando Group ---");
        List<String> initialMembers = Arrays.asList("alice", "bob");
        Group group1 = new Group("AmigosDelAlma", "alice");
        group1.addMember("bob");
        System.out.println("Grupo creado: " + group1);
        String groupJson = gson.toJson(group1);
        System.out.println("JSON de Grupo: " + groupJson);
        Group group2 = gson.fromJson(groupJson, Group.class);
        System.out.println("Grupo deserializado: " + group2);
        System.out.println("Miembros del grupo deserializado: " + group2.getMembers());
        System.out.println("Equals (group1 == group2)? " + group1.equals(group2)); // Debería ser true por groupId
        System.out.println("---------------------\n");


        System.out.println("--- Probando TextMessage ---");
        TextMessage textMsg1 = new TextMessage("alice", "bob", "Hola Bob, ¿cómo estás?");
        System.out.println("Mensaje de texto creado: " + textMsg1);
        String textMsgJson = gson.toJson(textMsg1);
        System.out.println("JSON de Mensaje de Texto: " + textMsgJson);
        TextMessage textMsg2 = gson.fromJson(textMsgJson, TextMessage.class);
        System.out.println("Mensaje de Texto deserializado: " + textMsg2);
        System.out.println("Contenido: " + textMsg2.getContent());
        System.out.println("Tipo: " + textMsg2.getType()); // Debería ser TEXT_MESSAGE
        System.out.println("Equals (textMsg1 == textMsg2)? " + textMsg1.equals(textMsg2)); // Debería ser true por messageId
        System.out.println("---------------------\n");


        System.out.println("--- Probando VoiceMessage ---");
        VoiceMessage voiceMsg1 = new VoiceMessage("alice", "group123", "audio_20231027_153000.wav");
        System.out.println("Mensaje de voz creado: " + voiceMsg1);
        String voiceMsgJson = gson.toJson(voiceMsg1);
        System.out.println("JSON de Mensaje de Voz: " + voiceMsgJson);
        VoiceMessage voiceMsg2 = gson.fromJson(voiceMsgJson, VoiceMessage.class);
        System.out.println("Mensaje de Voz deserializado: " + voiceMsg2);
        System.out.println("Nombre de archivo de audio: " + voiceMsg2.getAudioFileName());
        System.out.println("Tipo: " + voiceMsg2.getType()); // Debería ser VOICE_MESSAGE
        System.out.println("Equals (voiceMsg1 == voiceMsg2)? " + voiceMsg1.equals(voiceMsg2)); // Debería ser true por messageId
        System.out.println("---------------------\n");
    }
}