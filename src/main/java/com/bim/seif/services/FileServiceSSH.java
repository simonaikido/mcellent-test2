package com.bim.seif.services;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.net.URLEncoder;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Properties;
import java.util.List;
import java.util.Vector;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class FileServiceSSH {

    @Value("${ftp.path}")
    private String FTP_REMOTE_PATH;
    @Value("${ftp.host}")
    private String FTP_HOST;
    @Value("${ftp.port}")
    private int FTP_PORT;
    @Value("${ftp.user}")
    private String FTP_USER;
    @Value("${ftp.password}")
    private String FTP_PASSWORD;

    /** Método síncrono: sube el archivo o lanza excepción. */
    public void enviarPorSFTP(String filename, String directorioUsuario, byte[] fileContent)
            throws IOException, JSchException, SftpException {

        log.info("Preparando envío de '{}' a la ruta relativa: {}", filename, directorioUsuario);

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(fileContent)) {
            sendSFTP(filename, directorioUsuario, inputStream);
            log.info("Archivo '{}' enviado correctamente.", filename);
        }
    }

    public static class ArchivoInfo {
        private String nombre;
        private String rutaCompleta;

        public ArchivoInfo(String nombre, String rutaCompleta) {
            this.nombre = nombre;
            this.rutaCompleta = rutaCompleta;
        }

        public String getNombre() {
            return nombre;
        }

        public void setNombre(String nombre) {
            this.nombre = nombre;
        }

        public String getRutaCompleta() {
            return rutaCompleta;
        }

        public void setRutaCompleta(String rutaCompleta) {
            this.rutaCompleta = rutaCompleta;
        }
    }

    /** Descarga (ya la tenías) */
    public boolean getFile(String nombreArchivo, OutputStream outputStream) throws JSchException, SftpException {
        log.info("Intentando descargar el archivo: {}", nombreArchivo);
        Session[] sessionHolder = new Session[1];
        ChannelSftp[] channelHolder = new ChannelSftp[1];

        try {
            connectSftp(new JSch(), sessionHolder, channelHolder);
            ChannelSftp channelSftp = channelHolder[0];

            String rutaCompleta = FTP_REMOTE_PATH + "/" + nombreArchivo.trim();
            log.debug("Ruta de descarga solicitada: {}", rutaCompleta);

            channelSftp.get(rutaCompleta, outputStream);
            log.info("Archivo '{}' transferido completamente.", nombreArchivo);
            return true;

        } catch (JSchException | SftpException e) {
            log.error("Fallo al descargar el archivo '{}'.", nombreArchivo, e);
            throw e;
        } finally {
            disconnectSftp(sessionHolder[0], channelHolder[0]);
        }
    }

    /** Interno: hace la subida ya conectada */
    private void sendSFTP(String filename, String directorioUsuario, ByteArrayInputStream inputStream)
            throws IOException, JSchException, SftpException {

        Session[] sessionHolder = new Session[1];
        ChannelSftp[] channelHolder = new ChannelSftp[1];

        try {
            connectSftp(new JSch(), sessionHolder, channelHolder);
            ChannelSftp channelSftp = channelHolder[0];

            // Normaliza el directorio relativo
            String rutaRel = normalizarRutaRelativa(directorioUsuario);
            // Crea directorios
            crearDirectoriosSFTP(FTP_REMOTE_PATH, rutaRel, channelSftp);

            // Cambiar al directorio destino
            String fullPath = construirPath(FTP_REMOTE_PATH, rutaRel);
            channelSftp.cd(fullPath);
            log.debug("Cambiado al directorio destino: {}", fullPath);

            // Subir el archivo
            channelSftp.put(inputStream, filename);
            log.info("Archivo {} subido exitosamente a {}", filename, fullPath);

        } finally {
            disconnectSftp(sessionHolder[0], channelHolder[0]);
        }
    }

    private void connectSftp(JSch jsch, Session[] sessionHolder, ChannelSftp[] channelHolder)
            throws JSchException {

        log.debug("Abriendo sesión SFTP a {}:{} con usuario {}", FTP_HOST, FTP_PORT, FTP_USER);

        Session session = jsch.getSession(FTP_USER, FTP_HOST, FTP_PORT);
        session.setPassword(FTP_PASSWORD);

        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        // Opcional, mejora compatibilidad:
        config.put("PreferredAuthentications", "publickey,password,keyboard-interactive");
        session.setConfig(config);

        session.connect(15000); // timeout
        log.info("Conexión SFTP establecida.");

        sessionHolder[0] = session;

        ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
        channelSftp.connect(15000); // timeout
        log.debug("Canal SFTP conectado.");

        channelHolder[0] = channelSftp;
    }

    private void disconnectSftp(Session session, ChannelSftp channelSftp) {
        if (channelSftp != null && channelSftp.isConnected()) {
            channelSftp.disconnect();
            log.debug("Canal SFTP desconectado.");
        }
        if (session != null && session.isConnected()) {
            session.disconnect();
            log.debug("Sesión SFTP desconectada.");
        }
    }

    private String normalizarRutaRelativa(String ruta) {
        if (ruta == null)
            return "";
        String limpia = ruta.replace("\\", "/").replaceAll("/+", "/");
        return limpia.replaceAll("^/|/$", "");
    }

    private String construirPath(String raiz, String relativa) {
        String r = raiz.endsWith("/") ? raiz.substring(0, raiz.length() - 1) : raiz;
        String rel = normalizarRutaRelativa(relativa);
        return rel.isEmpty() ? r : (r + "/" + rel);
    }

    private void crearDirectoriosSFTP(String pathRaiz, String rutaRelativa, ChannelSftp channelSftp)
            throws SftpException {

        String[] directorios = normalizarRutaRelativa(rutaRelativa).split("/");
        String pathAbsoluto = pathRaiz.endsWith("/") ? pathRaiz.substring(0, pathRaiz.length() - 1) : pathRaiz;

        log.debug("Iniciando creación de directorios bajo raíz: {}", pathAbsoluto);

        for (String directorio : directorios) {
            if (directorio.isEmpty())
                continue;
            pathAbsoluto += "/" + directorio;

            try {
                channelSftp.mkdir(pathAbsoluto);
                log.info("Directorio SFTP creado: {}", pathAbsoluto);
            } catch (SftpException e) {
                // Muchos servidores devuelven SSH_FX_FAILURE (4) si ya existe;
                // otros devuelven SSH_FX_FILE_ALREADY_EXISTS (11).
                if (e.id == ChannelSftp.SSH_FX_FAILURE) {
                    log.debug("El directorio {} ya existe.", pathAbsoluto);
                } else {
                    log.error("Fallo al crear el directorio {} (ID error: {}).", pathAbsoluto, e.id, e);
                    throw e;
                }
            }
        }
    }

    public List<ArchivoInfo> listarArchivosSFTP(String directorioRemoto) throws JSchException, SftpException {
        Session session = null;
        ChannelSftp channelSftp = null;
        List<ArchivoInfo> archivos = new ArrayList<>();

        try {
            JSch jsch = new JSch();
            session = jsch.getSession(FTP_USER, FTP_HOST, FTP_PORT);
            session.setPassword(FTP_PASSWORD);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
            log.info("Conexión SFTP establecida para listar archivos.");

            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();
            log.info("Canal SFTP abierto para listar archivos.");

            // Directorio base para los formatos BIM (asumiendo que están en la raíz del FTP
            // + un subdirectorio "formatosBIM")
            String rutaBase = FTP_REMOTE_PATH + "/formatosBIM";

            // Navegar al directorio remoto
            try {
                channelSftp.cd(rutaBase);
            } catch (SftpException e) {
                log.error("El directorio de formatos BIM no existe o no se puede acceder: {}", rutaBase, e);
                throw new SftpException(ChannelSftp.SSH_FX_NO_SUCH_FILE, "Directorio no encontrado: " + rutaBase);
            }

            // Listar archivos
            Vector<ChannelSftp.LsEntry> fileList = channelSftp.ls(".");
            for (ChannelSftp.LsEntry entry : fileList) {
                // Excluir directorios y las entradas especiales "." y ".."
                if (!entry.getAttrs().isDir() && !entry.getFilename().equals(".")
                        && !entry.getFilename().equals("..")) {
                    String nombreArchivo = entry.getFilename();

                    // --- 🔑 CLAVE: URL ENCODING PARA EL NOMBRE DEL ARCHIVO ---
                    String nombreArchivoCodificado;
                    try {
                        // 1. Codificar el nombre del archivo usando UTF-8.
                        nombreArchivoCodificado = URLEncoder.encode(nombreArchivo, StandardCharsets.UTF_8.toString())
                                // 2. Reemplazar '+' por '%20', ya que '+' se usa típicamente para espacios en
                                // query params,
                                // pero en la ruta de acceso (path segment) se prefiere el %20.
                                .replace("+", "%20");
                    } catch (Exception e) {
                        log.error("Error al codificar el nombre del archivo: {}", nombreArchivo, e);
                        // Si falla la codificación, usar el nombre original (no codificado)
                        nombreArchivoCodificado = nombreArchivo;
                    }

                    String rutaCompletaCodificada = rutaBase + "/" + nombreArchivoCodificado;

                    // Usar el nombre original (sin codificar) para el campo 'nombre' y la ruta
                    // codificada para 'rutaCompleta'
                    archivos.add(new ArchivoInfo(nombreArchivo, rutaCompletaCodificada));
                }
            }

            log.info("Se encontraron {} archivos en el directorio de formatos BIM.", archivos.size());

        } finally {
            if (channelSftp != null)
                channelSftp.disconnect();
            if (session != null)
                session.disconnect();
        }

        return archivos;
    }

    /** Descarga tolerante (no lanza, devuelve false si no existe) */
    public boolean getFileSmart(String nombreArchivo, OutputStream out) {
        Session[] sessionHolder = new Session[1];
        ChannelSftp[] channelHolder = new ChannelSftp[1];
        try {
            connectSftp(new JSch(), sessionHolder, channelHolder);
            ChannelSftp sftp = channelHolder[0];

            // construir ruta base (si ya viene absoluta, úsala)
            String fullPath = buildAbsolutePath(FTP_REMOTE_PATH, nombreArchivo);

            // 1) intento directo
            if (existsFile(sftp, fullPath)) {
                sftp.get(fullPath, out);
                log.info("Archivo transferido (directo): {}", fullPath);
                return true;
            }

            // 2) listar padre y resolver por comparaciones tolerantes
            String dir = parentDir(fullPath);
            String file = fileName(fullPath);
            String resolved = resolveByListing(sftp, dir, file);

            if (resolved != null && existsFile(sftp, resolved)) {
                sftp.get(resolved, out);
                log.info("Archivo transferido (resuelto): {}", resolved);
                return true;
            }

            log.warn("No such file en SFTP. Padre: {}  Buscado: {}", dir, file);
            return false;

        } catch (Exception e) {
            log.error("Error en getFileSmart('{}')", nombreArchivo, e);
            return false;
        } finally {
            disconnectSftp(sessionHolder[0], channelHolder[0]);
        }
    }

    /* ===== helpers usados por getFileSmart ===== */

    private static String buildAbsolutePath(String base, String path) {
        String p = sanitizeSlashes(path);
        String b = (base == null ? "" : sanitizeSlashes(base));
        p = collapseSpaces(p);
        if (p.startsWith("/"))
            return p;
        if (b.isEmpty() || "/".equals(b))
            return "/" + p;
        return b.endsWith("/") ? b + p : b + "/" + p;
    }

    private static String sanitizeSlashes(String s) {
        if (s == null)
            return "";
        String r = s.replace('\\', '/').replaceAll("/+", "/").trim();
        return java.text.Normalizer.normalize(r, java.text.Normalizer.Form.NFC);
    }

    private static String collapseSpaces(String s) {
        if (s == null)
            return "";
        return s.replace('\u00A0', ' ').replaceAll("\\s+", " ").trim();
    }

    private static String parentDir(String fullPath) {
        int i = fullPath.lastIndexOf('/');
        return (i > 0) ? fullPath.substring(0, i) : "/";
    }

    private static String fileName(String fullPath) {
        int i = fullPath.lastIndexOf('/');
        return (i >= 0) ? fullPath.substring(i + 1) : fullPath;
    }

    private boolean existsFile(ChannelSftp sftp, String path) {
        try {
            SftpATTRS a = sftp.stat(path);
            return a != null && !a.isDir();
        } catch (SftpException e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private String resolveByListing(ChannelSftp sftp, String dir, String expectedFile) {
        try {
            Vector<ChannelSftp.LsEntry> ls = sftp.ls(dir);
            if (ls == null)
                return null;
            List<String> files = ls.stream()
                    .filter(e -> !e.getAttrs().isDir())
                    .map(ChannelSftp.LsEntry::getFilename)
                    .collect(java.util.stream.Collectors.toList());

            for (String f : files)
                if (f.equals(expectedFile))
                    return join(dir, f);
            for (String f : files)
                if (f.equalsIgnoreCase(expectedFile))
                    return join(dir, f);

            String expNorm = norm(expectedFile);
            for (String f : files)
                if (norm(f).equalsIgnoreCase(expNorm))
                    return join(dir, f);

            String expNoMulti = collapseSpaces(expectedFile);
            for (String f : files)
                if (collapseSpaces(f).equalsIgnoreCase(expNoMulti))
                    return join(dir, f);

            return null;
        } catch (SftpException e) {
            log.error("No se pudo listar el directorio {}: {}", dir, e.getMessage());
            return null;
        }
    }

    private static String join(String dir, String name) {
        return dir.endsWith("/") ? dir + name : dir + "/" + name;
    }

    private static String norm(String s) {
        if (s == null)
            return "";
        String n = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return n.replace('\u00A0', ' ').replaceAll("\\s+", " ").trim();
    }
}