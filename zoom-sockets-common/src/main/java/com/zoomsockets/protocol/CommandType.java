package com.zoomsockets.protocol;

public enum CommandType {
    // Client Requests (Server handles these)
    LOGIN_REQUEST,
    REGISTER_REQUEST,
    CREATE_ROOM,
    JOIN_ROOM_REQUEST,
    ADMIT_USER,
    LEAVE_ROOM,
    CHANGE_NAME_REQUEST,
    FILE_DOWNLOAD_REQUEST,

    // Server Responses (Client handles these)
    LOGIN_RESPONSE,
    REGISTER_RESPONSE,
    CREATE_ROOM_RESPONSE,
    JOIN_ROOM_RESPONSE,
    WAITING_ROOM_UPDATE,
    ROOM_MEMBERS_UPDATE,
    ROOM_TERMINATED,
    ROOM_CLOSED,
    FILE_SHARED,
    FILE_DOWNLOAD_RESPONSE,
    
    // Bidirectional (Both can send/receive depending on context)
    CHAT_MESSAGE,
    CHAT_BROADCAST,
    FILE_START,
    FILE_CHUNK,
    FILE_END,
    CAMERA_FRAME,
    CAMERA_FRAME_BROADCAST
}
