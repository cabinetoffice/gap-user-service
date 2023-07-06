package gov.cabinetofice.gapuserservice.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

@ExtendWith(MockitoExtension.class)
public class UserTest {

    @Nested
    class isUserApplicant {
        @Test
        void shouldReturnFalseWhenUserIsAdmin() {
            final List<Role> roles = List.of(
                    Role.builder().name(RoleEnum.ADMIN).build(),
                    Role.builder().name(RoleEnum.APPLICANT).build(),
                    Role.builder().name(RoleEnum.FIND).build()
            );
            final User user = User.builder().roles(roles).build();

            final boolean response = user.isAnApplicant();

            Assertions.assertFalse(response);
        }

        @Test
        void shouldReturnFalseWhenUserIsSuperAdmin() {
            final List<Role> roles = List.of(
                    Role.builder().name(RoleEnum.SUPER_ADMIN).build(),
                    Role.builder().name(RoleEnum.ADMIN).build(),
                    Role.builder().name(RoleEnum.APPLICANT).build(),
                    Role.builder().name(RoleEnum.FIND).build()
            );
            final User user = User.builder().roles(roles).build();

            final boolean response = user.isAnApplicant();

            Assertions.assertFalse(response);
        }

        @Test
        void shouldReturnFalseWhenUserIsNotAnApplicant() {
            final User user = User.builder().build();

            final boolean response = user.isAnApplicant();

            Assertions.assertFalse(response);
        }

        @Test
        void shouldReturnTrueWhenUserIsApplicant() {
            final List<Role> roles = List.of(
                    Role.builder().name(RoleEnum.APPLICANT).build(),
                    Role.builder().name(RoleEnum.FIND).build()
            );
            final User user = User.builder().roles(roles).build();

            final boolean response = user.isAnApplicant();

            Assertions.assertTrue(response);
        }
    }

    @Nested
    class isUserAdmin {
        @Test
        void shouldReturnTrueWhenUserIsAdmin() {
            final List<Role> roles = List.of(
                    Role.builder().name(RoleEnum.ADMIN).build(),
                    Role.builder().name(RoleEnum.APPLICANT).build(),
                    Role.builder().name(RoleEnum.FIND).build()
            );
            final User user = User.builder().roles(roles).build();

            final boolean response = user.isAnAdmin();

            Assertions.assertTrue(response);
        }

        @Test
        void shouldReturnTrueWhenUserIsSuperAdmin() {
            final List<Role> roles = List.of(
                    Role.builder().name(RoleEnum.SUPER_ADMIN).build(),
                    Role.builder().name(RoleEnum.ADMIN).build(),
                    Role.builder().name(RoleEnum.APPLICANT).build(),
                    Role.builder().name(RoleEnum.FIND).build()
            );
            final User user = User.builder().roles(roles).build();

            final boolean response = user.isAnAdmin();

            Assertions.assertTrue(response);
        }

        @Test
        void shouldReturnFalseWhenUserIsAnApplicant() {
            final List<Role> roles = List.of(
                    Role.builder().name(RoleEnum.APPLICANT).build(),
                    Role.builder().name(RoleEnum.FIND).build()
            );
            final User user = User.builder().roles(roles).build();

            final boolean response = user.isAnAdmin();

            Assertions.assertFalse(response);
        }
    }

    @Nested
    class isUserSuperAdmin {
        @Test
        void shouldReturnFalseWhenUserIsAdmin() {
            final List<Role> roles = List.of(
                    Role.builder().name(RoleEnum.ADMIN).build(),
                    Role.builder().name(RoleEnum.APPLICANT).build(),
                    Role.builder().name(RoleEnum.FIND).build()
            );
            final User user = User.builder().roles(roles).build();

            final boolean response = user.isASuperAdmin();

            Assertions.assertFalse(response);
        }

        @Test
        void shouldReturnTrueWhenUserIsSuperAdmin() {
            final List<Role> roles = List.of(
                    Role.builder().name(RoleEnum.SUPER_ADMIN).build(),
                    Role.builder().name(RoleEnum.ADMIN).build(),
                    Role.builder().name(RoleEnum.APPLICANT).build(),
                    Role.builder().name(RoleEnum.FIND).build()
            );
            final User user = User.builder().roles(roles).build();

            final boolean response = user.isASuperAdmin();

            Assertions.assertTrue(response);
        }

        @Test
        void shouldReturnFalseWhenUserIsAnApplicant() {
            final List<Role> roles = List.of(
                    Role.builder().name(RoleEnum.APPLICANT).build(),
                    Role.builder().name(RoleEnum.FIND).build()
            );
            final User user = User.builder().roles(roles).build();

            final boolean response = user.isASuperAdmin();

            Assertions.assertFalse(response);
        }
    }

    @Nested
    class hasEmail {
        @Test
        void shouldReturnTrueWhenUserHasEmail() {
            final User user = User.builder().email("").build();

            final boolean response = user.hasEmail();

            Assertions.assertTrue(response);
        }

        @Test
        void shouldReturnFalseWhenUserDoesNotHaveEmail() {
            final User user = User.builder().build();

            final boolean response = user.hasEmail();

            Assertions.assertFalse(response);
        }
    }

    @Nested
    class hasSub {
        @Test
        void shouldReturnTrueWhenUserHasSub() {
            final User user = User.builder().sub("").build();

            final boolean response = user.hasSub();

            Assertions.assertTrue(response);
        }

        @Test
        void shouldReturnFalseWhenUserDoesNotHaveSub() {
            final User user = User.builder().build();

            final boolean response = user.hasSub();

            Assertions.assertFalse(response);
        }
    }
}
