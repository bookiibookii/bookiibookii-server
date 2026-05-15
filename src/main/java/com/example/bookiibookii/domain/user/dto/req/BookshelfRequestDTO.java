package com.example.bookiibookii.domain.user.dto.req;

public class BookshelfRequestDTO {

    public record AddRepresentativeReqDTO(
            Long userBookId,
            Long groupBookId
    ) {}
}
