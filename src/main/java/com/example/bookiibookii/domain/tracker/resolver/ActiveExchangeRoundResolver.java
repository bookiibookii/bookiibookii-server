package com.example.bookiibookii.domain.tracker.resolver;

import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.tracker.enums.ExchangeRound;
import com.example.bookiibookii.domain.tracker.enums.ReadingStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class ActiveExchangeRoundResolver {

    public Optional<ExchangeRound> resolve(List<MatchedMember> members) {
        if (members == null || members.isEmpty()) {
            return Optional.empty();
        }

        Optional<ExchangeRound> firstRound = resolve(members.get(0).getReadingStatus());
        if (firstRound.isEmpty()) {
            return Optional.empty();
        }

        ExchangeRound round = firstRound.get();
        return members.stream()
                .allMatch(member -> resolve(member.getReadingStatus()).filter(round::equals).isPresent())
                ? Optional.of(round)
                : Optional.empty();
    }

    public Optional<ExchangeRound> resolve(ReadingStatus readingStatus) {
        if (readingStatus == ReadingStatus.EXCHANGING) {
            return Optional.of(ExchangeRound.FIRST_EXCHANGE);
        }
        if (readingStatus == ReadingStatus.RETURNING) {
            return Optional.of(ExchangeRound.RETURN_EXCHANGE);
        }
        return Optional.empty();
    }
}
