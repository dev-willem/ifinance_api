package com.willembergfilho.ifinance.application.admin;

import com.willembergfilho.ifinance.infrastructure.persistence.InvestmentJpaRepository;
import com.willembergfilho.ifinance.infrastructure.persistence.SimulationJpaRepository;
import com.willembergfilho.ifinance.infrastructure.persistence.UserJpaRepository;
import com.willembergfilho.ifinance.infrastructure.persistence.entity.UserEntity;
import com.willembergfilho.ifinance.infrastructure.persistence.entity.UserEntity.Role;
import com.willembergfilho.ifinance.infrastructure.security.UserSyncService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Every operation re-checks the actor's role in the database: the ROLE_ADMIN authority lives in the
 * session until the next login, so this is what makes a demotion take effect immediately.
 */
@Service
public class UserAdministrationUseCase {

    static final String NOT_ADMIN = "Acesso restrito a administradores.";

    private final UserJpaRepository userRepository;
    private final SimulationJpaRepository simulationRepository;
    private final InvestmentJpaRepository investmentRepository;
    private final UserSyncService userSyncService;

    public UserAdministrationUseCase(UserJpaRepository userRepository,
                                     SimulationJpaRepository simulationRepository,
                                     InvestmentJpaRepository investmentRepository,
                                     UserSyncService userSyncService) {
        this.userRepository = userRepository;
        this.simulationRepository = simulationRepository;
        this.investmentRepository = investmentRepository;
        this.userSyncService = userSyncService;
    }

    public record AdminStats(long totalUsers, Map<String, Long> usersByRole, Map<String, Long> usersByProvider,
                             long totalSimulations, long totalInvestments) {}

    @Transactional(readOnly = true)
    public Page<UserEntity> listUsers(UUID actorId, int page, int size) {
        requireAdmin(actorId);
        return userRepository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    /** Empty when the target user does not exist. */
    @Transactional
    public Optional<UserEntity> changeRole(UUID actorId, UUID targetId, Role role) {
        // Locks the admin rows: two admins demoting each other concurrently are serialized.
        List<UserEntity> admins = userRepository.findByRole(Role.ADMIN);
        if (admins.stream().noneMatch(admin -> admin.getId().equals(actorId))) {
            throw new SecurityException(NOT_ADMIN);
        }
        // The actor is a (locked) admin and cannot demote itself, so the last admin can never be removed.
        if (actorId.equals(targetId) && role != Role.ADMIN) {
            throw new IllegalArgumentException("Você não pode remover seu próprio acesso de administrador.");
        }
        return userRepository.findById(targetId).map(target -> {
            if (role != Role.ADMIN && isBootstrapAdmin(target)) {
                throw new IllegalArgumentException("Este e-mail está em APP_ADMIN_EMAILS e voltaria a ser "
                        + "administrador no próximo login. Remova-o dessa configuração antes de rebaixá-lo.");
            }
            target.setRole(role);
            return userRepository.save(target);
        });
    }

    @Transactional(readOnly = true)
    public AdminStats stats(UUID actorId) {
        requireAdmin(actorId);
        Map<String, Long> byRole = new LinkedHashMap<>();
        for (Role role : Role.values()) byRole.put(role.name(), 0L);
        userRepository.countGroupedByRole().forEach(row -> byRole.put(String.valueOf(row[0]), ((Number) row[1]).longValue()));

        Map<String, Long> byProvider = new TreeMap<>();
        userRepository.countGroupedByProvider().forEach(row -> byProvider.put(String.valueOf(row[0]), ((Number) row[1]).longValue()));

        long totalUsers = byRole.values().stream().mapToLong(Long::longValue).sum();
        return new AdminStats(totalUsers, byRole, byProvider, simulationRepository.count(), investmentRepository.count());
    }

    public boolean isBootstrapAdmin(UserEntity user) {
        return userSyncService.isBootstrapAdmin(user.getEmail());
    }

    private void requireAdmin(UUID actorId) {
        userRepository.findById(actorId)
                .filter(user -> user.getRole() == Role.ADMIN)
                .orElseThrow(() -> new SecurityException(NOT_ADMIN));
    }
}
