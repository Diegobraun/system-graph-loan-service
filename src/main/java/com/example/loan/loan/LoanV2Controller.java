package com.example.loan.loan;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Listagem paginada de empréstimos por conta. Sem {@code @Validated} na classe de propósito: assim o Spring MVC
 * valida os parâmetros com a validação de método nativa e responde 400 (HandlerMethodValidationException). Com
 * {@code @Validated} a validação passaria pelo proxy AOP e a ConstraintViolationException viraria 500.
 */
@RestController
@RequestMapping("/v2/loans")
public class LoanV2Controller {

    private final LoanRepository repository;

    public LoanV2Controller(LoanRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public LoanPage byAccount(@RequestParam Long accountId,
                              @RequestParam(defaultValue = "0") @Min(0) int page,
                              @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return repository.findPageByAccount(accountId, page, size);
    }
}
