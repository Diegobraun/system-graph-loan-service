package com.example.loan.loan;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LoanV2Controller.class)
class LoanV2ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LoanRepository repository;

    @Test
    void usaPaginaZeroETamanhoVintePorPadrao() throws Exception {
        when(repository.findPageByAccount(7L, 0, 20)).thenReturn(new LoanPage(List.of(), 0, 20, 0));

        mockMvc.perform(get("/v2/loans").param("accountId", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.total").value(0));

        verify(repository).findPageByAccount(7L, 0, 20);
    }

    @ParameterizedTest
    @CsvSource({"-1,20", "0,0", "0,101"})
    void rejeitaPaginacaoInvalidaCom400(String page, String size) throws Exception {
        mockMvc.perform(get("/v2/loans").param("accountId", "7").param("page", page).param("size", size))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(repository);
    }

    @Test
    void aceitaTamanhoCem() throws Exception {
        when(repository.findPageByAccount(anyLong(), anyInt(), anyInt())).thenReturn(new LoanPage(List.of(), 0, 100, 0));

        mockMvc.perform(get("/v2/loans").param("accountId", "7").param("size", "100"))
                .andExpect(status().isOk());
    }
}
