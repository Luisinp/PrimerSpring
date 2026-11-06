package com.std.ec.PrimerSpring.service;
 
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
 
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
 
/**
 * Guarda las fotos de los paquetes en una carpeta fuera de la compilación
 * (por defecto ./uploads/paquetes), para que aparezcan sin reiniciar la aplicación.
 * Valida extensión, tamaño y firma real del archivo; el nombre original se descarta.
 */
@Service
public class ImagenService {
 
    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final Set<String> EXTENSIONES = Set.of("jpg", "jpeg", "png", "gif", "webp");
 
    private final Path carpeta;
 
    public ImagenService(@Value("${travelzen.uploads.dir:uploads}") String directorio) {
        this.carpeta = Paths.get(directorio, "paquetes").toAbsolutePath().normalize();
        try {
            Files.createDirectories(carpeta);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo crear la carpeta de imágenes: " + carpeta, e);
        }
    }
 
    public Path getCarpeta() {
        return carpeta;
    }
 
    /** Valida y guarda la imagen. Devuelve el nombre con el que quedó guardada. */
    public String guardar(byte[] contenido, String nombreOriginal) {
        if (contenido == null || contenido.length == 0) {
            throw new IllegalArgumentException("La imagen está vacía.");
        }
        if (contenido.length > MAX_BYTES) {
            throw new IllegalArgumentException("La imagen supera los 5 MB.");
        }
        String ext = extension(nombreOriginal);
        if (!EXTENSIONES.contains(ext) || !firmaValida(contenido)) {
            throw new IllegalArgumentException("El archivo no es una imagen válida (JPG, PNG, GIF o WEBP).");
        }
        String nombre = UUID.randomUUID() + "." + ext;
        try {
            Files.write(carpeta.resolve(nombre), contenido);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar la imagen", e);
        }
        return nombre;
    }
 
    public void borrar(String nombreArchivo) {
        if (nombreArchivo == null || nombreArchivo.isBlank()) {
            return;
        }
        try {
            Path archivo = carpeta.resolve(Paths.get(nombreArchivo).getFileName().toString()).normalize();
            if (archivo.startsWith(carpeta)) {
                Files.deleteIfExists(archivo);
            }
        } catch (IOException e) {
            // Borrar la foto no es crítico: si falla, solo queda un archivo suelto.
        }
    }
 
    private String extension(String nombre) {
        if (nombre == null || !nombre.contains(".")) {
            return "";
        }
        return nombre.substring(nombre.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
 
    // Revisa los primeros bytes: evita que alguien suba otro tipo de archivo con extensión .jpg
    private boolean firmaValida(byte[] b) {
        if (b.length < 12) {
            return false;
        }
        boolean jpg = (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF;
        boolean png = (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G';
        boolean gif = b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8';
        boolean webp = b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P';
        return jpg || png || gif || webp;
    }
}
 