package com.willembergfilho.ifinance.application.investment;

import com.willembergfilho.ifinance.domain.investment.Investment;
import com.willembergfilho.ifinance.domain.investment.InvestmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class GetInvestmentHistoryUseCase {

    private final InvestmentRepository investmentRepository;

    public GetInvestmentHistoryUseCase(InvestmentRepository investmentRepository) {
        this.investmentRepository = investmentRepository;
    }

    public record HistoryPage(List<Investment> investments, long total) {}

    public HistoryPage execute(UUID userId, int page, int size) {
        List<Investment> investments = investmentRepository.findByUserId(userId, page, size);
        long total = investmentRepository.countByUserId(userId);
        return new HistoryPage(investments, total);
    }
}
