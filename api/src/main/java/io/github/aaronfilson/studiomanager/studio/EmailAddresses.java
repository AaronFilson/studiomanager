package io.github.aaronfilson.studiomanager.studio;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds the email addresses in pasted text: a spreadsheet column, a mailing list export, or an
 * email's To line such as {@code "Jane Doe" <jane@example.com>, bob@example.com}. Anything that
 * isn't an address is ignored.
 */
final class EmailAddresses {

    // Deliberately loose: the invite email itself is the real check that an address works
    private static final Pattern ADDRESS = Pattern.compile(
            "[A-Za-z0-9._%+'-]+@[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}");

    private EmailAddresses() {
    }

    /** The distinct addresses in {@code text}, lowercased, in the order they first appear. */
    static List<String> extract(String text) {
        Set<String> found = new LinkedHashSet<>();
        Matcher matcher = ADDRESS.matcher(text);
        while (matcher.find()) {
            String address = matcher.group();
            if (address.length() <= 254) {
                found.add(address.toLowerCase(Locale.ROOT));
            }
        }
        return List.copyOf(found);
    }

}
