package com.bim.seif.controllers;


import com.bim.seif.models.Cliente;
import com.bim.seif.services.RedisService;
import javax.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class ActivityController {

    @Autowired
    private RedisService redisService;

    private int TOKEN_EXPIRATION_MINUTES =10;

    @PostMapping("/reportaractividad")
    public void reportarActividad(@RequestBody Cliente cliente) throws MessagingException {
        redisService.refreshTimeToken(cliente.getUsername(),TOKEN_EXPIRATION_MINUTES);
    }

}
