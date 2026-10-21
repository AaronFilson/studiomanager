package io.github.aaronfilson.studiomanager.studio;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EmailAddressesTests {

    @Test
    void findsAddressesInAnEmailToLine() {
        assertThat(EmailAddresses.extract("\"Jane Doe\" <Jane.Doe@Example.com>, Bob <bob+clay@mail.example.co.uk>"))
                .containsExactly("jane.doe@example.com", "bob+clay@mail.example.co.uk");
    }

    @Test
    void findsAddressesInASpreadsheetColumnAndDropsRepeats() {
        assertThat(EmailAddresses.extract("Email\nann@example.com\r\nbea@example.org\n\nANN@example.com\n"))
                .containsExactly("ann@example.com", "bea@example.org");
    }

    @Test
    void acceptsSemicolonsAndTrailingPunctuation() {
        assertThat(EmailAddresses.extract("ann@example.com; o'neil@example.ie. and bea@example.org."))
                .containsExactly("ann@example.com", "o'neil@example.ie", "bea@example.org");
    }

    @Test
    void ignoresTextWithoutAnAddress() {
        assertThat(EmailAddresses.extract("Ann, Bea and @someone at example dot com")).isEmpty();
    }

}
