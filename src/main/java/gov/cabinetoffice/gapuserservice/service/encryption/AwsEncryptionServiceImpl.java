package gov.cabinetoffice.gapuserservice.service.encryption;

import com.amazonaws.encryptionsdk.AwsCrypto;
import com.amazonaws.encryptionsdk.CommitmentPolicy;
import com.amazonaws.encryptionsdk.CryptoResult;
import com.amazonaws.encryptionsdk.kmssdkv2.KmsMasterKey;
import com.amazonaws.encryptionsdk.kmssdkv2.KmsMasterKeyProvider;
import gov.cabinetoffice.gapuserservice.exceptions.EncryptionFailureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
public class AwsEncryptionServiceImpl {

    private final AwsCrypto crypto;
    private final KmsMasterKeyProvider keyProvider;

    final Map<String, String> encryptionContext = new HashMap<>();


    public AwsEncryptionServiceImpl(@Value("${aws.kms.key.arn}") String kmsKeyArn,
                                    @Value("${aws.kms.stage}") String stage,
                                    @Value("${aws.kms.origin}") String origin) {
        this.crypto = AwsCrypto.builder()
                .withCommitmentPolicy(CommitmentPolicy.RequireEncryptAllowDecrypt)
                .build();

        this.keyProvider = KmsMasterKeyProvider.builder()
                .defaultRegion(Region.EU_WEST_2)
                .buildStrict(kmsKeyArn);

        this.encryptionContext.put("purpose", "Gov.UK Grant Application Finder");
        this.encryptionContext.put("stage", stage);
        this.encryptionContext.put("origin", origin);
    }

    public byte[] encryptField(String field) {
        byte[] plaintextBytes = field.getBytes(StandardCharsets.UTF_8);

        try {
            final CryptoResult<byte[], KmsMasterKey> encryptResult = crypto.encryptData(keyProvider,
                    plaintextBytes, encryptionContext);

            return encryptResult.getResult();
        } catch (Exception e) {
            throw new EncryptionFailureException(e.getMessage());
        }
    }


    public String decryptField(byte[] cipheredField) {
        final CryptoResult<byte[], KmsMasterKey> decryptResult = crypto.decryptData(keyProvider, cipheredField);

        if (!encryptionContext.entrySet().stream()
                .allMatch(e -> e.getValue().equals(decryptResult.getEncryptionContext().get(e.getKey())))) {
            throw new IllegalStateException("Wrong Encryption Context!");
        }

        return new String(decryptResult.getResult(), StandardCharsets.UTF_8);
    }

}
