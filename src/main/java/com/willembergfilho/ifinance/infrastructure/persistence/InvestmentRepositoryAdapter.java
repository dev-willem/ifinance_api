package com.willembergfilho.ifinance.infrastructure.persistence;

import com.willembergfilho.ifinance.domain.investment.Investment;
import com.willembergfilho.ifinance.domain.investment.InvestmentRepository;
import com.willembergfilho.ifinance.infrastructure.persistence.entity.InvestmentEntity;
import com.willembergfilho.ifinance.infrastructure.persistence.mapper.InvestmentMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class InvestmentRepositoryAdapter implements InvestmentRepository {

    private final InvestmentJpaRepository jpaRepository;
    private final InvestmentMapper mapper;

    public InvestmentRepositoryAdapter(InvestmentJpaRepository jpaRepository, InvestmentMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public Investment save(Investment investment) {
        if (jpaRepository.existsById(investment.getId())) {
            jpaRepository.updateStatus(investment.getId(), investment.getStatus().name());
        } else {
            InvestmentEntity entity = mapper.toEntity(investment);
            jpaRepository.save(entity);
        }
        return investment;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Investment> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Investment> findByUserId(UUID userId, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return jpaRepository.findByUserId(userId, pageable)
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countByUserId(UUID userId) {
        return jpaRepository.countByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Investment> findAllById(List<UUID> ids) {
        return jpaRepository.findAllById(ids).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
