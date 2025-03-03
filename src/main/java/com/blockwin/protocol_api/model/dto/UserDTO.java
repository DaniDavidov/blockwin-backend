package com.blockwin.protocol_api.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private String username;

    private String email;

    private String firstName;

    private String lastName;
}
