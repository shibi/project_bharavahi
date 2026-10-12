package com.bharavahi.core.domain

import com.bharavahi.core.domain.error.ValidationError
import com.bharavahi.core.domain.validation.Validators
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ValidatorsTest {
    @Test
    fun email() {
        assertThat(Validators.email("a@b.co")).isNull()
        assertThat(Validators.email(" ")).isEqualTo(ValidationError.REQUIRED)
        assertThat(Validators.email("a@b")).isEqualTo(ValidationError.INVALID_EMAIL)
    }

    @Test
    fun phone() {
        assertThat(Validators.optionalPhone("")).isNull()
        assertThat(Validators.optionalPhone("+91 98765 43210")).isNull()
        assertThat(Validators.optionalPhone("12ab")).isEqualTo(ValidationError.INVALID_PHONE)
    }

    @Test
    fun password() {
        assertThat(Validators.password("")).isEqualTo(ValidationError.REQUIRED)
        assertThat(Validators.password("1234567")).isEqualTo(ValidationError.PASSWORD_TOO_SHORT)
        assertThat(Validators.password("12345678")).isNull()
    }
}
