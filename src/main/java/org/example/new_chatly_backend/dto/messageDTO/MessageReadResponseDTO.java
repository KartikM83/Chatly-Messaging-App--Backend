package org.example.new_chatly_backend.dto.messageDTO;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MessageReadResponseDTO {
    private int markedRead;
}
