package gov.cabinetofice.gapuserservice.service.encryption;

import gov.cabinetofice.gapuserservice.exceptions.NonceExpiredException;
import gov.cabinetofice.gapuserservice.model.Salt;
import gov.cabinetofice.gapuserservice.repository.SaltRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import static gov.cabinetofice.gapuserservice.util.HelperUtils.generateSecureRandomString;
import static gov.cabinetofice.gapuserservice.util.HelperUtils.generateUUID;

@RequiredArgsConstructor
@Service
public class Sha512Service {
    private final SaltRepository saltRepository;
    private final int SALT_LENGTH = 255;

    private String generateSalt() {
        return generateSecureRandomString(SALT_LENGTH);
    }

    private String storeSalt(String salt) {
        String saltId = generateUUID();
        final Salt saltModel = Salt.builder().salt(salt).saltId(saltId).build();
        this.saltRepository.save(saltModel);
        return saltId;
    }

    public String generateAndStoreSalt() {
        return storeSalt(generateSalt());
    }

    private String getSalt(String saltId) {
        final Optional<Salt> saltModel = this.saltRepository.findFirstBySaltIdOrderBySaltIdAsc(saltId);
        if (!saltModel.isPresent()) {
            throw new NonceExpiredException("Salt not found");
        }
        return saltModel.get().getSalt();
    }

    public void deleteSalt(String saltId) {
        final Optional<Salt> saltModel = this.saltRepository.findFirstBySaltIdOrderBySaltIdAsc(saltId);
        saltModel.ifPresent(this.saltRepository::delete);
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
            e.printStackTrace();
        }
        return generatedPassword;
    }

    private String getUserPepper(String saltedPassword) {
        return saltedPassword.substring(0, saltedPassword.length()/2);
    }
}
