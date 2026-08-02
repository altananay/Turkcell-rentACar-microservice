package com.kodlamaio.commonpackage.utils.annotations;

import org.junit.jupiter.api.Test;

import java.time.Year;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotFutureYearValidatorTest {

    private final NotFutureYearValidator validator = new NotFutureYearValidator();

    @Test
    void isValid_withPastYear_returnsTrue() {
        var pastYear = Year.now().getValue() - 1;

        assertThat(validator.isValid(pastYear, null)).isTrue();
    }

    @Test
    void isValid_withCurrentYear_returnsTrue() {
        var currentYear = Year.now().getValue();

        assertThat(validator.isValid(currentYear, null)).isTrue();
    }

    @Test
    void isValid_withFutureYear_returnsFalse() {
        var futureYear = Year.now().getValue() + 1;

        assertThat(validator.isValid(futureYear, null)).isFalse();
    }

    @Test
    void isValid_withNullValue_throwsNullPointerException() {
        // Documents existing behavior: isValid unboxes `value` directly (value <= currentYear),
        // so a null Integer throws NPE instead of being treated as valid/invalid. Not a fix target.
        assertThatThrownBy(() -> validator.isValid(null, null))
                .isInstanceOf(NullPointerException.class);
    }
}
