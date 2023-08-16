package gov.cabinetofice.gapuserservice.service.encryption;

import gov.cabinetofice.gapuserservice.dto.EncryptedResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Optional;

@Service
public class AESService
{
    private static final Logger logger = LoggerFactory.getLogger(AESService.class);
    private static final int AES_KEY_SIZE = 256;
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final String KEY_SELECTOR = "AES";
    private static final String CIPHER_SELECTOR = "AES/GCM/NoPadding";

    private SecretKey secretKey;

    @Autowired
    private SigningService signingService;

    @Autowired
    private Sha512Service sha512Service;

    public AESService() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(KEY_SELECTOR);
        keyGenerator.init(AES_KEY_SIZE);

        secretKey = keyGenerator.generateKey();

    }

    public Optional<EncryptedResponseDto> encrypt(String message) {
        try {
            byte[] initVector = new byte[GCM_IV_LENGTH];
            GenerateInitVector(initVector);

            Cipher cipher = Cipher.getInstance(CIPHER_SELECTOR);
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getEncoded(), KEY_SELECTOR);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH , initVector);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);
            byte[] cipherText = cipher.doFinal(message.getBytes());

            EncryptedResponseDto response = new EncryptedResponseDto();
            response.setEncryptedText(DatatypeConverter.printHexBinary(cipherText));
            response.setIV(initVector);
            response.setPrivateKey(secretKey);
            response = signingService.signMessage(response);
            response.setShaSum(sha512Service.getSHA512SecurePassword(response.getEncryptedText() + response.getSignature()));
            return Optional.of(response);
        }
        catch (NoSuchAlgorithmException
                | InvalidKeyException
                | InvalidAlgorithmParameterException
                | NoSuchPaddingException
                | BadPaddingException
                | IllegalBlockSizeException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    private void GenerateInitVector(byte[] initVector) {
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(initVector);
    }

    public Optional<String> decrypt(EncryptedResponseDto response) {
        var shaSum  = response.getEncryptedText() + response.getSignature();
        shaSum = sha512Service.getSHA512SecurePassword(shaSum);

        if(signingService.verify(response) && response.getShaSum().equals(shaSum)) {
            try {
                Cipher cipher = Cipher.getInstance(CIPHER_SELECTOR);
                SecretKeySpec spec = new SecretKeySpec(secretKey.getEncoded(), KEY_SELECTOR);
                GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, response.getIV());
                cipher.init(Cipher.DECRYPT_MODE, spec, gcmSpec);
                return Optional.of(new String(cipher.doFinal(DatatypeConverter.parseHexBinary(response.getEncryptedText()))));
            }
            catch (NoSuchAlgorithmException
                    | InvalidKeyException
                    | InvalidAlgorithmParameterException
                    | NoSuchPaddingException
                    | BadPaddingException
                    | IllegalBlockSizeException e) {
                e.printStackTrace();
            }
        }
        else {
            logger.error("Failed to verify signature");
        }
        return Optional.empty();
    }

    public Optional<String> decrypt(final String message, final byte[] iv) {

        try {
            Cipher cipher = Cipher.getInstance(CIPHER_SELECTOR);
            SecretKeySpec spec = new SecretKeySpec(secretKey.getEncoded(), KEY_SELECTOR);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, spec, gcmSpec);
            return Optional.of(new String(cipher.doFinal(DatatypeConverter.parseHexBinary(message))));
        } catch (NoSuchAlgorithmException
                | InvalidKeyException
                | InvalidAlgorithmParameterException
                | NoSuchPaddingException
                | BadPaddingException
                | IllegalBlockSizeException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }
}
