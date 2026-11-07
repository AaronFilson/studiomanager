# Sending email from our own domain

Plan 22, lane E. The domain isn't chosen yet. Terraform is ready for it, and this is the order
to follow once it is.

**Why:**
- Dev sends from Azure's managed domain (`DoNotReply@<guid>.azurecomm.net`).
- Mail from it lands in junk, and the domain has low sending limits.
- A pilot needs a domain studios recognise.

## 0. Choose the domain

- Use a subdomain for sending, such as `mail.<product domain>`. Its SPF and DMARC then don't
  affect anything else sent from the main domain.
- You need to be able to add TXT and CNAME records to its DNS.

## 1. Add the domain (first apply)

1. In `infra/envs/dev/dev.tfvars`, set `email_custom_domain = "mail.example.com"`.
2. Merge, and let the deploy apply it. Nothing changes for senders yet.
3. Read the records to add:
   - from the deploy job's `terraform output email_domain_dns_records`; or
   - in the portal: Email Communication Service > Provision domains > the domain.

## 2. Add the DNS records and verify

1. At the DNS host, add the records Azure lists:
   - **Domain:** a TXT record proving ownership.
   - **SPF:** a TXT record `v=spf1 include:spf.protection.outlook.com -all`. If the name
     already has an SPF record, merge the include into it; a name may have only one.
   - **DKIM and DKIM2:** two CNAMEs, `selector1-azurecomm-prod-net._domainkey` and
     `selector2-...`.
2. In the portal, on the domain, choose **Verify** for each of Domain, SPF, DKIM and DKIM2.
   Verification can take from minutes to a day, depending on the DNS host.
3. Add a DMARC record yourself (Azure doesn't create one): TXT `_dmarc.mail.example.com` with
   `v=DMARC1; p=none; rua=mailto:<your address>`. Move to `p=quarantine` once the reports look
   clean.

## 3. Send from it (second apply)

1. Once all four show **Verified**, set `email_send_from_custom_domain = true`.
2. Merge. Terraform:
   - links the domain to the Communication Services resource (ACS refuses to link an unverified
     domain, which is why this is a second step);
   - changes the API's `MAIL_FROM` to `DoNotReply@mail.example.com`.
3. Check: send yourself an invite from a studio's Members page, and confirm it arrives in the
   inbox, not junk, with SPF, DKIM and DMARC passing (in Gmail: "Show original").

## Limits

New custom domains start with low sending quotas too. Ask Azure support to raise them before a
pilot sends invites in bulk. The pilot is a handful of studios, so the starting quota may be enough.

## Undo

Set `email_send_from_custom_domain = false` and apply. The API goes back to the Azure-managed
sender, and the domain stays verified for later.
