package com.urlshortener.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class Base62EncoderTest {

    @Test
    void should_padToSevenCharacters_when_encodingSmallIds() {
        assertThat(Base62Encoder.encode(1, 7)).isEqualTo("0000001");
        assertThat(Base62Encoder.encode(61, 7)).isEqualTo("000000Z");
        assertThat(Base62Encoder.encode(62, 7)).isEqualTo("0000010");
    }

    @Test
    void should_beCollisionFreeByConstruction_when_encodingSequentialIds() {
        String first = Base62Encoder.encode(100, 7);
        String second = Base62Encoder.encode(101, 7);
        assertThat(first).isNotEqualTo(second);
        assertThat(first).hasSize(7);
        assertThat(second).hasSize(7);
    }

    @Test
    void should_rejectNegativeValues() {
        assertThatThrownBy(() -> Base62Encoder.encode(-1, 7))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
