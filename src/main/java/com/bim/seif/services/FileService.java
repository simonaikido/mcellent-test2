package com.bim.seif.services;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
public class FileService {

    @Value("${ftp.host}")
    private String server;
    @Value("${ftp.port}")
    private int port;
    @Value("${ftp.user}")
    private String user;
    @Value("${ftp.password}")
    private String pass;
    @Value("${ftp.path}")
    private String remotePath;

    // Constantes de tiempo de espera
    private static final int TIMEOUT_MS = 30000;


    public Mono<Void> enviarPorSFTP(String filename, String directorioUsuario, byte[] fileContent) throws JSchException, SftpException {
        
        // Mueve la operación de bloqueo (sendSFTP) al Scheduler de IO.
        return Mono.fromRunnable(() -> {
            
            log.info("Preparando envio de '{}' a la ruta: {}", filename, directorioUsuario);
            
            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(fileContent)) {
                
                sendFTP(filename, directorioUsuario, inputStream);
                
                log.info("Archivo '{}' enviado correctamente.", filename);
                
            } catch (IOException e) {
                
                // Registra el error detallado (causa raíz)
                log.error("ERROR CRÍTICO SFTP: Fallo al enviar archivo '{}' al directorio '{}'. Causa: {}", 
                          filename, directorioUsuario, e.getMessage(), e); 
                
                // Lanza RuntimeException para que el flujo reactivo falle
                throw new RuntimeException("Error al enviar archivo por SFTP. Revisar logs para la causa raiz.", e);
            }
        // Suscribe la operación de bloqueo a un scheduler para tareas de E/S de bloqueo.
        }).subscribeOn(Schedulers.boundedElastic()).then(); 
    }


    public void sendFTP(String filename, String directorioUsuario, ByteArrayInputStream inputStream)
            throws IOException {
        
        FTPClient ftpClient = new FTPClient();
        
        try {
            // Reemplazando la lógica de conexión repetida
            connectFTP(ftpClient);

            // Ajustes del cliente FTP
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            

            ftpClient.enterLocalPassiveMode(); 
            
            ftpClient.setControlKeepAliveTimeout(300);

            if (crearDirectorios(remotePath, directorioUsuario, ftpClient)) {
                log.info("Ruta destino creada exitosamente.");
            } else {
                log.debug("La ruta destino ya existía o fallo la creación de un nivel superior.");
            }


            String fullPath = remotePath + "/" + directorioUsuario;
            if (!ftpClient.changeWorkingDirectory(fullPath)) {
                log.error("Fallo al acceder al directorio remoto {}. Verifique permisos o existencia.", fullPath);
                throw new IOException("No se pudo acceder al directorio remoto: " + fullPath);
            }
            log.info("Directorio remoto OK: {}", fullPath);

            if (!ftpClient.storeFile(filename, inputStream)) {
                log.error("Error al subir archivo '{}'. Respuesta del servidor: {}", filename, ftpClient.getReplyString());
                throw new IOException("Fallo en la transferencia del archivo: " + ftpClient.getReplyString());
            }

            log.info("Archivo '{}' subido exitosamente a {}.", filename, fullPath);

        } finally {
            // Reemplazando la lógica de desconexión repetida
            disconnectFTP(ftpClient);
        }
    }

    private void connectFTP(FTPClient ftpClient) throws IOException {
        
        log.info("Intentando conectar a FTP: {}:{} con usuario {}", server, port, user);

        ftpClient.setDefaultTimeout(TIMEOUT_MS);
        ftpClient.setConnectTimeout(TIMEOUT_MS);
        
        // Conexión
        ftpClient.connect(server, port);
        
        int reply = ftpClient.getReplyCode();
        if (!FTPReply.isPositiveCompletion(reply)) {
            ftpClient.disconnect();
            throw new IOException("Excepcion al conectar al servidor FTP. Respuesta: " + reply);
        }

        // Login
        if (!ftpClient.login(user, pass)) {
            ftpClient.disconnect();
            throw new IOException("Fallo de autenticacion para el usuario: " + user);
        }
        
        log.info("Conexion y login FTP exitosos.");
    }
    

    private void disconnectFTP(FTPClient ftpClient) {
        if (ftpClient.isConnected()) {
            try {
                ftpClient.logout();
            } catch (IOException e) {
                log.warn("Error al intentar logout del servidor FTP.", e);
            }
            try {
                ftpClient.disconnect();
                log.info("Cliente FTP desconectado.");
            } catch (IOException e) {
                log.error("Error al cerrar la conexion FTP.", e);
            }
        }
    }

    private boolean crearDirectorios(String pathRaiz, String rutaRelativa, FTPClient ftpClient) throws IOException {
        boolean seCreoAlguno = false;

        String rutaLimpia = rutaRelativa.replaceAll("^/|/$", "");
        String[] directorios = rutaLimpia.split("/");

        log.debug("CREACION DIRECTORIOS FTP. Directorios a crear: {}", Arrays.toString(directorios));

        String pathAbsoluto = pathRaiz;

        for (String directorio : directorios) {

            if (directorio.isEmpty()) {
                continue;
            }

            pathAbsoluto += "/" + directorio;

            // Intenta cambiar al directorio para ver si ya existe
            if (ftpClient.changeWorkingDirectory(pathAbsoluto)) {
                log.debug("Directorio ya existe: {}", pathAbsoluto);
                continue;
            }
            
            // Si no existe, intenta crearlo
            boolean isCreated = ftpClient.makeDirectory(pathAbsoluto);

            if (isCreated) {
                seCreoAlguno = true;
                log.debug("Directorio creado exitosamente: {}", pathAbsoluto);
            } else {
                // Fallo en la creación (permisos o error desconocido)
                log.debug("Fallo al crear directorio: {}. Respuesta: {}", pathAbsoluto, ftpClient.getReplyString());
            }
        }

        log.debug("CREACION DIRECTORIOS FTP. Se creo al menos un directorio: {}", seCreoAlguno);
        
        // Regresa al directorio raíz antes de terminar
        ftpClient.changeWorkingDirectory("/");
        return seCreoAlguno;
    }

}