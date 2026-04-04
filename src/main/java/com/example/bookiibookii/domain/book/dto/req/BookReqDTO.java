package com.example.bookiibookii.domain.book.dto.req;

import jakarta.validation.constraints.NotBlank;

public class BookReqDTO {
    public record UserPickReqDTO(
            @NotBlank
            String isbn13
    ) {}
}
