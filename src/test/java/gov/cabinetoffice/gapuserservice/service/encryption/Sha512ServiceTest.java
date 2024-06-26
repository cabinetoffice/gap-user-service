package gov.cabinetoffice.gapuserservice.service.encryption;

import gov.cabinetoffice.gapuserservice.model.Salt;
import gov.cabinetoffice.gapuserservice.repository.SaltRepository;
import gov.cabinetoffice.gapuserservice.util.HelperUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Sha512ServiceTest {
    @InjectMocks
    private Sha512Service encryptionService;

    @Mock
    private SaltRepository saltRepository;

    @Test
    void generateAndStoreSalt() {
        String salt = "A_VERY_SECURE_SALT";
        String saltId = "saltId";
        try (MockedStatic<HelperUtils> utilities = Mockito.mockStatic(HelperUtils.class)) {
            utilities.when(() -> HelperUtils.generateSecureRandomString(255)).thenReturn(salt);
            utilities.when(HelperUtils::generateUUID).thenReturn(saltId);
            String result = encryptionService.generateAndStoreSalt();
            Assertions.assertEquals(saltId, result);
            final ArgumentCaptor<Salt> saltArgumentCaptor = ArgumentCaptor.forClass(Salt.class);
            verify(saltRepository).save(saltArgumentCaptor.capture());
            Assertions.assertEquals(salt, saltArgumentCaptor.getValue().getSaltValue());
            Assertions.assertEquals(saltId, saltArgumentCaptor.getValue().getSaltId());
        }
    }

    @Test
    void deleteSalt() {
        String salt = "A_VERY_SECURE_SALT";
        String saltId = "saltId";
        Salt saltModel = Salt.builder().saltValue(salt).saltId(saltId).build();
        when(saltRepository.findFirstBySaltIdOrderBySaltIdAsc(saltId)).thenReturn(Optional.ofNullable(saltModel));
        encryptionService.deleteSalt(saltId);
        assert saltModel != null;
        verify(saltRepository).delete(saltModel);
    }

    @Nested
    class GetSHA512SecurePasswordTest {
        String saltId = "saltId";
        // trust me
        String expectedHash = "95f43348cafc5543fd8e90216b4c27f4f52b0867ef8576428c1d4c7dfa020fb5b485d1fcd8efc217d614924552d66ccf7ccb7f56ea4fb6d976f2b27a880bb9ac";

        @Test
        void shouldGenerateSameHash_IfPayloadAndSaltAreTheSame() {
            String input = "input";
            String salt = "A_VERY_SECURE_SALT";
            Salt saltModel = Salt.builder().saltValue(salt).saltId(saltId).build();
            when(saltRepository.findFirstBySaltIdOrderBySaltIdAsc(saltId)).thenReturn(Optional.ofNullable(saltModel));
            String result = encryptionService.getSHA512SecurePassword(input, saltId);
            Assertions.assertEquals(expectedHash, result);
        }

        @Test
        void shouldNotGenerateSameHash_IfSaltIsDifferent() {
            String input = "input";
            String salt = "NOT_A_VERY_SECURE_SALT";
            Salt saltModel = Salt.builder().saltValue(salt).saltId(saltId).build();
            when(saltRepository.findFirstBySaltIdOrderBySaltIdAsc(saltId)).thenReturn(Optional.ofNullable(saltModel));
            String result = encryptionService.getSHA512SecurePassword(input, saltId);
            Assertions.assertNotEquals(expectedHash, result);
        }

        @Test
        void shouldNotGenerateSameHash_IfPayloadIsDifferent() {
            String input = "egg mcmuffin";
            String salt = "A_VERY_SECURE_SALT";
            Salt saltModel = Salt.builder().saltValue(salt).saltId(saltId).build();
            when(saltRepository.findFirstBySaltIdOrderBySaltIdAsc(saltId)).thenReturn(Optional.ofNullable(saltModel));
            String result = encryptionService.getSHA512SecurePassword(input, saltId);
            Assertions.assertNotEquals(expectedHash, result);
        }
    }
}
