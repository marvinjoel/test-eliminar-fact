package reve_back.infrastructure.external.sunat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class SunatSoapClient {
    // URL del entorno de pruebas (Homologación) de SUNAT
    @Value("${sunat.ws.url}")
    private String sunatUrl;

    /**
     * Envía el ZIP a SUNAT y devuelve los bytes del ZIP de respuesta (CDR).
     */
    public byte[] sendBill(String ruc, String usuarioSol, String claveSol, String zipFileName, String base64ZipContent) throws Exception {

        // 1. Construir el sobre SOAP con el estándar estricto de seguridad WS-Security de SUNAT
        String soapEnvelope = """
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ser="http://service.sunat.gob.pe" xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd">
                    <soapenv:Header>
                        <wsse:Security>
                            <wsse:UsernameToken>
                                <wsse:Username>%s%s</wsse:Username>
                                <wsse:Password>%s</wsse:Password>
                            </wsse:UsernameToken>
                        </wsse:Security>
                    </soapenv:Header>
                    <soapenv:Body>
                        <ser:sendBill>
                            <fileName>%s</fileName>
                            <contentFile>%s</contentFile>
                        </ser:sendBill>
                    </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(ruc, usuarioSol, claveSol, zipFileName, base64ZipContent);

        // 2. Preparar el cliente HTTP nativo de Java (no requiere librerías extra)
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();

        // 3. Crear la petición HTTP (POST)
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(sunatUrl))
                .header("Content-Type", "text/xml; charset=utf-8")
                .header("SOAPAction", "urn:sendBill")
                .POST(HttpRequest.BodyPublishers.ofString(soapEnvelope))
                .build();

        // 4. Enviar a SUNAT y esperar respuesta
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // 5. Analizar la respuesta
        String responseBody = response.body();

        if (response.statusCode() != 200) {
            // Si el código HTTP no es 200 OK, SUNAT devolvió un error
            throw new RuntimeException("SUNAT rechazó la petición: " + extractFaultString(responseBody));
        }

        // Si fue exitoso, extraemos el ZIP de respuesta (CDR) que viene en base64
        String base64Response = extractApplicationResponse(responseBody);
        return java.util.Base64.getDecoder().decode(base64Response);
    }

    // --- MÉTODOS AUXILIARES (Para cortar el texto XML rápido y sin librerías pesadas) ---

    private String extractApplicationResponse(String soapResponse) {
        String startTag = "<applicationResponse>";
        String endTag = "</applicationResponse>";
        int start = soapResponse.indexOf(startTag);
        int end = soapResponse.indexOf(endTag);

        if (start != -1 && end != -1) {
            return soapResponse.substring(start + startTag.length(), end);
        }
        throw new RuntimeException("La petición fue exitosa, pero SUNAT no devolvió el archivo de respuesta (applicationResponse).");
    }

    private String extractFaultString(String soapResponse) {
        String startTag = "<faultstring>";
        String endTag = "</faultstring>";
        int start = soapResponse.indexOf(startTag);
        int end = soapResponse.indexOf(endTag);

        if (start != -1 && end != -1) {
            return soapResponse.substring(start + startTag.length(), end);
        }
        return "Error desconocido de comunicación. Código HTTP recibido.";
    }
}
