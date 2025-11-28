package org.example.new_chatly_backend.entity.messageEntity;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum MessageStatus {
    SENT, DELIVERED, SEEN;

    @JsonCreator
    public static MessageStatus fromString(String value) {
        if (value == null) {
            return null;
        }
        return MessageStatus.valueOf(value.trim().toUpperCase());
    }


}
