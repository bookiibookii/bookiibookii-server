package com.example.bookiibookii.domain.group.controller;

import com.example.bookiibookii.domain.group.service.MatchedMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/groups")
public class MatchedMemberController {

    private final MatchedMemberService matchedMemberService;
}
