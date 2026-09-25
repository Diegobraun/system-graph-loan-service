package com.example.loan.loan;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class LoanRepositoryTest {

    private static final Instant BASE = Instant.parse("2026-01-01T00:00:00Z");

    private final LoanRepository repository = new LoanRepository();

    private static Loan loan(String id, Long accountId, Instant createdAt) {
        return new Loan(id, accountId, 1L, new BigDecimal("1000.00"), 12, LoanStatus.values()[0], null, createdAt);
    }

    @Test
    void ordenaPorCreatedAtDecrescenteEDesempataPorIdCrescente() {
        repository.save(loan("c", 1L, BASE));
        repository.save(loan("b", 1L, BASE.plusSeconds(60)));
        repository.save(loan("a", 1L, BASE.plusSeconds(60)));
        repository.save(loan("d", 1L, BASE.plusSeconds(120)));

        LoanPage page = repository.findPageByAccount(1L, 0, 20);

        assertThat(page.items()).extracting(Loan::id).containsExactly("d", "a", "b", "c");
        assertThat(page.total()).isEqualTo(4);
    }

    @Test
    void paginaSemRepetirItensEConsiderandoSoAConta() {
        IntStream.range(0, 25).forEach(i ->
                repository.save(loan("loan-%02d".formatted(i), 1L, BASE.plusSeconds(i))));
        repository.save(loan("outra", 2L, BASE));

        LoanPage first = repository.findPageByAccount(1L, 0, 20);
        LoanPage second = repository.findPageByAccount(1L, 1, 20);
        LoanPage beyond = repository.findPageByAccount(1L, 5, 20);

        assertThat(first.items()).hasSize(20);
        assertThat(first.total()).isEqualTo(25);
        assertThat(first.page()).isZero();
        assertThat(first.size()).isEqualTo(20);
        assertThat(second.items()).hasSize(5).doesNotContainAnyElementsOf(first.items());
        assertThat(second.total()).isEqualTo(25);
        assertThat(beyond.items()).isEmpty();
        assertThat(beyond.total()).isEqualTo(25);
    }

    @Test
    void contaSemEmprestimosDevolvePaginaVazia() {
        repository.save(loan("x", 2L, BASE));

        LoanPage page = repository.findPageByAccount(1L, 0, 20);

        assertThat(page.items()).isEmpty();
        assertThat(page.total()).isZero();
    }
}
