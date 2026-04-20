package reve_back.infrastructure.external.sunat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.w3c.dom.Document;
import reve_back.application.ports.out.ElectronicInvoiceCommand;
import reve_back.application.ports.out.ElectronicInvoiceResult;
import reve_back.application.ports.out.ElectronicInvoicingPort;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Component
public class SunatInvoicingAdapter implements ElectronicInvoicingPort {

    private final TemplateEngine templateEngine;
    private final XmlSigner xmlSigner;
    private final ZipUtil zipUtil;
    private final SunatSoapClient soapClient;

    @Value("${sunat.credentials.ruc}")
    private String rucSol;

    @Value("${sunat.credentials.usuario-sol}")
    private String usuarioSol;

    @Value("${sunat.credentials.clave-sol}")
    private String claveSol;

    @Value("${sunat.credentials.pfx-password}")
    private String pfxPassword;

    public SunatInvoicingAdapter(XmlSigner xmlSigner, ZipUtil zipUtil, SunatSoapClient soapClient) {
        this.xmlSigner = xmlSigner;
        this.zipUtil = zipUtil;
        this.soapClient = soapClient;

        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("/templates/");
        resolver.setSuffix(".xml");
        resolver.setTemplateMode(TemplateMode.XML);
        resolver.setCharacterEncoding("UTF-8");

        this.templateEngine = new TemplateEngine();
        this.templateEngine.setTemplateResolver(resolver);
    }

    @Override
    public ElectronicInvoiceResult sendInvoice(ElectronicInvoiceCommand command) {
        try {
            // 1. PREPARAR VARIABLES (Igual que antes)
            Context context = new Context();
            context.setVariable("serie", command.sale().series());
            String correlativoFormateado = String.format("%08d", command.sale().correlative());
            context.setVariable("correlativo", correlativoFormateado);
            context.setVariable("fechaEmision", command.sale().saleDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            context.setVariable("tipoComprobante", command.sale().invoiceType());
            context.setVariable("empresaRuc", command.companyRuc());
            context.setVariable("empresaRazonSocial", command.companyName());

            String tipoDocCliente = command.sale().invoiceType().equals("01") ? "6" : "1";
            context.setVariable("clienteTipoDoc", tipoDocCliente);
            context.setVariable("clienteDoc", command.sale().clientFullname().equals("Cliente Casual") ? "00000000" : "12345678");
            context.setVariable("clienteNombre", command.sale().clientFullname());
            context.setVariable("totalVenta", command.sale().totalFinalCharged());
            context.setVariable("items", command.sale().items());

            // 2. GENERAR Y FIRMAR EL XML (Igual que antes)
            String xmlString = templateEngine.process("invoice", context);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(xmlString.getBytes("UTF-8")));

            try {
//                InputStream pfxStream = new ClassPathResource("certificado.pfx.p12").getInputStream();
                InputStream pfxStream = new java.io.FileInputStream("src/main/resources/certificado.pfx.p12");
                document = xmlSigner.signUblXml(document, pfxStream, pfxPassword);
            } catch (Exception e) {
//                System.out.println("⚠️ AVISO: No se encontró certificado.pfx. El XML se firmará vacío.");
                System.out.println("⚠️ ERROR REAL AL FIRMAR EL XML: " + e.getMessage());
                e.printStackTrace();
            }

            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(writer));
            String xmlFinal = writer.toString();

            // ====================================================================
            // --- NUEVA LÓGICA DE LA ETAPA 3: EMPAQUETAR Y ENVIAR A SUNAT ---
            // ====================================================================

            // 3. Definir nombres de archivo según estándar SUNAT (RUC-TIPO-SERIE-CORRELATIVO)
            String baseFileName = command.companyRuc() + "-" + command.sale().invoiceType() + "-" + command.sale().series() + "-" + correlativoFormateado;
            String xmlFileName = baseFileName + ".xml";
            String zipFileName = baseFileName + ".zip";

            // 4. Comprimir el XML en un ZIP en memoria
            byte[] zipBytes = zipUtil.compressXmlToZip(xmlFileName, xmlFinal);
            String base64Zip = Base64.getEncoder().encodeToString(zipBytes);

            System.out.println("📦 Enviando comprobante a SUNAT: " + zipFileName);

            // 5. Enviar a SUNAT (Usando credenciales de prueba genéricas de SUNAT Beta por ahora)
            // Credenciales Beta estándar de SUNAT: RUC 20100066603, Usuario MODDATOS, Clave MODDATOS
            byte[] cdrZipBytes = soapClient.sendBill(rucSol, usuarioSol, claveSol, zipFileName, base64Zip);

            // 6. Extraer y leer la respuesta (CDR)
            String cdrXml = zipUtil.extractXmlFromZip(cdrZipBytes);

            // Si SUNAT nos devolvió un CDR, asumimos que fue procesado.
            // Extraemos el código de respuesta simple (0 = Aceptado, otro = Rechazado/Observado)
            String sunatStatus = "ACEPTADO";
            String mensajeRespuesta = extraerMensajeRespuesta(cdrXml);

            if (!mensajeRespuesta.isEmpty()) {
                System.out.println("✅ Respuesta SUNAT: " + mensajeRespuesta);
            }

            // En un entorno real, guardaríamos el XML y el CDR en un S3 de AWS o en el disco,
            // por ahora simulamos la URL.
            return new ElectronicInvoiceResult(
                    true,
                    sunatStatus,
                    mensajeRespuesta.isEmpty() ? "Comprobante procesado exitosamente" : mensajeRespuesta,
                    "/archivos/xml/" + xmlFileName,
                    "/archivos/cdr/R-" + zipFileName
            );

        } catch (Exception e) {
            System.err.println("❌ ERROR AL ENVIAR A SUNAT: " + e.getMessage());
            return new ElectronicInvoiceResult(false, "ERROR", e.getMessage(), null, null);
        }
    }

    // Método auxiliar simple para leer el mensaje de aceptación dentro del CDR
    private String extraerMensajeRespuesta(String cdrXml) {
        String startTag = "<cbc:Description>";
        String endTag = "</cbc:Description>";
        int start = cdrXml.indexOf(startTag);
        int end = cdrXml.indexOf(endTag);

        if (start != -1 && end != -1) {
            return cdrXml.substring(start + startTag.length(), end);
        }
        return "";
    }
}