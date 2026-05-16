package com.willembergfilho.ifinance.domain.index;

public enum EconomicIndex {
    IPCA(433, Periodicity.MONTHLY),
    IGP_M(189, Periodicity.MONTHLY),
    CDI(12, Periodicity.DAILY),
    SELIC(11, Periodicity.DAILY),
    TR(226, Periodicity.MONTHLY);

    public enum Periodicity {
        /** BCB retorna taxa diária em % — anualizar com (1+r)^252 */
        DAILY,
        /** BCB retorna taxa mensal em % — anualizar com (1+r)^12 */
        MONTHLY
    }

    private final int bcbSeriesCode;
    private final Periodicity periodicity;

    EconomicIndex(int bcbSeriesCode, Periodicity periodicity) {
        this.bcbSeriesCode = bcbSeriesCode;
        this.periodicity = periodicity;
    }

    public int getBcbSeriesCode() {
        return bcbSeriesCode;
    }

    public Periodicity getPeriodicity() {
        return periodicity;
    }
}
