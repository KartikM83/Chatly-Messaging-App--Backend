// MessageDeleteRequestDTO.java
package org.example.new_chatly_backend.dto.messageDTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MessageDeleteRequestDTO {
    // "ME" or "EVERYONE"
    private String scope;
}
