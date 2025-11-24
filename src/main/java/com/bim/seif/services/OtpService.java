package com.bim.seif.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class OtpService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * @param hashName La clave principal (nombre del hash, típicamente el email del usuario).
     * @param key La clave dentro del hash (ej. "OTP").
     * @param value El valor a almacenar (ej. el código OTP).
     * @param timeout El tiempo de expiración.
     * @param timeUnit La unidad de tiempo.
     */
    public void setHashValue(String hashName, String key, String value, long timeout, TimeUnit timeUnit) {
        log.info("REDIS: Guardando valor hash en '{}' con clave '{}'. Expiracion: {} {}", 
                 hashName, key, timeout, timeUnit.toString());
        try {
            HashOperations<String, String, String> hashOperations = redisTemplate.opsForHash();
            hashOperations.put(hashName, key, value);
            redisTemplate.expire(hashName, timeout, timeUnit);
            log.debug("REDIS: Hash guardado y tiempo de vida establecido para '{}'.", hashName);
        } catch (Exception e) {
            log.error("ERROR REDIS: Fallo al establecer el valor hash en '{}'.", hashName, e);
            throw new RuntimeException("Error de comunicación con Redis.", e);
        }
    }

    /**
     * @param hashName La clave principal (típicamente el email del usuario).
     * @param key La clave del contador dentro del hash (ej. "INTENTOS").
     * @param timeout El tiempo de expiración.
     * @param timeUnit La unidad de tiempo.
     * @return El nuevo valor del contador.
     */
    public Long incrementHashValue(String hashName, String key, long timeout, TimeUnit timeUnit) {
        log.info("REDIS: Incrementando contador '{}' en hash '{}'. Expiracion: {} {}", 
                 key, hashName, timeout, timeUnit.toString());
        try {
            HashOperations<String, String, Object> hashOperations = redisTemplate.opsForHash();
            Long intentos = hashOperations.increment(hashName, key, 1);
            redisTemplate.expire(hashName, timeout, timeUnit);
            log.debug("REDIS: Nuevo conteo para '{}': {}. Tiempo de vida actualizado.", hashName, intentos);
            return intentos;
        } catch (Exception e) {
            log.error("ERROR REDIS: Fallo al incrementar el contador hash en '{}'.", hashName, e);
            throw new RuntimeException("Error de comunicacion con Redis.", e);
        }
    }

    /**
     * Elimina una clave específica dentro de un hash.
     * @param hashName La clave principal del hash.
     * @param key La clave dentro del hash a eliminar.
     */
    public void deleteHashValue(String hashName, String key) {
        log.info("REDIS: Eliminando clave '{}' dentro del hash '{}'.", key, hashName);
        try {
            HashOperations<String, String, Object> hashOperations = redisTemplate.opsForHash();
            hashOperations.delete(hashName, key);
            log.debug("REDIS: Clave '{}' eliminada del hash '{}'.", key, hashName);
        } catch (Exception e) {
            log.error("ERROR REDIS: Fallo al eliminar clave hash en '{}'.", hashName, e);
            
        }
    }

    /**
     * Obtiene todos los pares clave-valor de un hash.
     * @param hashName La clave principal del hash.
     * @return Un mapa con las entradas del hash.
     */
    public Map<String, Object> getUserData(String hashName) {
        log.info("REDIS: Obteniendo todos los datos del hash: {}", hashName);
        try {
            HashOperations<String, String, Object> hashOperations = redisTemplate.opsForHash();
            Map<String, Object> data = hashOperations.entries(hashName);
            log.debug("REDIS: Datos obtenidos para '{}'. Total de entradas: {}", hashName, data.size());
            return data;
        } catch (Exception e) {
            log.error("ERROR REDIS: Fallo al obtener todos los datos del hash '{}'.", hashName, e);
            throw new RuntimeException("Error de comunicacion con Redis.", e);
        }
    }

    
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
        log.debug("REDIS: Obteniendo valor para la clave: {}", key);
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
        log.info("REDIS: Solicitando eliminacion de la clave: {}", key);
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
}