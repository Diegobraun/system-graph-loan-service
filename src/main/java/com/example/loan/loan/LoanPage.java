package com.example.loan.loan;

import java.util.List;

public record LoanPage(List<Loan> items, int page, int size, long total) {
}
