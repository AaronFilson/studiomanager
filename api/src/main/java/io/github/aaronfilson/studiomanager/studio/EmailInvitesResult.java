package io.github.aaronfilson.studiomanager.studio;

import java.util.List;

/** The outcome of pasting a list of addresses: who will get an invite, and who was left out. */
public record EmailInvitesResult(List<OpenInviteView> invited, List<Skipped> skipped) {

    public record Skipped(String email, Reason reason) {
    }

    public enum Reason {
        /** Someone in the studio already signs in with this address. */
        ALREADY_MEMBER,
        /** The address has an unexpired invite; resend that one instead. */
        ALREADY_INVITED
    }

}
