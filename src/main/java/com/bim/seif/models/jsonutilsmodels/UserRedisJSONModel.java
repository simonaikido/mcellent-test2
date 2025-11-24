package com.bim.seif.models.jsonutilsmodels;

import lombok.Data;

@Data
public class UserRedisJSONModel {
        private String jwt;
        private String codeAuth;
        private String role;
        private int attempts;
}
