package com.example.bookiibookii.domain.user.dto.req;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class BookshelfRequestDTO {

    public record AddRepresentativeReqDTO(
            Long userBookId,
            Long memberBookId
    ) {}

    public record MoveRepresentativeReqDTO(
            @NotNull Long userBookId,
            @NotNull @Min(1) @Max(7) Integer targetOrder
    ) {}
}
