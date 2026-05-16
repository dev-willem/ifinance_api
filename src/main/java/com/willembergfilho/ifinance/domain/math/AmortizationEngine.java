package com.willembergfilho.ifinance.domain.math;

import com.willembergfilho.ifinance.domain.simulation.Installment;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static com.willembergfilho.ifinance.domain.math.MonetaryRounding.*;

public class AmortizationEngine {

    private static final BigDecimal ONE = BigDecimal.ONE;

    /**
     * SAC — Sistema de Amortização Constante.
     * Fixed amortization, decreasing installments, linearly shrinking balance.
     */
    public List<Installment> calculateSac(BigDecimal principal, BigDecimal periodicRate, int terms) {
        validate(principal, periodicRate, terms);

        List<Installment> schedule = new ArrayList<>(terms);
        BigDecimal amortization = round(principal.divide(BigDecimal.valueOf(terms), MC));
        BigDecimal balance = principal;

        for (int period = 1; period <= terms; period++) {
            BigDecimal interest = round(balance.multiply(periodicRate, MC));
            // Last period: amortization absorbs any rounding remainder
            BigDecimal periodAmortization = (period == terms)
                    ? balance
                    : amortization;
            BigDecimal total = round(periodAmortization.add(interest, MC));
            BigDecimal balanceAfter = balance.subtract(periodAmortization, MC);

            schedule.add(new Installment(period, balance, periodAmortization, interest,
                    BigDecimal.ZERO, total, round(balanceAfter)));
            balance = round(balanceAfter);
        }
        return schedule;
    }

    /**
     * PRICE — Sistema Francês (parcelas fixas).
     * Constant installment = PV * [i * (1+i)^n] / [(1+i)^n - 1]
     */
    public List<Installment> calculatePrice(BigDecimal principal, BigDecimal periodicRate, int terms) {
        validate(principal, periodicRate, terms);

        List<Installment> schedule = new ArrayList<>(terms);

        BigDecimal onePlusI = ONE.add(periodicRate, MC);
        BigDecimal onePlusIPowN = onePlusI.pow(terms, MC);
        BigDecimal installmentAmount = round(
                principal.multiply(periodicRate.multiply(onePlusIPowN, MC), MC)
                         .divide(onePlusIPowN.subtract(ONE, MC), MC)
        );

        BigDecimal balance = principal;

        for (int period = 1; period <= terms; period++) {
            BigDecimal interest = round(balance.multiply(periodicRate, MC));
            BigDecimal amortization = round(installmentAmount.subtract(interest, MC));

            // Last period: absorb rounding residual so balance reaches exactly zero
            if (period == terms) {
                amortization = balance;
                installmentAmount = round(amortization.add(interest, MC));
            }

            BigDecimal balanceAfter = balance.subtract(amortization, MC);

            schedule.add(new Installment(period, balance, amortization, interest,
                    BigDecimal.ZERO, installmentAmount, round(balanceAfter)));
            balance = round(balanceAfter);
        }
        return schedule;
    }

    /**
     * AMERICAN — Sistema Americano (Balão).
     * Periods 1..n-1: interest only. Period n: full principal + interest.
     */
    public List<Installment> calculateAmerican(BigDecimal principal, BigDecimal periodicRate, int terms) {
        validate(principal, periodicRate, terms);

        List<Installment> schedule = new ArrayList<>(terms);
        BigDecimal balance = principal;

        for (int period = 1; period <= terms; period++) {
            BigDecimal interest = round(balance.multiply(periodicRate, MC));
            BigDecimal amortization;
            BigDecimal total;
            BigDecimal balanceAfter;

            if (period == terms) {
                amortization = balance;
                total = round(amortization.add(interest, MC));
                balanceAfter = BigDecimal.ZERO;
            } else {
                amortization = BigDecimal.ZERO;
                total = interest;
                balanceAfter = balance;
            }

            schedule.add(new Installment(period, balance, amortization, interest,
                    BigDecimal.ZERO, total, round(balanceAfter)));
            balance = round(balanceAfter);
        }
        return schedule;
    }

    /**
     * GERMAN — Sistema Alemão (juros simples sobre o principal original).
     * Constant amortization (PV/n) + interest always on original principal.
     * Total payment each period = PV/n + PV*i (constant). Last period absorbs rounding.
     */
    public List<Installment> calculateGerman(BigDecimal principal, BigDecimal periodicRate, int terms) {
        validate(principal, periodicRate, terms);

        List<Installment> schedule = new ArrayList<>(terms);
        BigDecimal amortization = round(principal.divide(BigDecimal.valueOf(terms), MC));
        BigDecimal constantInterest = round(principal.multiply(periodicRate, MC));
        BigDecimal balance = principal;

        for (int period = 1; period <= terms; period++) {
            BigDecimal periodAmortization = (period == terms) ? balance : amortization;
            BigDecimal total = round(periodAmortization.add(constantInterest, MC));
            BigDecimal balanceAfter = balance.subtract(periodAmortization, MC);

            schedule.add(new Installment(period, balance, periodAmortization, constantInterest,
                    BigDecimal.ZERO, total, round(balanceAfter)));
            balance = round(balanceAfter);
        }
        return schedule;
    }

    /**
     * SAM — Sistema de Amortização Misto.
     * Each installment = (SAC_installment_k + PRICE_constant_installment) / 2.
     * Amortization = SAM_installment - interest on current balance.
     * Last period corrects balance to zero.
     */
    public List<Installment> calculateSam(BigDecimal principal, BigDecimal periodicRate, int terms) {
        validate(principal, periodicRate, terms);

        List<Installment> sacSchedule = calculateSac(principal, periodicRate, terms);

        BigDecimal onePlusI = ONE.add(periodicRate, MC);
        BigDecimal onePlusIPowN = onePlusI.pow(terms, MC);
        BigDecimal priceInstallment = round(
                principal.multiply(periodicRate.multiply(onePlusIPowN, MC), MC)
                         .divide(onePlusIPowN.subtract(ONE, MC), MC)
        );

        List<Installment> schedule = new ArrayList<>(terms);
        BigDecimal balance = principal;

        for (int period = 1; period <= terms; period++) {
            BigDecimal sacTotal = sacSchedule.get(period - 1).total();
            BigDecimal samInstallment = round(sacTotal.add(priceInstallment, MC)
                    .divide(new BigDecimal("2"), MC));

            BigDecimal interest = round(balance.multiply(periodicRate, MC));
            BigDecimal amortization;
            BigDecimal balanceAfter;

            if (period == terms) {
                amortization = balance;
                samInstallment = round(amortization.add(interest, MC));
                balanceAfter = BigDecimal.ZERO;
            } else {
                amortization = round(samInstallment.subtract(interest, MC));
                balanceAfter = balance.subtract(amortization, MC);
            }

            schedule.add(new Installment(period, balance, amortization, interest,
                    BigDecimal.ZERO, samInstallment, round(balanceAfter)));
            balance = round(balanceAfter);
        }
        return schedule;
    }

    private void validate(BigDecimal principal, BigDecimal periodicRate, int terms) {
        if (principal == null || principal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CalculationException("Principal must be a positive value.");
        }
        if (periodicRate == null || periodicRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new CalculationException("Periodic rate must be non-negative.");
        }
        if (terms <= 0) {
            throw new CalculationException("Number of terms must be positive.");
        }
    }
}
