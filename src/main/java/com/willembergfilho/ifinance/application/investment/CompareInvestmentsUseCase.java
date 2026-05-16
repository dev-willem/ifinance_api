package com.willembergfilho.ifinance.application.investment;

import com.willembergfilho.ifinance.domain.investment.Investment;
import com.willembergfilho.ifinance.domain.investment.InvestmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

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

        boolean unauthorized = investments.stream()
                .anyMatch(i -> !i.getUserId().equals(userId));
        if (unauthorized) {
            throw new SecurityException("One or more investments do not belong to the authenticated user.");
        }

        return investments;
    }
}
