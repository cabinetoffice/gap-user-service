package gov.cabinetofice.gapuserservice.service.encryption;
import gov.cabinetofice.gapuserservice.dto.EncryptedResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.xml.bind.DatatypeConverter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.UnsupportedCharsetException;
import java.security.*;
import java.security.spec.ECGenParameterSpec;

@Service
public class SigningService
{
    //Logger for this class
    private static final Logger logger = LoggerFactory.getLogger(SigningService.class.getName());

    // Our keypairs
    private KeyPair keyPair;

    //Static values for configuring the signing algorithm
    private static final String KEY_PAIR_GENERATOR = "EC";
    private static final String ELLIPTIC_CURVE_SPEC = "secp256r1";
    private static final String SIGNING_SPEC = "SHA512withECDSA";

    public SigningService() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException
    {
        logger.info("Generating key pair: [{}, {}]",SIGNING_SPEC,ELLIPTIC_CURVE_SPEC);
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(KEY_PAIR_GENERATOR);
        keyGen.initialize(new ECGenParameterSpec(ELLIPTIC_CURVE_SPEC));
        keyPair = keyGen.generateKeyPair();
    }

    public EncryptedResponseDto signMessage(EncryptedResponseDto response)
    {
        response.setSignature(signMessage(response.getEncryptedText()));
        response.setSigningKeyPair(keyPair);
        return response;
    }

    public String signMessage(String message)
    {
        try
        {
            Signature signature = Signature.getInstance(SIGNING_SPEC);
            signature.initSign(keyPair.getPrivate());
            byte[] signedString = message.getBytes();
            signature.update(signedString);
            return DatatypeConverter.printHexBinary(signature.sign());
        }
        catch(NoSuchAlgorithmException
                | UnsupportedCharsetException
                | InvalidKeyException
                | SignatureException signatureException)
        {
            signatureException.printStackTrace();
            logger.error("Algorithm [{}] not found", SIGNING_SPEC);
        }
        return "";
    }

    public boolean verify(EncryptedResponseDto response)
    {
        this.keyPair = response.getSigningKeyPair();
        return verify(response.getEncryptedText(), response.getSignature());
    }
    private boolean verify(String message, String sign)
    {
        try
        {
            byte[] messageBytes = message.getBytes();
            Signature signature = Signature.getInstance(SIGNING_SPEC);
            signature.initVerify(keyPair.getPublic());
            signature.update(messageBytes);
            return signature.verify(DatatypeConverter.parseHexBinary(sign));
        }
        catch(NoSuchAlgorithmException
                | UnsupportedCharsetException
                | InvalidKeyException
                | SignatureException signatureException)
        {
            signatureException.printStackTrace();
        }
        return false;
    }
}
