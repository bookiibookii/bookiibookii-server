package com.example.bookiibookii.domain.user.controller;

import com.example.bookiibookii.domain.user.dto.req.UserRequestDTO;
import com.example.bookiibookii.domain.user.enums.WithdrawalReason;
import com.example.bookiibookii.domain.user.service.UserWithdrawalService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserWithdrawalControllerTest {

    @Mock
    private UserWithdrawalService userWithdrawalService;

    private UserWithdrawalController controller;
    private MockMvc mockMvc;
    private Validator validator;

    @BeforeEach
    void setUp() {
        controller = new UserWithdrawalController(userWithdrawalService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void withdrawalValidationMetadataDoesNotConflictWithDocsInterface() throws Exception {
        Method method = UserWithdrawalController.class.getMethod(
                "withdraw",
                com.example.bookiibookii.domain.user.entity.User.class,
                UserRequestDTO.WithdrawalReqDTO.class
        );
        Object[] parameters = {
                null,
                new UserRequestDTO.WithdrawalReqDTO(WithdrawalReason.CUSTOM_INPUT, null)
        };

        assertThatCode(() -> validator.forExecutables()
                .validateParameters(controller, method, parameters))
                .doesNotThrowAnyException();
    }

    @Test
    void withdrawalRejectsRequestWithoutReasonAtValidationStage() throws Exception {
        mockMvc.perform(post("/api/users/me/withdrawal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customReason": "탈퇴 사유"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userWithdrawalService);
    }
}
