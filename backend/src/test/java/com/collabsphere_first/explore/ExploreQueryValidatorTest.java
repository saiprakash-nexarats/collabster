package com.collabsphere_first.explore;

import com.collabsphere_first.explore.config.ExploreProperties;
import com.collabsphere_first.explore.exception.InvalidExploreQueryException;
import com.collabsphere_first.explore.search.ExploreQueryValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExploreQueryValidatorTest {

    private ExploreQueryValidator validator;

    @BeforeEach
    void setUp() {
        ExploreProperties props = new ExploreProperties();
        validator = new ExploreQueryValidator(props);
    }

    @Test
    @DisplayName("Valid query is trimmed and normalized")
    void testValidQuery() {
        String result = validator.validateAndNormalize("   rust programming   ");
        assertThat(result).isEqualTo("rust programming");
    }

    @Test
    @DisplayName("Null query is rejected")
    void testNullQuery() {
        assertThatThrownBy(() -> validator.validateAndNormalize(null))
                .isInstanceOf(InvalidExploreQueryException.class)
                .hasMessageContaining("null or empty");
    }

    @Test
    @DisplayName("Empty or blank query is rejected")
    void testBlankQuery() {
        assertThatThrownBy(() -> validator.validateAndNormalize("   "))
                .isInstanceOf(InvalidExploreQueryException.class)
                .hasMessageContaining("empty");
    }

    @Test
    @DisplayName("Query shorter than minimum length is rejected")
    void testTooShortQuery() {
        assertThatThrownBy(() -> validator.validateAndNormalize("a"))
                .isInstanceOf(InvalidExploreQueryException.class)
                .hasMessageContaining("at least 2 characters");
    }

    @Test
    @DisplayName("Query longer than maximum length is rejected")
    void testTooLongQuery() {
        String longQuery = "a".repeat(101);
        assertThatThrownBy(() -> validator.validateAndNormalize(longQuery))
                .isInstanceOf(InvalidExploreQueryException.class)
                .hasMessageContaining("cannot exceed 100 characters");
    }

    @Test
    @DisplayName("Unicode queries are safely normalized")
    void testUnicodeNormalization() {
        String unicodeQuery = "Café \u0065\u0301";
        String normalized = validator.validateAndNormalize(unicodeQuery);
        assertThat(normalized).isNotBlank();
    }
}
