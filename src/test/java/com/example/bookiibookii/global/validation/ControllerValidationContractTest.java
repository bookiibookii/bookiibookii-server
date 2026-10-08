package com.example.bookiibookii.global.validation;

import com.example.bookiibookii.domain.group.controller.GroupController;
import com.example.bookiibookii.domain.memberbook.controller.MemberBookCardController;
import com.example.bookiibookii.domain.memberbook.controller.MemberBookLibraryController;
import com.example.bookiibookii.domain.tracker.controller.PackageDeliveryController;
import com.example.bookiibookii.domain.user.controller.UserWithdrawalController;
import com.example.bookiibookii.global.auth.controller.AuthController;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;

class ControllerValidationContractTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @ParameterizedTest
    @ValueSource(classes = {
            AuthController.class,
            MemberBookCardController.class,
            MemberBookLibraryController.class,
            GroupController.class,
            PackageDeliveryController.class,
            UserWithdrawalController.class
    })
    void controllerValidationMetadataDoesNotViolateOverrideContract(Class<?> controllerType) {
        assertThatCode(() -> validator.getConstraintsForClass(controllerType))
                .doesNotThrowAnyException();
    }
}
