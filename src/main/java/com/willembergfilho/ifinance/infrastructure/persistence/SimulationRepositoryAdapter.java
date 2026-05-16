package com.willembergfilho.ifinance.infrastructure.persistence;

import com.willembergfilho.ifinance.domain.simulation.Simulation;
import com.willembergfilho.ifinance.domain.simulation.SimulationRepository;
import com.willembergfilho.ifinance.infrastructure.persistence.entity.SimulationEntity;
import com.willembergfilho.ifinance.infrastructure.persistence.mapper.SimulationMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class SimulationRepositoryAdapter implements SimulationRepository {

    private final SimulationJpaRepository jpaRepository;
    private final SimulationMapper mapper;

    public SimulationRepositoryAdapter(SimulationJpaRepository jpaRepository, SimulationMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public Simulation save(Simulation simulation) {
        if (jpaRepository.existsById(simulation.getId())) {
            jpaRepository.updateStatus(simulation.getId(), simulation.getStatus().name());
        } else {
            SimulationEntity entity = mapper.toEntity(simulation);
            jpaRepository.save(entity);
        }
        return simulation;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Simulation> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Simulation> findByUserId(UUID userId, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return jpaRepository.findByUserId(userId, pageable)
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public long countByUserId(UUID userId) {
        return jpaRepository.countByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Simulation> findAllById(List<UUID> ids) {
        return jpaRepository.findAllById(ids).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
