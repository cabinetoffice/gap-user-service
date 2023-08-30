package gov.cabinetofice.gapuserservice.service.encryption;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

@Service
public class Sha512Service {
    private String systemSalt;
    private final int SALT_LENGTH = 255;

    public Sha512Service() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        systemSalt = new String(salt);
    }

    public String getSHA512SecurePassword(String passwordToHash) {
        String generatedPassword = null;
        try {
            passwordToHash += systemSalt;
            passwordToHash += getUserPepper(passwordToHash);
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte[] bytes = md.digest(passwordToHash.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte aByte : bytes) {
                sb.append(Integer.toString((aByte & 0xff) + 0x100, 16).substring(1));
            }
            generatedPassword = sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return generatedPassword;
    }

    private String getUserPepper(String saltedPassword) {
        return saltedPassword.substring(0, saltedPassword.length()/2);
    }
}
