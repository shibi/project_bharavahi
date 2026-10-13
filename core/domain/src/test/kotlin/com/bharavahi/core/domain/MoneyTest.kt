package com.bharavahi.core.domain

import com.bharavahi.core.domain.validation.Money
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MoneyTest {
    @Test
    fun `parses rupees into paise`() {
        assertThat(Money.parseRupees("1500")).isEqualTo(150_000L)
        assertThat(Money.parseRupees("1,500")).isEqualTo(150_000L)
        assertThat(Money.parseRupees(" ₹12.5 ")).isEqualTo(1_250L)
        assertThat(Money.parseRupees("0.05")).isEqualTo(5L)
    }

    @Test
    fun `rejects anything else`() {
        assertThat(Money.parseRupees("")).isNull()
        assertThat(Money.parseRupees("-5")).isNull()
        assertThat(Money.parseRupees("1.234")).isNull()
        assertThat(Money.parseRupees("abc")).isNull()
    }

    @Test
    fun `formats for text fields`() {
        assertThat(Money.toInput(150_000)).isEqualTo("1500")
        assertThat(Money.toInput(1_250)).isEqualTo("12.50")
    }
}

class MoneyFormatTest {
    @Test
    fun `indian grouping`() {
        assertThat(Money.format(15_000_000)).isEqualTo("₹1,50,000")
        assertThat(Money.format(123_456_789_00)).isEqualTo("₹12,34,56,789")
        assertThat(Money.format(99_900)).isEqualTo("₹999")
        assertThat(Money.format(1_250)).isEqualTo("₹12.50")
        assertThat(Money.format(-50_000)).isEqualTo("-₹500")
    }
}
