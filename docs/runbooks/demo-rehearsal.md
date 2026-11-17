# Demo rehearsal

Plan 22, lane E (plan 20, "Finish": "rehearse the demo with a scheduled boost"). Run through this
once end to end before a real demo, then use it as the day-of checklist.

## The day before

1. **The latest deploy is green,** including the Playwright smoke job.
2. **Reset the demo studio:** Admin > Demo studio > **Reset the demo studio**. Open it and check
   that kilns, pieces, firings, glazes, maintenance, supplies and the ledger are all there. A
   reset keeps the Anthropic key and the platform-readers consent, and moves the invoice readers'
   rules to the new studio (plan 24). The very first time, add them: Settings > AI for invoices,
   paste the key, **Test key**, and tick "Allow the platform's readers".
3. **Check the readers:** Admin > Invoice readers. Each platform reader says whether it's set up on
   this server, and should be on for Demo Clay Co. Then upload the invoices you'll show (photos
   under 4 MB, 1–2 pages) and run **Compare readers** once on each, so the first read on stage
   isn't the first read ever. A reset deletes them, so upload after resetting.
4. **Make a demo token:** Admin > Demo studio > **Make a demo token**. It reads and proposes, and
   lasts a day. Keep it somewhere you can paste from.
5. **Connect an assistant** with it (`docs/guides/connect-an-ai-assistant.md`). Ask the
   "morning check-in": today, open firings, low supplies, maintenance due and problems. Every tool
   should answer.
6. **Schedule the boost:** Admin > Limits > **Demo: 30 minutes unlimited, then ease down over an
   hour**. Start it 10 minutes before the demo, scoped to the demo studio.
7. **Sign in with GitHub** in a private window, if github_sign_in is on, so you know the
   interviewer's path works.
8. **Merge nothing on demo day.** A deploy rolls a new revision and puts the API's minimum
   replicas back to 0, which ends "Warm for an hour" early.
9. **Webhooks:** the seed has no endpoint. Add one that points at a request bin before the demo.

## 15 minutes before

1. **Wake the API:** Actions > **Warm for an hour** > Run, just before you start: it lasts
   exactly an hour. Or open the site and wait for Today to load.
2. Open, in tabs:
   - the demo studio's Today page;
   - Roles (owner);
   - Webhooks;
   - Supplies, with the invoices you uploaded already read, and one more photo ready to upload
     live;
   - the assistant.
3. Admin > Limits should say "Unlimited for N more minutes".

## The tour (about 15 minutes)

| Minute | Show | The point |
|---|---|---|
| 0–2 | Today, a piece's path from greenware to pickup | The domain: it knows what a kiln is |
| 2–5 | Roles: untick "Refund and adjust" for Staff, see "N people lose…", the access history | Owner-configurable RBAC, audited, can't lock yourself out |
| 5–8 | The assistant: the morning check-in, then "reorder what's under par" (it proposes purchase requests) and approve one under Assistant suggestions | MCP on the official SDK; assistants propose, people decide |
| 8–11 | Supplies' invoice inbox: upload a photo live; the draft beside the photo; a line matched to stock; then **Compare readers** on the handwritten one | Untrusted input to an LLM with one output schema; checks that explain themselves; three readers side by side with time and cost, and why any didn't run |
| 11–13 | Webhooks: send a test, show the signed payload and the delivery log | Standard Webhooks, SSRF-safe, retried |
| 13–15 | The repo: README diagram, ADRs, CI checks, Error Prone and NullAway with `-Werror` | How it's built |

## Afterwards

1. Admin > Limits > **End now**, if the boost is still running.
2. Revoke the demo token (Settings > API tokens).
3. Note what was slow or confusing in `docs/pilot/diary.md`.
