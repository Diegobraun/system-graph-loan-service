package com.example.loan.loan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Testes de aceite da tarefa paginar-loans-3 com a aplicação inteira de pé (MockMvc). O repositório é o bean real
 * em memória; o Kafka fica desligado (nenhuma função ligada a binding e StreamBridge mockado), então nada depende
 * de broker local.
 */
@SpringBootTest(properties = {
        "spring.cloud.function.definition=",
        "spring.cloud.stream.function.autodetect=false"
})
@AutoConfigureMockMvc
class LoanV2PaginationAcceptanceTest {

    private static final Instant BASE = Instant.parse("2026-01-01T00:00:00Z");
    // Cada teste usa contas próprias, porque o repositório em memória é compartilhado pelo contexto.
    private static final AtomicLong ACCOUNTS = new AtomicLong(1_000);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LoanRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StreamBridge streamBridge;

    private long novaConta() {
        return ACCOUNTS.incrementAndGet();
    }

    private void popular(long accountId, int quantidade) {
        IntStream.range(0, quantidade).forEach(i -> repository.save(
                loan("acc%d-loan-%02d".formatted(accountId, i), accountId, BASE.plusSeconds(i))));
    }

    private static Loan loan(String id, long accountId, Instant createdAt) {
        return new Loan(id, accountId, 1L, new BigDecimal("1000.00"), 12, LoanStatus.values()[0], null, createdAt);
    }

    private JsonNode getJson(String url) throws Exception {
        String body = mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private static List<String> ids(JsonNode items) {
        List<String> ids = new ArrayList<>();
        items.forEach(item -> ids.add(item.get("id").asText()));
        return ids;
    }

    @Test
    void criterio1_semPageESizeUsaPaginaZeroTamanhoVinteETotalDaConta() throws Exception {
        long conta = novaConta();
        popular(conta, 25);

        mockMvc.perform(get("/v2/loans").param("accountId", String.valueOf(conta)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.total").value(25))
                .andExpect(jsonPath("$.items.length()").value(20));
    }

    @Test
    void criterio2_segundaPaginaTrazOsCincoRestantesSemRepetirDaPrimeira() throws Exception {
        long conta = novaConta();
        popular(conta, 25);

        JsonNode pagina0 = getJson("/v2/loans?accountId=" + conta + "&page=0&size=20");
        JsonNode pagina1 = getJson("/v2/loans?accountId=" + conta + "&page=1&size=20");

        assertThat(pagina1.get("items")).hasSize(5);
        assertThat(pagina1.get("total").asLong()).isEqualTo(25);
        assertThat(pagina1.get("page").asInt()).isEqualTo(1);
        assertThat(ids(pagina1.get("items"))).doesNotContainAnyElementsOf(ids(pagina0.get("items")));
        List<String> todos = new ArrayList<>(ids(pagina0.get("items")));
        todos.addAll(ids(pagina1.get("items")));
        assertThat(todos).doesNotHaveDuplicates().hasSize(25);
    }

    @Test
    void criterio3_ordenaPorCreatedAtDecrescenteEDesempataPorIdCrescente() throws Exception {
        long conta = novaConta();
        repository.save(loan("c", conta, BASE));
        repository.save(loan("b", conta, BASE.plusSeconds(60)));
        repository.save(loan("a", conta, BASE.plusSeconds(60)));
        repository.save(loan("d", conta, BASE.plusSeconds(120)));

        JsonNode pagina = getJson("/v2/loans?accountId=" + conta);

        assertThat(ids(pagina.get("items"))).containsExactly("d", "a", "b", "c");
    }

    @Test
    void criterio4_contaSemEmprestimosDevolveItemsVazioETotalZero() throws Exception {
        long conta = novaConta();

        mockMvc.perform(get("/v2/loans").param("accountId", String.valueOf(conta)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void criterio5_paginaAlemDoFimDevolveItemsVazioComTotal() throws Exception {
        long conta = novaConta();
        popular(conta, 25);

        mockMvc.perform(get("/v2/loans").param("accountId", String.valueOf(conta))
                        .param("page", "5").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.total").value(25));
    }

    @ParameterizedTest(name = "page={0}, size={1} -> 400")
    @CsvSource({"-1,20", "0,0", "0,101"})
    void criterio6_paginacaoInvalidaDevolve400(String page, String size) throws Exception {
        mockMvc.perform(get("/v2/loans").param("accountId", "1").param("page", page).param("size", size))
                .andExpect(status().isBadRequest());
    }

    @Test
    void criterio6_tamanhoCemEAceito() throws Exception {
        long conta = novaConta();
        popular(conta, 25);

        mockMvc.perform(get("/v2/loans").param("accountId", String.valueOf(conta)).param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100))
                .andExpect(jsonPath("$.items.length()").value(25));
    }

    @Test
    void criterio7_itemsETotalConsideramSoAContaPaginada() throws Exception {
        long conta = novaConta();
        long outra = novaConta();
        popular(conta, 3);
        popular(outra, 7);

        JsonNode pagina = getJson("/v2/loans?accountId=" + conta);

        assertThat(pagina.get("total").asLong()).isEqualTo(3);
        assertThat(pagina.get("items")).hasSize(3);
        pagina.get("items").forEach(item -> assertThat(item.get("accountId").asLong()).isEqualTo(conta));
    }

    @Test
    void criterio8_getLoansContinuaDevolvendoArrayComTodosOsEmprestimos() throws Exception {
        long conta = novaConta();
        popular(conta, 25);

        mockMvc.perform(get("/loans").param("accountId", String.valueOf(conta)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(25))
                .andExpect(jsonPath("$.items").doesNotExist())
                .andExpect(jsonPath("$.total").doesNotExist());
    }
}
