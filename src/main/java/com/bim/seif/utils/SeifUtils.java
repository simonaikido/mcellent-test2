package com.bim.seif.utils;

import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;

public class SeifUtils {

    public static Timestamp obtenerTimestampActual() {
        return Timestamp.from(Instant.now());
    }
    public static LocalDateTime obtenerTimestampActualMas3Dias() {

        // Obtener el Timestamp actual
        Timestamp timestampActual = Timestamp.from(Instant.now());
        // Convertir a LocalDateTime
        LocalDateTime localDateTime = timestampActual.toLocalDateTime();
        // Sumar 3 días
        LocalDateTime nuevoLocalDateTime = localDateTime.plusDays(3);

        return nuevoLocalDateTime;
    }

    public String fotmatoTimestamp(Timestamp timestamp, String formato) {
        SimpleDateFormat sdf = new SimpleDateFormat(formato);
        return sdf.format(timestamp);
        // Ejemplo de salida: "2023-05-15"

    }

    public static LocalDateTime obtenerLocalDateActual() {

        // Obtener el Timestamp actual
        Timestamp timestampActual = Timestamp.from(Instant.now());
        // Convertir a LocalDateTime
        LocalDateTime localDateTime = timestampActual.toLocalDateTime();
        // Sumar 3 días


        return localDateTime;
    }

    public String guardarArchivo(String ruta, String nombreArchivo, MultipartFile file) {
        try {
            // Crear directorio si no existe
            Path uploadPath = Paths.get(ruta);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFilename = file.getOriginalFilename();
            int lastDotIndex = originalFilename.lastIndexOf('.');
            if (lastDotIndex == -1 || lastDotIndex == originalFilename.length() - 1) {
                return "";
            }

            String nombreArchivoCompleto = nombreArchivo + originalFilename.substring(lastDotIndex);
            if (originalFilename == null) {
                return "";
            }
            // Crear la ruta completa del archivo destino
            Path filePath = uploadPath.resolve(nombreArchivoCompleto);

            // Guardar el archivo en el sistema de archivos
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            return ruta + nombreArchivo + nombreArchivoCompleto;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String extraerNombreDeCorreo(String correo){
        int pos =  correo.indexOf("@");
        return correo.substring(0,pos);
    }

    public String extraerExtecionArchivo(String fileName){
        String[] archivoDividido = fileName.split("\\.");
        return "." + archivoDividido[(archivoDividido.length-1)];
    }
}
