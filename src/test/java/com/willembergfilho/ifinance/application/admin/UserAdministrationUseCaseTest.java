package com.willembergfilho.ifinance.application.admin;

import com.willembergfilho.ifinance.infrastructure.persistence.InvestmentJpaRepository;
import com.willembergfilho.ifinance.infrastructure.persistence.SimulationJpaRepository;
import com.willembergfilho.ifinance.infrastructure.persistence.UserIdentityJpaRepository;
import com.willembergfilho.ifinance.infrastructure.persistence.UserJpaRepository;
import com.willembergfilho.ifinance.infrastructure.persistence.entity.UserEntity;
import com.willembergfilho.ifinance.infrastructure.persistence.entity.UserEntity.Role;
import com.willembergfilho.ifinance.infrastructure.security.UserSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserAdministrationUseCaseTest {

    private UserJpaRepository userRepository;
    private SimulationJpaRepository simulationRepository;
    private InvestmentJpaRepository investmentRepository;
    private UserAdministrationUseCase useCase;

    private UserEntity admin;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserJpaRepository.class);
        simulationRepository = mock(SimulationJpaRepository.class);
        investmentRepository = mock(InvestmentJpaRepository.class);
        useCase = new UserAdministrationUseCase(userRepository, simulationRepository, investmentRepository,
                new UserSyncService(userRepository, mock(UserIdentityJpaRepository.class), "boot@x.com"));

        admin = new UserEntity(UUID.randomUUID(), "a", "admin@x.com", "Admin", "google", null, null);
        admin.setRole(Role.ADMIN);
        user = new UserEntity(UUID.randomUUID(), "u", "user@x.com", "User", "github", null, null);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.findByRole(Role.ADMIN)).thenReturn(List.of(admin));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void changeRole_adminPromotesUser() {
        Optional<UserEntity> result = useCase.changeRole(admin.getId(), user.getId(), Role.ADMIN);

        assertThat(result).containsSame(user);
        assertThat(user.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void changeRole_selfDemotion_isRejected() {
        assertThatThrownBy(() -> useCase.changeRole(admin.getId(), admin.getId(), Role.USER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("próprio acesso");
        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        verify(userRepository, never()).save(any());
    }

    @Test
    void changeRole_demotingBootstrapAdmin_isRejected() {
        // Listed in APP_ADMIN_EMAILS: the next login would promote it again, so the demotion is refused up front.
        UserEntity boot = new UserEntity(UUID.randomUUID(), "b", "Boot@X.com", "Boot", "google", null, null);
        boot.setRole(Role.ADMIN);
        when(userRepository.findById(boot.getId())).thenReturn(Optional.of(boot));

        assertThatThrownBy(() -> useCase.changeRole(admin.getId(), boot.getId(), Role.USER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("APP_ADMIN_EMAILS");
        assertThat(boot.getRole()).isEqualTo(Role.ADMIN);
        assertThat(useCase.isBootstrapAdmin(boot)).isTrue();
        assertThat(useCase.isBootstrapAdmin(user)).isFalse();
    }

    @Test
    void changeRole_actorNoLongerAdminInDatabase_isForbidden() {
        // Session still holds ROLE_ADMIN, but the DB (locked admin rows) no longer lists the actor.
        when(userRepository.findByRole(Role.ADMIN)).thenReturn(List.of());

        assertThatThrownBy(() -> useCase.changeRole(admin.getId(), user.getId(), Role.ADMIN))
                .isInstanceOf(SecurityException.class);
        assertThat(user.getRole()).isEqualTo(Role.USER);
    }

    @Test
    void changeRole_unknownTarget_isEmpty() {
        assertThat(useCase.changeRole(admin.getId(), UUID.randomUUID(), Role.ADMIN)).isEmpty();
    }

    @Test
    void listUsers_nonAdminActor_isForbidden() {
        assertThatThrownBy(() -> useCase.listUsers(user.getId(), 0, 20)).isInstanceOf(SecurityException.class);
    }

    @Test
    void stats_fillsMissingRolesWithZeroAndSumsTotal() {
        when(userRepository.countGroupedByRole()).thenReturn(List.<Object[]>of(new Object[]{Role.USER, 3L}));
        when(userRepository.countGroupedByProvider()).thenReturn(List.<Object[]>of(
                new Object[]{"google", 2L}, new Object[]{"github", 1L}));
        when(simulationRepository.count()).thenReturn(10L);
        when(investmentRepository.count()).thenReturn(4L);

        UserAdministrationUseCase.AdminStats stats = useCase.stats(admin.getId());

        assertThat(stats.totalUsers()).isEqualTo(3);
        assertThat(stats.usersByRole()).containsEntry("USER", 3L).containsEntry("ADMIN", 0L);
        assertThat(stats.usersByProvider()).containsEntry("google", 2L).containsEntry("github", 1L);
        assertThat(stats.totalSimulations()).isEqualTo(10);
        assertThat(stats.totalInvestments()).isEqualTo(4);
    }
}
