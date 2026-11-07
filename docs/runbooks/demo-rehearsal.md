# Demo rehearsal

Plan 22, lane E (plan 20, "Finish": "rehearse the demo with a scheduled boost"). Run through this
once end to end before a real demo, then use it as the day-of checklist.

## The day before

1. **The latest deploy is green,** including the Playwright smoke job.
2. **Reset the demo studio:** Admin > Demo studio > **Reset the demo studio**. Open it and check
   that kilns, pieces, firings, glazes, maintenance and the ledger are all there.
3. **Make a demo token:** Admin > Demo studio > **Make a demo token**. It's read-scoped and
   lasts a day. Keep it somewhere you can paste from.
4. **Connect an assistant** with it (`docs/guides/connect-an-ai-assistant.md`). Ask the
   "morning check-in": today, open firings, low supplies, maintenance due and problems. Every tool
   should answer.
5. **Schedule the boost:** Admin > Limits > **Demo: 30 minutes unlimited, then ease down over an
   hour**. Start it 10 minutes before the demo, scoped to the demo studio.
6. **Sign in with GitHub** in a private window, if github_sign_in is on, so you know the
   interviewer's path works.

## 15 minutes before

1. **Wake the API:** Actions > **Warm for an hour** > Run. Or open the site and wait for Today to
   load.
2. Open, in tabs:
   - the demo studio's Today page;
   - Roles (owner);
   - Webhooks;
   - Supplies, with an invoice in its invoice inbox ready to read;
   - the assistant.
3. Admin > Limits should say "Unlimited for N more minutes".

## The tour (about 15 minutes)

| Minute | Show | The point |
|---|---|---|
| 0–2 | Today, a piece's path from greenware to pickup | The domain: it knows what a kiln is |
| 2–5 | Roles: untick "Refund payments" for Staff, see "N people lose…", the access history | Owner-configurable RBAC, audited, can't lock yourself out |
| 5–8 | The assistant: the morning check-in, then propose a glaze change and approve it | MCP on the official SDK; assistants propose, people decide |
| 8–11 | Supplies' invoice inbox: read one, then **Compare readers** (Claude, Azure, Google) | Three AI services, keyless, side by side with time and cost |
| 11–13 | Webhooks: send a test, show the signed payload and the delivery log | Standard Webhooks, SSRF-safe, retried |
| 13–15 | The repo: README diagram, ADRs, CI checks, Error Prone and NullAway with `-Werror` | How it's built |

## Afterwards

1. Admin > Limits > **End now**, if the boost is still running.
2. Revoke the demo token (Settings > API tokens).
3. Note what was slow or confusing in `docs/pilot/diary.md`.
