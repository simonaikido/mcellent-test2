package com.bim.seif.services;

import com.bim.seif.models.Cliente;
import com.bim.seif.models.jsonutilsmodels.UserRedisJSONModel;
import com.bim.seif.utils.JSONUtils;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class RedisService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    private UserRedisJSONModel userRedisJSONModel = new UserRedisJSONModel(); 


    public void setValue(String key, String value) {
        log.debug("REDIS: Estableciendo valor simple para la clave: {}", key);
        try {
            redisTemplate.opsForValue().set(key, value);
        } catch (Exception e) {
            log.error("ERROR REDIS: Fallo al establecer el valor para la clave '{}'.", key, e);
        }
    }

    public void setValueWithExpiration(String key, String value, long timeout, TimeUnit unit) {
        log.info("REDIS: Estableciendo valor para la clave '{}' con expiracion: {} {}", key, timeout, unit.toString());
        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
        } catch (Exception e) {
            log.error("ERROR REDIS: Fallo al establecer el valor con expiracion para la clave '{}'.", key, e);
        }
    }

    public String getValue(String key) {
        log.debug("REDIS: Intentando obtener valor para la clave: {}", key);
        try {
            String value = (String) redisTemplate.opsForValue().get(key);
            log.debug("REDIS: Valor obtenido para '{}': {}", key, (value != null ? "ENCONTRADO" : "NULL"));
            return value;
        } catch (Exception e) {
            log.error("ERROR REDIS: Fallo al obtener el valor para la clave '{}'.", key, e);
            return null;
        }
    }

    public Boolean deleteKey(String key) {
        log.info("REDIS: Solicitando eliminación de la clave: {}", key);
        try {
            Boolean deleted = redisTemplate.delete(key);
            log.info("REDIS: Eliminación de la clave '{}': {}", key, deleted ? "EXITOSA" : "NO ENCONTRADA");
            return deleted;
        } catch (Exception e) {
            log.error("ERROR REDIS: Fallo al eliminar la clave '{}'.", key, e);
            return false;
        }
    }


    public void refreshTimeToken(String key, int minuts) {
        log.info("REDIS: Refrescando tiempo de expiracion para la clave '{}' a {} minutos.", key, minuts);
        try {
            redisTemplate.expire(key, minuts, TimeUnit.MINUTES);
            log.debug("REDIS: Tiempo de expiracion refrescado para la clave '{}'.", key);
        } catch (Exception e) {
            log.error("ERROR REDIS: Fallo al refrescar el tiempo de expiracion para la clave '{}'.", key, e);
        }
    }

    public void guardarUsuarioRedis(Cliente cliente, String role, int attempts, String jwt, String codeAuth, int minutes) {
        String key = cliente.getUsername();
        log.info("REDIS USUARIO: Iniciando guardado de datos de sesion/OTP para el usuario: {}", key);
        

        String userRedisManageJSON = JSONUtils.userRedisManage(role, attempts, jwt, codeAuth);
        
        log.debug("REDIS USUARIO: JSON de usuario creado. Role: {}, Attempts: {}, JWT Status: {}, OTP Status: {}", 
                  role, attempts, jwt != null ? "Presente" : "Ausente", codeAuth != null ? "Presente" : "Ausente");


        guardarValorLlaveRedis(key, userRedisManageJSON, minutes);
        log.info("REDIS USUARIO: Datos de usuario guardados en '{}'. Expiracion: {} minutos.", key, minutes);
    }


    public void guardarValorLlaveRedis(String key, String value, int minutes) {
        // Llama al método setValueWithExpiration con la unidad de minutos
        setValueWithExpiration(key, value, minutes, TimeUnit.MINUTES);
    }

    public String obtenerCodigoAuthRedis(Cliente cliente) {
        String key = cliente.getUsername();
        log.info("REDIS USUARIO: Intentando obtener código de autenticacionpara el usuario: {}", key);
        
        try {
            String userRedisJSON = getValue(key);
            
            if (userRedisJSON == null) {
                log.warn("REDIS USUARIO: No se encontraron datos de usuario para '{}'. OTP no disponible.", key);
                return null;
            }

            userRedisJSONModel = new UserRedisJSONModel();
            userRedisJSONModel = JSONUtils.covertJSONToUserRedisJSONModel(userRedisJSON, userRedisJSONModel);
            
            String codeAuth = userRedisJSONModel.getCodeAuth();
            log.debug("REDIS USUARIO: OTP recuperado para '{}': {}", key, codeAuth != null ? "ENCONTRADO" : "NULL");
            return codeAuth;
            
        } catch (Exception e) {
            log.error("ERROR REDIS USUARIO: Fallo al obtener/parsear datos JSON de usuario para '{}'.", key, e);
            return null;
        }
    }
}