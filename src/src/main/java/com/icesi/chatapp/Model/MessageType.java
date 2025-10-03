package com.icesi.chatapp.Model;

public enum MessageType {

    LOGIN,
    REGISTER,
    LOGOUT,

    // Gestión de Grupos
    CREATE_GROUP,
    JOIN_GROUP,
    LEAVE_GROUP,
    ADD_MEMBER_TO_GROUP,
    REMOVE_MEMBER_FROM_GROUP,

    // Mensajería
    TEXT_MESSAGE,
    VOICE_MESSAGE,

    // Llamadas (señalización)
    CALL_REQUEST,
    CALL_ACCEPT,
    CALL_REJECT,
    CALL_END,

    // Respuestas del Servidor
    SERVER_RESPONSE_OK,     // Para indicar éxito en una operación
    SERVER_RESPONSE_ERROR,  // Para indicar un error en una operación
    CHAT_HISTORY_RESPONSE,  // Para enviar el historial de mensajes
    NEW_MESSAGE_NOTIFICATION, // Para notificar a los clientes sobre un nuevo mensaje
    GROUP_UPDATE_NOTIFICATION, // Para notificar a los clientes sobre cambios en un grupo
    USER_STATUS_UPDATE,     // Para notificar a los clientes sobre cambios de estado de otros usuarios

    // Opcionales para mejorar la UX
    GET_USER_LIST,
    GET_GROUP_LIST,
    GET_GROUP_MEMBERS
}