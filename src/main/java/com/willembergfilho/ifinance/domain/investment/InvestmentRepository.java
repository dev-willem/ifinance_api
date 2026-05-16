package com.willembergfilho.ifinance.domain.investment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvestmentRepository {

    Investment save(Investment investment);

    Optional<Investment> findById(UUID id);

    List<Investment> findByUserId(UUID userId, int page, int size);

    long countByUserId(UUID userId);

    List<Investment> findAllById(List<UUID> ids);
}
