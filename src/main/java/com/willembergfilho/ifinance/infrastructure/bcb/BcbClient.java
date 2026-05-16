package com.willembergfilho.ifinance.infrastructure.bcb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Component
public class BcbClient {

    private static final Logger log = LoggerFactory.getLogger(BcbClient.class);
    private static final DateTimeFormatter BCB_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String SGS_PATH = "/dados/serie/bcdata.sgs.{code}/dados";

    private final RestClient restClient;

    public BcbClient(@Value("${bcb.api.base-url:https://api.bcb.gov.br}") String baseUrl,
                     RestClient.Builder builder) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public List<BcbIndexResponse> fetchSeries(int seriesCode, LocalDate from, LocalDate to) {
        String dataInicial = from.format(BCB_FORMAT);
        String dataFinal = to.format(BCB_FORMAT);

        try {
            List<BcbIndexResponse> response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(SGS_PATH)
                            .queryParam("formato", "json")
                            .queryParam("dataInicial", dataInicial)
                            .queryParam("dataFinal", dataFinal)
                            .build(seriesCode))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            return response != null ? response : Collections.emptyList();
        } catch (RestClientException e) {
            log.warn("BCB API unavailable for series {} [{} to {}]: {}", seriesCode, dataInicial, dataFinal, e.getMessage());
            return Collections.emptyList();
        }
    }
}
