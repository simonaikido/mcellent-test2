package com.bim.seif.models.jsonutilsmodels;

import lombok.Data;
@Data
public class ResponseJSONModel {
        private Boolean admin = false;
        private String msg = "";
        private String JWT = "";
        private String role ="";
        private String codeAuth = "";
}
