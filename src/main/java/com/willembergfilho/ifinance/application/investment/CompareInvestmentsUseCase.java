package com.willembergfilho.ifinance.application.investment;

import com.willembergfilho.ifinance.domain.investment.Investment;
import com.willembergfilho.ifinance.domain.investment.InvestmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CompareInvestmentsUseCase {

    private static final int MAX_COMPARISONS = 5;

    private final InvestmentRepository investmentRepository;

    public CompareInvestmentsUseCase(InvestmentRepository investmentRepository) {
        this.investmentRepository = investmentRepository;
    }

    public List<Investment> execute(UUID userId, List<UUID> investmentIds) {
        if (investmentIds == null || investmentIds.isEmpty()) {
            throw new IllegalArgumentException("At least one investment ID must be provided.");
        }
        if (investmentIds.size() > MAX_COMPARISONS) {
            throw new IllegalArgumentException("Cannot compare more than " + MAX_COMPARISONS + " investments at once.");
        }

        List<Investment> investments = investmentRepository.findAllById(investmentIds);

        List<Investment> owned = investments.stream()
                .filter(i -> i.getUserId().equals(userId))
                .toList();

        // 404 (not 403) for any ID that doesn't exist or isn't owned by the caller —
        // a distinct status would let callers enumerate other users' investment IDs.
        if (owned.size() < investmentIds.size()) {
            Set<UUID> ownedIds = owned.stream().map(Investment::getId).collect(Collectors.toSet());
            UUID missingOrForeign = investmentIds.stream()
                    .filter(id -> !ownedIds.contains(id))
                    .findFirst()
                    .orElseThrow();
            throw new InvestmentNotFoundException(missingOrForeign);
        }

        return owned;
    }
}
