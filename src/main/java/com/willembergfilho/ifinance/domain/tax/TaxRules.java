package com.willembergfilho.ifinance.domain.tax;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

/** Legal rates and limits for calendar year 2026. */
public final class TaxRules {

    // LC 123/2006 art. 3º, II (LC 155/2016) — valid since 2018
    public static final BigDecimal SIMPLES_LIMIT = new BigDecimal("4800000");
    // LC 123/2006 Anexos I a V (LC 155/2016): upper RBT12 limit of each of the 6 faixas, same for every annex
    public static final List<BigDecimal> SIMPLES_BRACKET_LIMITS = Stream.of(
            "180000", "360000", "720000", "1800000", "3600000", "4800000").map(BigDecimal::new).toList();
    // LC 123/2006 art. 13-A (LC 155/2016) — valid since 2018
    public static final BigDecimal SIMPLES_SUBLIMIT = new BigDecimal("3600000");
    // LC 123/2006 art. 18, §§ 5º-J e 5º-M (Fator R)
    public static final BigDecimal FATOR_R_THRESHOLD = new BigDecimal("0.28");
    // Lei 8.036/1990 art. 15: FGTS 8%, part of the Fator R payroll (LC 123/2006 art. 18, § 24)
    public static final BigDecimal FGTS_RATE = new BigDecimal("0.08");
    // LC 123/2006 Anexos III e IV, nota da 5ª faixa: ISS efetivo máximo de 5%
    public static final BigDecimal SIMPLES_ISS_CAP = new BigDecimal("0.05");

    // Lei 9.718/1998 art. 13 (redação Lei 12.814/2013)
    public static final BigDecimal PRESUMIDO_LIMIT = new BigDecimal("78000000");
    // LC 224/2025 art. 4º, § 4º, VII e § 5º: presunção +10% sobre a receita que exceder R$ 5 mi no ano (2026)
    public static final BigDecimal PRESUMPTION_SURCHARGE = new BigDecimal("0.10");
    public static final BigDecimal PRESUMPTION_SURCHARGE_THRESHOLD = new BigDecimal("5000000");

    // Lei 9.249/1995 art. 3º: 15% + adicional de 10% acima de R$ 20.000/mês (R$ 240.000/ano)
    public static final BigDecimal IRPJ_RATE = new BigDecimal("0.15");
    public static final BigDecimal IRPJ_SURTAX_RATE = new BigDecimal("0.10");
    public static final BigDecimal IRPJ_SURTAX_ANNUAL_THRESHOLD = new BigDecimal("240000");
    // Lei 7.689/1988 art. 3º, III
    public static final BigDecimal CSLL_RATE = new BigDecimal("0.09");

    // Lei 9.715/1998 art. 8º e Lei 9.718/1998 art. 8º (cumulativo)
    public static final BigDecimal PIS_CUMULATIVE = new BigDecimal("0.0065");
    public static final BigDecimal COFINS_CUMULATIVE = new BigDecimal("0.03");
    // Lei 10.637/2002 art. 2º e Lei 10.833/2003 art. 2º (não cumulativo)
    public static final BigDecimal PIS_NON_CUMULATIVE = new BigDecimal("0.0165");
    public static final BigDecimal COFINS_NON_CUMULATIVE = new BigDecimal("0.076");

    // INSS patronal 20% + RAT 2% (Lei 8.212/1991 art. 22, I e II) + terceiros 5,8%
    // (salário-educação 2,5%, Incra 0,2%, Sistema S 2,5%, Sebrae 0,6%). FGTS fica fora: igual em todos os regimes.
    public static final BigDecimal DEFAULT_PAYROLL_CHARGES_RATE = new BigDecimal("0.278");
    // Anexo IV paga CPP fora do DAS (LC 123/2006 art. 18, § 5º-C), sem terceiros (art. 13, § 3º): 20% + RAT 2%
    public static final BigDecimal ANNEX_IV_CPP_RATE = new BigDecimal("0.22");

    public static final String DISCLAIMER =
            "Simulação simplificada e educativa; não substitui a análise de um contador. Regras vigentes em 2026: "
            + "LC 123/2006 (Anexos da LC 155/2016), Leis 9.249/1995, 9.718/1998, 10.637/2002, 10.833/2003 e LC 224/2025 "
            + "(acréscimo de 10% na presunção do Lucro Presumido, questionado na ADI 7936). IPI fora do Simples, "
            + "substituição tributária, incentivos fiscais e FGTS não são considerados. A Reforma Tributária "
            + "(EC 132/2023 e LC 214/2025) não é modelada: em 2026 a CBS (0,9%) e o IBS (0,1%) são cobrados em caráter "
            + "de teste e, a partir de 2027, a CBS substitui o PIS e a Cofins.";

    private TaxRules() {}
}
