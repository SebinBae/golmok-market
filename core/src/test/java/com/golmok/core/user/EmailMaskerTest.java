package com.golmok.core.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class EmailMaskerTest {

    @ParameterizedTest
    @CsvSource({
            "hyeonjin@gmail.com, hy****@gmail.com",
            "bae@gmail.com,      ba****@gmail.com",
            "ab@naver.com,       ab****@naver.com",
            "a@x.com,            a****@x.com",
            "sebin@sub.co.kr,    se****@sub.co.kr"
    })
    void 앞_두_글자만_남기고_가린다(String email, String expected) {
        assertThat(EmailMasker.mask(email)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "short@x.com,           2",
            "verylongaddress@x.com, 2"
    })
    void 별표_개수는_원래_길이와_무관하게_항상_넷이다(String email, int visibleLength) {
        String masked = EmailMasker.mask(email);

        assertThat(masked.chars().filter(ch -> ch == '*').count()).isEqualTo(4);
        assertThat(masked).startsWith(email.substring(0, visibleLength));
    }

    @org.junit.jupiter.api.Test
    void 골뱅이가_없으면_전부_가린다() {
        assertThat(EmailMasker.mask("broken")).isEqualTo("****");
    }
}
