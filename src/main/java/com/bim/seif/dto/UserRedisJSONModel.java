package com.bim.seif.dto;

import lombok.Data;

@Data
public class UserRedisJSONModel {
    private String jwt;
    private String codeAuth;
    private String role;
    private int attempts;
}
