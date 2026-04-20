package reve_back.infrastructure.external.sunat;

import org.apache.xml.security.Init;
import org.apache.xml.security.algorithms.MessageDigestAlgorithm;
import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.transforms.Transforms;
import org.apache.xml.security.utils.Constants;
import org.apache.xml.security.utils.ElementProxy;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

@Component
public class XmlSigner {

    // Inicializamos la librería de seguridad de Apache Santuario al cargar la clase
    static {
        Init.init();
        try {
            // CORRECCIÓN 1: Ahora requiere try-catch obligatorio
            ElementProxy.setDefaultPrefix(Constants.SignatureSpecNS, "ds");
        } catch (Exception e) {
            throw new RuntimeException("Error al inicializar prefijos de seguridad XML", e);
        }
    }

    /**
     * Firma un documento XML bajo el estándar UBL 2.1 de SUNAT.
     * @param document El XML en memoria
     * @param pfxStream El archivo del certificado digital (.pfx o .p12)
     * @param password La contraseña del certificado
     * @return El mismo documento XML pero con el nodo <ds:Signature> inyectado
     */
    public Document signUblXml(Document document, InputStream pfxStream, String password) throws Exception {

        // 1. Cargar el Certificado y la Clave Privada
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(pfxStream, password.toCharArray());

        String alias = keyStore.aliases().nextElement();
        PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, password.toCharArray());
        X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);

        // 2. Preparar el motor de firma
        XMLSignature sig = new XMLSignature(document, "", XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA256);

        // SUNAT exige que la firma vaya dentro del nodo <ext:ExtensionContent>
        NodeList extensionContentList = document.getElementsByTagNameNS("urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2", "ExtensionContent");

        if (extensionContentList.getLength() == 0) {
            // Si por alguna razón el prefijo es diferente, buscamos solo por el nombre local
            extensionContentList = document.getElementsByTagName("ext:ExtensionContent");
            if (extensionContentList.getLength() == 0) {
                throw new RuntimeException("No se encontró el nodo ext:ExtensionContent en el XML para inyectar la firma.");
            }
        }

        // Insertamos la estructura de firma dentro del XML
        Element extensionContent = (Element) extensionContentList.item(0);
        extensionContent.appendChild(sig.getElement());

        // 3. Configurar Transformaciones (Estándar exigido por SUNAT)
        Transforms transforms = new Transforms(document);
        transforms.addTransform(Transforms.TRANSFORM_ENVELOPED_SIGNATURE);

        // CORRECCIÓN 2: Usamos MessageDigestAlgorithm en lugar de Constants
        sig.addDocument("", transforms, MessageDigestAlgorithm.ALGO_ID_DIGEST_SHA256);

        // 4. Añadir la información del certificado público al XML
        sig.addKeyInfo(cert);
        sig.addKeyInfo(cert.getPublicKey());

        // 5. ¡Firmar! (Aplica algoritmos matemáticos pesados)
        sig.sign(privateKey);

        return document;
    }
}
