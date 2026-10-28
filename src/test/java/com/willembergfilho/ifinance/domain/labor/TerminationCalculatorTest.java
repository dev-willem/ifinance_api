package com.willembergfilho.ifinance.domain.labor;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;

import static com.willembergfilho.ifinance.domain.labor.NoticeType.INDEMNIFIED;
import static com.willembergfilho.ifinance.domain.labor.NoticeType.NOT_SERVED;
import static com.willembergfilho.ifinance.domain.labor.NoticeType.WORKED;
import static com.willembergfilho.ifinance.domain.labor.TerminationType.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TerminationCalculatorTest {

    private static final BigDecimal SALARY = new BigDecimal("3100");
    private static final LocalDate ADMISSION = LocalDate.of(2023, 3, 10);
    private static final LocalDate TERMINATION = LocalDate.of(2026, 6, 25);
    private static final BigDecimal FGTS = new BigDecimal("10000");

    private static TerminationResult calc(TerminationType type, NoticeType notice, int expired) {
        return TerminationCalculator.calculate(new TerminationParameters(
                SALARY, ADMISSION, TERMINATION, type, notice, expired, FGTS, 0, 2026));
    }

    private static Map<String, BigDecimal> amounts(TerminationResult r) {
        return r.lines().stream().collect(Collectors.toMap(PayslipLine::description, PayslipLine::amount));
    }

    @Test
    void withoutCause_indemnifiedNotice() {
        // 3 complete years -> notice 30 + 9 = 39 days, projected to 03/08/2026
        // saldo 3100 x 25/30 = 2583.33; aviso 3100 x 39/30 = 4030.00
        // 13º Jan–Jul + 3 days of Aug = 7/12 -> 1808.33; férias from 10/03/2026 to 03/08 = 4 months + 25 days = 5/12
        // -> 1291.67 + 1/3 430.56; FGTS deposit (2583.33 + 4030 + 1808.33) x 8% = 673.73; fine 40% x 10673.73 = 4269.49,
        // deposited in the FGTS account (not a statement line) -> withdrawable 10673.73 + 4269.49 = 14943.22
        // INSS saldo 121.575 + 962.33 x 9% = 208.18; INSS 13º 121.575 + 187.33 x 9% = 138.43; IRRF 0 on both
        TerminationResult r = calc(WITHOUT_CAUSE, INDEMNIFIED, 0);
        Map<String, BigDecimal> a = amounts(r);

        assertThat(r.noticeDays()).isEqualTo(39);
        assertThat(r.projectedEndDate()).isEqualTo(LocalDate.of(2026, 8, 3));
        assertThat(a.get("Saldo de salário")).isEqualByComparingTo("2583.33");
        assertThat(a.get("Aviso prévio indenizado")).isEqualByComparingTo("4030.00");
        assertThat(r.thirteenthMonths()).isEqualTo(7);
        assertThat(a.get("13º salário proporcional")).isEqualByComparingTo("1808.33");
        assertThat(r.vacationMonths()).isEqualTo(5);
        assertThat(a.get("Férias proporcionais")).isEqualByComparingTo("1291.67");
        assertThat(a.get("1/3 sobre férias proporcionais")).isEqualByComparingTo("430.56");
        assertThat(r.fgts().terminationDeposit()).isEqualByComparingTo("673.73");
        assertThat(a).doesNotContainKey("Multa rescisória do FGTS");
        assertThat(r.fgts().fine()).isEqualByComparingTo("4269.49");
        assertThat(r.fgts().withdrawableAmount()).isEqualByComparingTo("14943.22");
        assertThat(r.salaryInss().total()).isEqualByComparingTo("208.18");
        assertThat(r.thirteenthInss().total()).isEqualByComparingTo("138.43");
        assertThat(r.grossTotal()).isEqualByComparingTo("10143.89");
        assertThat(r.totalDeductions()).isEqualByComparingTo("346.61");
        assertThat(r.netTotal()).isEqualByComparingTo("9797.28");
        assertThat(r.unemploymentInsuranceEligible()).isTrue();
        assertThat(r.lines()).filteredOn(l -> l.description().startsWith("Aviso"))
                .allMatch(l -> Boolean.FALSE.equals(l.taxable()));
    }

    @Test
    void withoutCause_workedNotice_tenYears_extraDaysIndemnified() {
        // 10 years -> 60-day notice: 30 worked, 30 indemnified (5000) and projected to 09/04/2026
        // saldo 5000 x 10/30 = 1666.67; 13º Jan–Mar (Apr has 9 days) = 3/12 = 1250; férias from 15/01 = 2 months + 26 days = 3/12
        // fine 40% x (50000 + 633.33) = 20253.33, withdrawable 70886.66; INSS saldo 125.69 + 13º 93.75; IRRF 0 (2 dependents)
        // gross 1666.67 + 5000 + 1250 + 1250 + 416.67 = 9583.34
        TerminationResult r = TerminationCalculator.calculate(new TerminationParameters(new BigDecimal("5000"),
                LocalDate.of(2016, 1, 15), LocalDate.of(2026, 3, 10), WITHOUT_CAUSE, WORKED, 0,
                new BigDecimal("50000"), 2, 2026));

        assertThat(r.noticeDays()).isEqualTo(60);
        assertThat(r.indemnifiedNoticeDays()).isEqualTo(30);
        assertThat(r.projectedEndDate()).isEqualTo(LocalDate.of(2026, 4, 9));
        assertThat(r.thirteenthMonths()).isEqualTo(3);
        assertThat(r.vacationMonths()).isEqualTo(3);
        assertThat(r.fgts().fine()).isEqualByComparingTo("20253.33");
        assertThat(r.fgts().withdrawableAmount()).isEqualByComparingTo("70886.66");
        assertThat(r.grossTotal()).isEqualByComparingTo("9583.34");
        assertThat(r.netTotal()).isEqualByComparingTo("9363.90"); // - 125.69 - 93.75
    }

    @Test
    void noticeCappedAt90Days() {
        TerminationResult r = TerminationCalculator.calculate(new TerminationParameters(SALARY,
                LocalDate.of(2000, 1, 1), LocalDate.of(2026, 1, 31), WITHOUT_CAUSE, INDEMNIFIED, 0, FGTS, 0, 2026));

        assertThat(r.noticeDays()).isEqualTo(90);
    }

    @Test
    void resignation_noticeNotServed_discounted() {
        // saldo 3100 x 20/30 = 2066.67; 13º Jan–Apr (20 days) 4/12 = 1033.33; férias from 03/11/2025 = 5 months + 18 days = 6/12
        // = 1550 + 516.67; earnings 5166.67; INSS 161.69 + 77.50; discount 3100 -> net 1827.48
        // FGTS estimate: 5 monthly deposits (Nov–Mar) + 2/12 13º of 2025 = 3100 x 8% x 5.1667 = 1281.33; no withdrawal
        TerminationResult r = TerminationCalculator.calculate(new TerminationParameters(SALARY,
                LocalDate.of(2025, 11, 3), LocalDate.of(2026, 4, 20), RESIGNATION, NOT_SERVED, 0, null, 0, 2026));
        Map<String, BigDecimal> a = amounts(r);

        assertThat(r.indemnifiedNoticeDays()).isZero();
        assertThat(a.get("13º salário proporcional")).isEqualByComparingTo("1033.33");
        assertThat(a.get("Férias proporcionais")).isEqualByComparingTo("1550.00");
        assertThat(a.get("Aviso prévio não cumprido")).isEqualByComparingTo("3100.00");
        assertThat(a).doesNotContainKey("Multa rescisória do FGTS");
        assertThat(r.fgts().balanceEstimated()).isTrue();
        assertThat(r.fgts().balance()).isEqualByComparingTo("1281.33");
        assertThat(r.fgts().withdrawableAmount()).isEqualByComparingTo("0");
        assertThat(r.grossTotal()).isEqualByComparingTo("5166.67");
        assertThat(r.netTotal()).isEqualByComparingTo("1827.48");
        assertThat(r.unemploymentInsuranceEligible()).isFalse();
    }

    @Test
    void resignation_noticeDiscountNeverMakesNetNegative() {
        // 5 days of salary only: discount is capped at what is due
        TerminationResult r = TerminationCalculator.calculate(new TerminationParameters(SALARY,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 5), RESIGNATION, NOT_SERVED, 0, null, 0, 2026));

        assertThat(r.netTotal()).isEqualByComparingTo("0");
    }

    @Test
    void withCause_onlyBalanceAndExpiredVacation() {
        // saldo 2583.33 + férias vencidas 3100 + 1/3 1033.33 = 6716.66; INSS 208.18 -> 6508.48
        TerminationResult r = calc(WITH_CAUSE, null, 1);
        Map<String, BigDecimal> a = amounts(r);

        assertThat(a).containsOnlyKeys("Saldo de salário", "Férias vencidas", "1/3 sobre férias vencidas",
                "INSS sobre saldo de salário");
        assertThat(r.thirteenthMonths()).isZero();
        assertThat(r.vacationMonths()).isZero();
        assertThat(r.fgts().fine()).isEqualByComparingTo("0");
        assertThat(r.fgts().withdrawableAmount()).isEqualByComparingTo("0");
        assertThat(r.grossTotal()).isEqualByComparingTo("6716.66");
        assertThat(r.netTotal()).isEqualByComparingTo("6508.48");
    }

    @Test
    void withCause_twoExpiredPeriods_olderPaidInDouble() {
        // 1 simple + 1 double = 3 x 3100 = 9300; 1/3 = 3100; + saldo 2583.33 = 14983.33; - INSS 208.18 = 14775.15
        TerminationResult r = calc(WITH_CAUSE, null, 2);
        Map<String, BigDecimal> a = amounts(r);

        assertThat(a.get("Férias vencidas")).isEqualByComparingTo("9300.00");
        assertThat(a.get("1/3 sobre férias vencidas")).isEqualByComparingTo("3100.00");
        assertThat(r.grossTotal()).isEqualByComparingTo("14983.33");
        assertThat(r.netTotal()).isEqualByComparingTo("14775.15");
        assertThat(r.notes()).anyMatch(n -> n.contains("em dobro"));
    }

    @Test
    void mutualAgreement_halfNoticeAndTwentyPercentFine() {
        // aviso 3100 x 39/60 = 2015.00 (projection still 39 days); deposit (2583.33 + 2015 + 1808.33) x 8% = 512.53
        // fine 20% x 10512.53 = 2102.51; withdrawable 80% x (10512.53 + 2102.51) = 10092.03
        // gross 2583.33 + 2015 + 1808.33 + 1291.67 + 430.56 = 8128.89
        TerminationResult r = calc(MUTUAL_AGREEMENT, INDEMNIFIED, 0);
        Map<String, BigDecimal> a = amounts(r);

        assertThat(a.get("Aviso prévio indenizado")).isEqualByComparingTo("2015.00");
        assertThat(r.thirteenthMonths()).isEqualTo(7);
        assertThat(r.fgts().fineRate()).isEqualByComparingTo("0.20");
        assertThat(r.fgts().fine()).isEqualByComparingTo("2102.51");
        assertThat(r.fgts().withdrawableAmount()).isEqualByComparingTo("10092.03");
        assertThat(r.grossTotal()).isEqualByComparingTo("8128.89");
        assertThat(r.netTotal()).isEqualByComparingTo("7782.28"); // - 346.61
        assertThat(r.unemploymentInsuranceEligible()).isFalse();
    }

    @Test
    void endOfFixedTerm_noNoticeNoFine_fullWithdrawal() {
        // 90-day trial 02/03–30/05/2026 (May 30 not last day -> 30 days): saldo 2551.40; 13º 3/12 = 637.85;
        // férias 2 months + 29 days = 3/12 = 637.85 + 212.62; INSS 205.31 + 47.84
        // FGTS estimate Mar + Apr = 2551.40 x 8% x 2 = 408.22; deposit 255.14; withdrawable 663.36
        TerminationResult r = TerminationCalculator.calculate(new TerminationParameters(new BigDecimal("2551.40"),
                LocalDate.of(2026, 3, 2), LocalDate.of(2026, 5, 30), END_OF_FIXED_TERM, null, 0, null, 0, 2026));

        assertThat(r.noticeDays()).isZero();
        assertThat(r.salaryBalanceDays()).isEqualTo(30);
        assertThat(r.fgts().balance()).isEqualByComparingTo("408.22");
        assertThat(r.fgts().fine()).isEqualByComparingTo("0");
        assertThat(r.fgts().withdrawableAmount()).isEqualByComparingTo("663.36");
        assertThat(r.grossTotal()).isEqualByComparingTo("4039.72");
        assertThat(r.netTotal()).isEqualByComparingTo("3786.57");
    }

    @Test
    void projectionIntoNextYear_splitsThirteenthByYear() {
        // 10 years -> 60 days from 20/12/2026 to 18/02/2027: 13º 2026 = 12/12 (3000); 2027 = Jan + 18 days of Feb = 2/12 (500)
        // INSS on the 3500 paid together: 121.575 + 1281.84 x 9% + 597.16 x 12% = 308.60
        // férias from 15/01/2026 = 13 months + 4 days -> 12/12 + 1/12 = 3250
        TerminationResult r = TerminationCalculator.calculate(new TerminationParameters(new BigDecimal("3000"),
                LocalDate.of(2016, 1, 15), LocalDate.of(2026, 12, 20), WITHOUT_CAUSE, INDEMNIFIED, 0, FGTS, 0, 2026));
        Map<String, PayslipLine> l = r.lines().stream().collect(Collectors.toMap(PayslipLine::description, x -> x));

        assertThat(r.projectedEndDate()).isEqualTo(LocalDate.of(2027, 2, 18));
        assertThat(l.get("13º salário proporcional").reference()).isEqualTo("12/12 avos");
        assertThat(l.get("13º salário proporcional").amount()).isEqualByComparingTo("3000.00");
        assertThat(l.get("13º salário proporcional (2027)").reference()).isEqualTo("2/12 avos");
        assertThat(l.get("13º salário proporcional (2027)").amount()).isEqualByComparingTo("500.00");
        assertThat(r.thirteenthInss().total()).isEqualByComparingTo("308.60");
        assertThat(l.get("Férias proporcionais").reference()).isEqualTo("12/12 + 1/12 avos");
        assertThat(l.get("Férias proporcionais").amount()).isEqualByComparingTo("3250.00");
        assertThat(r.notes()).anyMatch(n -> n.contains("2027"));
    }

    @Test
    void workedNotice_yearsCountedAtNoticeDate() {
        // admission 10/03/2023, worked notice ends 21/03/2026 -> given 19/02/2026 with 2 complete years: 36 days.
        // Indemnified on the same date: 3 complete years -> 39 days.
        LocalDate end = LocalDate.of(2026, 3, 21);
        TerminationResult worked = TerminationCalculator.calculate(new TerminationParameters(SALARY, ADMISSION, end,
                WITHOUT_CAUSE, WORKED, 0, FGTS, 0, 2026));
        TerminationResult indemnified = TerminationCalculator.calculate(new TerminationParameters(SALARY, ADMISSION, end,
                WITHOUT_CAUSE, INDEMNIFIED, 0, FGTS, 0, 2026));

        assertThat(worked.noticeDays()).isEqualTo(36);
        assertThat(worked.indemnifiedNoticeDays()).isEqualTo(6);
        assertThat(indemnified.noticeDays()).isEqualTo(39);
    }

    @Test
    void yearDefaultsToTerminationYear() {
        TerminationResult r = TerminationCalculator.calculate(new TerminationParameters(SALARY, ADMISSION,
                LocalDate.of(2025, 6, 25), WITHOUT_CAUSE, INDEMNIFIED, 0, FGTS, 0, null));
        assertThat(r.year()).isEqualTo(2025);

        assertThatThrownBy(() -> TerminationCalculator.calculate(new TerminationParameters(SALARY, ADMISSION,
                LocalDate.of(2024, 6, 25), WITHOUT_CAUSE, INDEMNIFIED, 0, FGTS, 0, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2024");
    }

    // ------------------------------------------------------------- day rules --

    @Test
    void salaryBalanceDays_commercialMonth() {
        assertThat(TerminationCalculator.salaryBalanceDays(ADMISSION, LocalDate.of(2026, 2, 28))).isEqualTo(30);
        assertThat(TerminationCalculator.salaryBalanceDays(ADMISSION, LocalDate.of(2026, 1, 31))).isEqualTo(30);
        assertThat(TerminationCalculator.salaryBalanceDays(LocalDate.of(2026, 2, 10), LocalDate.of(2026, 2, 28))).isEqualTo(19);
        // hired on the 1st and left on the last day of February: full commercial month
        assertThat(TerminationCalculator.salaryBalanceDays(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28))).isEqualTo(30);
    }

    @Test
    void thirteenthMonths_fifteenDaysCountAsMonth() {
        LocalDate end = LocalDate.of(2026, 5, 10);
        // Mar 17–31 = 15 days counts; Apr full; May 1–10 doesn't
        assertThat(TerminationCalculator.thirteenthMonths(LocalDate.of(2026, 3, 17), end)).isEqualTo(2);
        assertThat(TerminationCalculator.thirteenthMonths(LocalDate.of(2026, 3, 18), end)).isEqualTo(1);
    }

    @Test
    void vacationMonths_fractionAbove14Days() {
        LocalDate start = LocalDate.of(2026, 1, 10);
        assertThat(TerminationCalculator.vacationMonths(start, LocalDate.of(2026, 3, 23))).isEqualTo(2); // 2m + 14d
        assertThat(TerminationCalculator.vacationMonths(start, LocalDate.of(2026, 3, 24))).isEqualTo(3); // 2m + 15d
    }

    // ------------------------------------------------------------ validation --

    @Test
    void invalidCombinations_rejected() {
        assertThatThrownBy(() -> calc(WITHOUT_CAUSE, NOT_SERVED, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calc(RESIGNATION, INDEMNIFIED, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calc(WITHOUT_CAUSE, INDEMNIFIED, 4)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TerminationParameters(SALARY, LocalDate.of(2025, 1, 1), LocalDate.of(2024, 1, 1),
                WITHOUT_CAUSE, INDEMNIFIED, 0, null, 0, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TerminationParameters(SALARY, LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 1),
                WITHOUT_CAUSE, INDEMNIFIED, 2, null, 0, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Férias vencidas");
        assertThatThrownBy(() -> calc(END_OF_FIXED_TERM, null, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2 anos");
    }
}
