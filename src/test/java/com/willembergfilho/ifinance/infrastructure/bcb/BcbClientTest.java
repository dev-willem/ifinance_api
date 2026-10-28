package com.willembergfilho.ifinance.infrastructure.bcb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class BcbClientTest {

    private MockRestServiceServer server;
    private BcbClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new BcbClient("https://api.bcb.gov.br", builder);
    }

    @Test
    void parsesSgsSeriesUsingBcbDateFormatInQuery() {
        server.expect(requestTo(startsWith("https://api.bcb.gov.br/dados/serie/bcdata.sgs.433/dados")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("formato", "json"))
                .andExpect(queryParam("dataInicial", "01/01/2026"))
                .andExpect(queryParam("dataFinal", "31/03/2026"))
                .andRespond(withSuccess("""
                        [{"data":"01/01/2026","valor":"0.16"},
                         {"data":"01/02/2026","valor":"1.31"},
                         {"data":"01/03/2026","valor":"0.56"}]
                        """, MediaType.APPLICATION_JSON));

        List<BcbIndexResponse> result = client.fetchSeries(433, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31));

        server.verify();
        assertThat(result).containsExactly(
                new BcbIndexResponse("01/01/2026", "0.16"),
                new BcbIndexResponse("01/02/2026", "1.31"),
                new BcbIndexResponse("01/03/2026", "0.56"));
    }

    @Test
    void emptyArrayYieldsEmptyList() {
        server.expect(requestTo(startsWith("https://api.bcb.gov.br/dados/serie/bcdata.sgs.12/dados")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(client.fetchSeries(12, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2))).isEmpty();
    }

    @Test
    void serverErrorYieldsEmptyList() {
        server.expect(requestTo(startsWith("https://api.bcb.gov.br/")))
                .andRespond(withServerError());

        assertThat(client.fetchSeries(433, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31))).isEmpty();
        server.verify();
    }

    @Test
    void notFoundYieldsEmptyList() {
        server.expect(requestTo(startsWith("https://api.bcb.gov.br/")))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(client.fetchSeries(433, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31))).isEmpty();
    }
}
