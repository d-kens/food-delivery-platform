package com.example.user_service.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmailsTest {

    @Test
    void normalize_trimsAndLowercases() {
        assertThat(Emails.normalize("  Jane.Doe@Gmail.COM ")).isEqualTo("jane.doe@gmail.com");
    }

    @Test
    void normalize_returnsNull_whenEmailIsNull() {
        assertThat(Emails.normalize(null)).isNull();
    }
}
