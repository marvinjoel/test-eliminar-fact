package reve_back.infrastructure.external.sunat;

import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Component
public class ZipUtil {
    /**
     * Toma el texto de tu XML firmado y lo mete dentro de un archivo .zip en memoria.
     */
    public byte[] compressXmlToZip(String xmlFileName, String xmlContent) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {

            // Creamos el archivo dentro del zip con el nombre exigido por SUNAT
            ZipEntry entry = new ZipEntry(xmlFileName);
            zos.putNextEntry(entry);

            // Escribimos el contenido del XML
            zos.write(xmlContent.getBytes("UTF-8"));
            zos.closeEntry();
        }
        return baos.toByteArray();
    }

    /**
     * Abre el .zip que nos devuelve SUNAT y saca el texto del XML (CDR) que viene adentro.
     */
    public String extractXmlFromZip(byte[] zipBytes) throws Exception {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry = zis.getNextEntry();

            while (entry != null) {
                // Buscamos el archivo que termine en .xml
                if (entry.getName().toLowerCase().endsWith(".xml")) {
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    int nRead;
                    byte[] data = new byte[1024];

                    while ((nRead = zis.read(data, 0, data.length)) != -1) {
                        buffer.write(data, 0, nRead);
                    }
                    return buffer.toString("UTF-8");
                }
                entry = zis.getNextEntry();
            }
        }
        throw new RuntimeException("No se encontró ningún archivo XML (CDR) dentro del ZIP de respuesta de SUNAT.");
    }
}
