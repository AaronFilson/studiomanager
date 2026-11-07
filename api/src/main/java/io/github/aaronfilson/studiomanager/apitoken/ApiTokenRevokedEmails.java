package io.github.aaronfilson.studiomanager.apitoken;

import io.github.aaronfilson.studiomanager.platform.mail.Email;
import io.github.aaronfilson.studiomanager.platform.mail.EmailSender;
import io.github.aaronfilson.studiomanager.platform.mail.MailProperties;
import io.github.aaronfilson.studiomanager.platform.tenancy.AccountDirectory;
import java.net.URI;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Emails a token's owner when it's revoked, saying who revoked it. Runs after the revoke commits,
 * from the event publication registry, so a failed send is retried like any other email.
 */
@Component
class ApiTokenRevokedEmails {

  private static final Logger log = LoggerFactory.getLogger(ApiTokenRevokedEmails.class);

  private final AccountDirectory accounts;
  private final EmailSender sender;
  private final URI webOrigin;

  ApiTokenRevokedEmails(AccountDirectory accounts, EmailSender sender, MailProperties mail) {
    this.accounts = accounts;
    this.sender = sender;
    this.webOrigin = mail.webOrigin();
  }

  @ApplicationModuleListener
  void on(ApiTokenRevoked revoked) {
    String address =
        accounts.emails(List.of(revoked.ownerAccountId())).get(revoked.ownerAccountId());
    if (address == null) {
      log.debug(
          "No email for account {}, so no notice that token {} was revoked",
          revoked.ownerAccountId(),
          revoked.tokenId());
      return;
    }
    sender.send(email(address, revoked));
  }

  private Email email(String address, ApiTokenRevoked revoked) {
    boolean byOwner = revoked.revokedByAccountId().equals(revoked.ownerAccountId());
    String who;
    if (byOwner) {
      who = "You revoked it.";
    } else {
      String revokerName =
          accounts
              .displayNames(List.of(revoked.revokedByAccountId()))
              .getOrDefault(revoked.revokedByAccountId(), "The studio's owner");
      who = revokerName + " revoked it.";
    }
    String link = webOrigin.resolve("/studios/" + revoked.studioId()).toString();
    String text =
        """
        Your API token "%s" no longer works. %s

        Anything that used it, such as an AI assistant or a script, needs a new token. \
        Make one under Settings, then API access: %s

        If you didn't expect this, tell the studio's owner.\
        """
            .formatted(revoked.tokenName(), who, link);
    return new Email(address, "Your API token \"" + revoked.tokenName() + "\" was revoked", text);
  }
}
