package com.bim.seif.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
  private String token;         // access token (JWT)
  private String refreshToken;  // refresh token
  private int    expiresIn;     // segundos hasta expiración del access
}
