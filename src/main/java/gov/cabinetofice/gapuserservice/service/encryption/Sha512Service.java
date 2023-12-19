package gov.cabinetofice.gapuserservice.service.encryption;

import gov.cabinetofice.gapuserservice.exceptions.NonceExpiredException;
import gov.cabinetofice.gapuserservice.model.Salt;
import gov.cabinetofice.gapuserservice.repository.SaltRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static gov.cabinetofice.gapuserservice.util.HelperUtils.generateSecureRandomString;
import static gov.cabinetofice.gapuserservice.util.HelperUtils.generateUUID;

@RequiredArgsConstructor
@Service
@Slf4j
public class Sha512Service {
    private final SaltRepository saltRepository;
    private static final int SALT_LENGTH = 255;

    private String generateSalt() {
        return generateSecureRandomString(SALT_LENGTH);
    }

    private String storeSalt(String salt) {
        String saltId = generateUUID();
        final Salt saltModel = Salt.builder().saltValue(salt).saltId(saltId).build();
        this.saltRepository.save(saltModel);
        return saltId;
    }

    public String generateAndStoreSalt() {
        return storeSalt(generateSalt());
    }

    private String getSalt(String saltId) {
        final Salt saltModel = this.saltRepository
                .findFirstBySaltIdOrderBySaltIdAsc(saltId)
                .orElseThrow(() -> new NonceExpiredException("Salt not found"));

        return saltModel.getSaltValue();
    }

    public void deleteSalt(String saltId) {
        saltRepository
                .findFirstBySaltIdOrderBySaltIdAsc(saltId)
                .ifPresent(this.saltRepository::delete);
    }

    public String getSHA512SecurePassword(String passwordToHash, String saltId) {
        String salt = getSalt(saltId);
        String generatedPassword = null;
        try {
            passwordToHash += salt;
            passwordToHash += getUserPepper(passwordToHash);
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte[] bytes = md.digest(passwordToHash.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte aByte : bytes) {
                sb.append(Integer.toString((aByte & 0xff) + 0x100, 16).substring(1));
            }
            generatedPassword = sb.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("A no such algorithm exception was thrown: ", e);
        }
        return generatedPassword;
    }

    private String getUserPepper(String saltedPassword) {
        return saltedPassword.substring(0, saltedPassword.length() / 2);
    }
}
