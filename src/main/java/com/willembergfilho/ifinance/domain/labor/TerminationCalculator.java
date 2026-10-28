package com.willembergfilho.ifinance.domain.labor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static com.willembergfilho.ifinance.domain.labor.PayrollTaxes.FGTS_RATE;
import static com.willembergfilho.ifinance.domain.labor.PayrollTaxes.THIRTY;
import static com.willembergfilho.ifinance.domain.labor.PayrollTaxes.THREE;
import static com.willembergfilho.ifinance.domain.labor.PayrollTaxes.TWELVE;
import static com.willembergfilho.ifinance.domain.labor.PayrollTaxes.fraction;
import static com.willembergfilho.ifinance.domain.labor.TerminationType.MUTUAL_AGREEMENT;
import static com.willembergfilho.ifinance.domain.labor.TerminationType.RESIGNATION;
import static com.willembergfilho.ifinance.domain.labor.TerminationType.WITHOUT_CAUSE;
import static com.willembergfilho.ifinance.domain.labor.TerminationType.WITH_CAUSE;
import static com.willembergfilho.ifinance.domain.math.MonetaryRounding.MC;
import static com.willembergfilho.ifinance.domain.math.MonetaryRounding.round;
import static java.math.BigDecimal.ZERO;

/**
 * Termination (rescisão) items for CLT contracts. Indemnities are exempt: vacation paid on termination and the
 * FGTS fine from INSS (Lei 8.212/1991 art. 28 §9º "d" and "e") and IRRF (Lei 7.713/1988 art. 6º V; STJ Súmulas 125
 * and 386); indemnified notice from INSS (STJ Tema 478) and IRRF (Lei 7.713/1988 art. 6º V).
 * Salary balance and 13º are taxed separately from each other. The FGTS fine goes to the FGTS account, not to the
 * statement lines. Without an explicit year, the table of the termination year is used.
 */
public final class TerminationCalculator {

    private static final BigDecimal SIXTY = BigDecimal.valueOf(60);
    private static final DateTimeFormatter BR_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private TerminationCalculator() {}

    public static TerminationResult calculate(TerminationParameters p) {
        TerminationType type = p.terminationType();
        BigDecimal salary = p.monthlySalary();
        LocalDate admission = p.admissionDate();
        LocalDate end = p.terminationDate();
        LaborTaxTable table = LaborTaxTables.forYear(p.year() != null ? p.year() : end.getYear());
        int years = Period.between(admission, end).getYears();
        // Proportional notice counts complete years up to the day the notice is given; a worked notice ends 30 days later.
        boolean worked = p.noticeType() == NoticeType.WORKED;
        int noticeYears = Period.between(admission, worked ? end.minusDays(30) : end).getYears();

        // Lei 12.506/2011: 30 days + 3 per complete year, max 90 — owed by the employer only; resigning employees give 30.
        boolean employerNotice = type == WITHOUT_CAUSE || type == MUTUAL_AGREEMENT;
        int noticeDays = employerNotice ? Math.min(90, 30 + 3 * noticeYears) : type == RESIGNATION ? 30 : 0;
        // A worked notice covers 30 days; the proportional extra days are indemnified.
        int indemnifiedDays = !employerNotice ? 0
                : p.noticeType() == NoticeType.INDEMNIFIED ? noticeDays : noticeDays - 30;
        LocalDate projectedEnd = end.plusDays(indemnifiedDays);

        int balanceDays = salaryBalanceDays(admission, end);
        BigDecimal salaryBalance = fraction(salary, balanceDays, THIRTY);
        // CLT art. 484-A, I, "a": half the indemnified notice in a mutual agreement; the projection stays whole.
        BigDecimal notice = fraction(salary, indemnifiedDays, type == MUTUAL_AGREEMENT ? SIXTY : THIRTY);

        // Dismissal with cause forfeits proportional 13º and vacation (Decreto 57.155/1965 art. 7º; TST Súmula 171).
        boolean proportional = type != WITH_CAUSE;
        LocalDate yearEnd = LocalDate.of(end.getYear(), 12, 31);
        int currentYearMonths = proportional
                ? thirteenthMonths(max(admission, yearEnd.withDayOfYear(1)), min(projectedEnd, yearEnd)) : 0;
        // The projection (max 90 days) may reach next year: those avos belong to next year's 13º.
        int nextYearMonths = proportional && projectedEnd.isAfter(yearEnd) ? thirteenthMonths(yearEnd.plusDays(1), projectedEnd) : 0;
        int thirteenthMonths = currentYearMonths + nextYearMonths;
        BigDecimal currentThirteenth = fraction(salary, currentYearMonths, TWELVE);
        BigDecimal nextThirteenth = fraction(salary, nextYearMonths, TWELVE);
        BigDecimal thirteenth = currentThirteenth.add(nextThirteenth);
        // CLT arts. 137 and 146: with two expired periods the older one is past its concessive period, so it is paid in double.
        int expired = p.expiredVacationPeriods();
        BigDecimal expiredVacation = round(salary.multiply(BigDecimal.valueOf(expired == 2 ? 3 : expired)));
        BigDecimal expiredBonus = round(expiredVacation.divide(THREE, MC));
        int vacationMonths = proportional ? vacationMonths(admission.plusYears(years), projectedEnd) : 0;
        BigDecimal vacation = fraction(salary, vacationMonths, TWELVE);
        BigDecimal vacationBonus = round(vacation.divide(THREE, MC));

        // FGTS is also due on the indemnified notice (TST Súmula 305) and on the 13º; the fine covers all deposits.
        BigDecimal terminationDeposit = round(salaryBalance.add(notice).add(thirteenth).multiply(FGTS_RATE));
        boolean estimated = p.fgtsBalance() == null;
        BigDecimal balance = estimated ? estimateFgtsBalance(salary, admission, end) : round(p.fgtsBalance());
        BigDecimal fgtsBase = balance.add(terminationDeposit);
        BigDecimal fine = round(fgtsBase.multiply(type.fgtsFineRate()));
        // The fine is deposited in the FGTS account (Lei 8.036/1990 art. 18 §1º), so it is withdrawn with the balance.
        TerminationResult.FgtsSummary fgts = new TerminationResult.FgtsSummary(balance, estimated, terminationDeposit,
                type.fgtsFineRate(), fine, type.fgtsWithdrawalRate(),
                round(fgtsBase.add(fine).multiply(type.fgtsWithdrawalRate())));

        InssResult salaryInss = PayrollTaxes.inss(salaryBalance, table);
        IrrfResult salaryIrrf = PayrollTaxes.irrf(salaryBalance, salaryInss.total(), p.dependents(), ZERO, table);
        InssResult thirteenthInss = PayrollTaxes.inss(thirteenth, table);
        IrrfResult thirteenthIrrf = PayrollTaxes.irrf(thirteenth, thirteenthInss.total(), p.dependents(), ZERO, table);

        List<PayslipLine> lines = new ArrayList<>();
        addEarning(lines, "Saldo de salário", balanceDays + " dias", salaryBalance, true);
        addEarning(lines, "Aviso prévio indenizado",
                indemnifiedDays + " dias" + (type == MUTUAL_AGREEMENT ? " (50%)" : ""), notice, false);
        addEarning(lines, "13º salário proporcional", currentYearMonths + "/12 avos", currentThirteenth, true);
        addEarning(lines, "13º salário proporcional (" + (end.getYear() + 1) + ")", nextYearMonths + "/12 avos",
                nextThirteenth, true);
        addEarning(lines, "Férias vencidas", expired == 2 ? "1 simples + 1 em dobro" : "1 período", expiredVacation, false);
        addEarning(lines, "1/3 sobre férias vencidas", null, expiredBonus, false);
        // Over 12: the projection completes the acquisition period and starts a new one.
        addEarning(lines, "Férias proporcionais", (vacationMonths > 12 ? "12/12 + " + (vacationMonths - 12)
                : vacationMonths) + "/12 avos", vacation, false);
        addEarning(lines, "1/3 sobre férias proporcionais", null, vacationBonus, false);

        BigDecimal earnings = lines.stream().map(PayslipLine::amount).reduce(ZERO, BigDecimal::add);
        BigDecimal taxes = salaryInss.total().add(salaryIrrf.tax()).add(thirteenthInss.total()).add(thirteenthIrrf.tax());
        addDeduction(lines, "INSS sobre saldo de salário", salaryInss.total());
        addDeduction(lines, "IRRF sobre saldo de salário", salaryIrrf.tax());
        addDeduction(lines, "INSS sobre 13º salário", thirteenthInss.total());
        addDeduction(lines, "IRRF sobre 13º salário", thirteenthIrrf.tax());

        BigDecimal noticeDiscount = ZERO;
        if (type == RESIGNATION && p.noticeType() == NoticeType.NOT_SERVED) {
            // CLT art. 487 §2º; capped at what is due so the payment never goes negative (art. 477 §5º).
            noticeDiscount = round(salary.min(earnings.subtract(taxes)).max(ZERO));
            if (noticeDiscount.signum() > 0) {
                lines.add(PayslipLine.deduction("Aviso prévio não cumprido", "30 dias", noticeDiscount));
            }
        }
        BigDecimal deductions = taxes.add(noticeDiscount);

        return new TerminationResult(table.year(), type, p.noticeType(), years, noticeDays, indemnifiedDays,
                projectedEnd, balanceDays, thirteenthMonths, vacationMonths, lines, salaryInss, salaryIrrf,
                thirteenthInss, thirteenthIrrf, fgts, type == WITHOUT_CAUSE,
                notes(type, worked, noticeDays, indemnifiedDays, projectedEnd, estimated, thirteenthMonths, nextYearMonths,
                        expired, table.year()),
                earnings, deductions, earnings.subtract(deductions));
    }

    /** Commercial month: a full calendar month counts as 30 days. */
    static int salaryBalanceDays(LocalDate admission, LocalDate end) {
        boolean admittedThisMonth = YearMonth.from(admission).equals(YearMonth.from(end));
        int from = admittedThisMonth ? admission.getDayOfMonth() : 1;
        if (from == 1 && end.getDayOfMonth() == end.lengthOfMonth()) return 30;
        return Math.min(30, end.getDayOfMonth() - from + 1);
    }

    /** Calendar months from start to end (inclusive) with 15+ days worked (Lei 4.090/1962 art. 1º §2º). */
    static int thirteenthMonths(LocalDate start, LocalDate end) {
        int months = 0;
        for (YearMonth m = YearMonth.from(start); !m.isAfter(YearMonth.from(end)); m = m.plusMonths(1)) {
            LocalDate from = max(start, m.atDay(1));
            LocalDate to = min(end, m.atEndOfMonth());
            if (ChronoUnit.DAYS.between(from, to) + 1 >= 15) months++;
        }
        return months;
    }

    /**
     * Months of the current acquisition period, a fraction of 15+ days counting as a month (CLT art. 146
     * parágrafo único). May exceed 12 when the notice projection completes another period (12/12 + new avos).
     */
    static int vacationMonths(LocalDate periodStart, LocalDate projectedEnd) {
        LocalDate endExclusive = projectedEnd.plusDays(1);
        long months = ChronoUnit.MONTHS.between(periodStart, endExclusive);
        long rest = ChronoUnit.DAYS.between(periodStart.plusMonths(months), endExclusive);
        return (int) months + (rest >= 15 ? 1 : 0);
    }

    // shortcut: current salary for every month (admission month counted whole), no TR + 3% p.a. interest;
    // the statement balance is more accurate.
    static BigDecimal estimateFgtsBalance(BigDecimal salary, LocalDate admission, LocalDate end) {
        YearMonth first = YearMonth.from(admission);
        long monthlyDeposits = ChronoUnit.MONTHS.between(first, YearMonth.from(end));
        long monthsInPastYears = Math.max(0, ChronoUnit.MONTHS.between(first, YearMonth.of(end.getYear(), 1)));
        // one deposit per month before the termination month + the 13º deposits of past years
        BigDecimal months = BigDecimal.valueOf(monthlyDeposits)
                .add(BigDecimal.valueOf(monthsInPastYears).divide(TWELVE, MC));
        return round(salary.multiply(FGTS_RATE).multiply(months, MC));
    }

    private static List<String> notes(TerminationType type, boolean worked, int noticeDays, int indemnifiedDays,
                                      LocalDate projectedEnd, boolean estimated, int thirteenthMonths,
                                      int nextYearMonths, int expired, int tableYear) {
        List<String> notes = new ArrayList<>();
        if (type == WITHOUT_CAUSE || type == MUTUAL_AGREEMENT) {
            notes.add("Aviso prévio de " + noticeDays + " dias: 30 + 3 por ano completo, até 90 (Lei 12.506/2011)."
                    + (worked ? " Com aviso trabalhado, os anos contam até a data do aviso (30 dias antes do desligamento)." : ""));
        }
        if (indemnifiedDays > 0) {
            notes.add("A projeção do aviso indenizado até " + projectedEnd.format(BR_DATE)
                    + " conta para o 13º e as férias proporcionais (CLT, art. 487, §1º)."
                    + (type == MUTUAL_AGREEMENT ? " No acordo, o aviso é pago pela metade, mas a projeção é integral "
                    + "(leitura majoritária; o art. 484-A não trata da projeção)." : ""));
        }
        if (expired == 2) {
            notes.add("O período de férias vencidas mais antigo já passou do prazo de concessão e é pago em dobro "
                    + "(CLT, arts. 137 e 146).");
        }
        if (nextYearMonths > 0) {
            notes.add("A projeção do aviso alcança " + projectedEnd.getYear() + ": " + nextYearMonths + "/12 avos do 13º "
                    + "desse ano são pagos na rescisão e tributados junto com o 13º, pela tabela de " + tableYear + ".");
        }
        if (thirteenthMonths > 0) {
            notes.add("Considera que nenhuma parcela do 13º deste ano foi adiantada.");
        }
        if (estimated) {
            notes.add("Saldo do FGTS estimado com 8% do salário atual por mês (e sobre o 13º), sem correção (TR + 3% a.a.). "
                    + "Informe o saldo do extrato para maior precisão.");
        }
        notes.add(switch (type) {
            case WITHOUT_CAUSE -> "A multa de 40% é depositada na conta do FGTS, não paga no termo (Lei 8.036/1990, art. 18, §1º); "
                    + "pode sacar 100% do FGTS, multa incluída. Seguro-desemprego: tem direito se cumprir a carência — 12 meses "
                    + "trabalhados nos últimos 18 (1ª solicitação), 9 nos últimos 12 (2ª) ou 6 meses (demais) — Lei 7.998/1990, art. 3º.";
            case MUTUAL_AGREEMENT -> "A multa de 20% é depositada na conta do FGTS, não paga no termo. Saque de 80% do saldo da conta, "
                    + "multa incluída (leitura adotada para o limite do art. 484-A, §1º); o acordo não dá direito ao seguro-desemprego.";
            case END_OF_FIXED_TERM -> "Pode sacar 100% do FGTS (Lei 8.036/1990, art. 20, IX); sem seguro-desemprego e sem multa de 40%.";
            case RESIGNATION -> "No pedido de demissão não há saque do FGTS, multa de 40% nem seguro-desemprego.";
            case WITH_CAUSE -> "Na justa causa não há saque do FGTS, multa, 13º e férias proporcionais nem seguro-desemprego.";
        });
        return notes;
    }

    private static void addEarning(List<PayslipLine> lines, String description, String reference,
                                   BigDecimal amount, boolean taxable) {
        if (amount.signum() > 0) lines.add(PayslipLine.earning(description, reference, amount, taxable));
    }

    private static void addDeduction(List<PayslipLine> lines, String description, BigDecimal amount) {
        if (amount.signum() > 0) lines.add(PayslipLine.deduction(description, null, amount));
    }

    private static LocalDate max(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }

    private static LocalDate min(LocalDate a, LocalDate b) {
        return a.isBefore(b) ? a : b;
    }
}
