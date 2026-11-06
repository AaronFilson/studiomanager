# Connect an AI assistant to your studio

Studio Manager runs an MCP server for each studio. An assistant that speaks MCP, such as Claude,
can read the studio through it as you: today's firings and bookings, kilns, low supplies,
maintenance due, open problem reports and the glaze book. **Every tool only reads.** Nothing an
assistant does through it changes the studio.

## 1. Make a token

Go to Settings, then **API access**. Name the token after the assistant ("Claude desktop"),
leave **Read** ticked, pick an expiry, and select **Make token**. Copy the `smpat_...` value now,
because it isn't shown again.

The token acts as you, in this studio only. It stops working if you stop being staff, and
you can revoke it from the same card at any time.

## 2. Add the server

The server's address is your studio's API address plus the studio id:

```
https://<api-host>/api/v1/studios/<studio-id>/mcp
```

**Claude Code:**

```sh
claude mcp add --transport http studio https://<api-host>/api/v1/studios/<studio-id>/mcp \
  --header "Authorization: Bearer smpat_..."
```

**Another client:** set up a remote, Streamable HTTP server at that address, and send the token
as an `Authorization: Bearer` header. A client that only signs in to remote servers with OAuth
can't connect yet. OAuth sign-in is planned
([17](../planning/17-mcp-and-api-tokens-plan.md), step 5).

## 3. Ask

- "What's happening in the studio today?"
- "Which supplies are we low on, and is anything already on order?"
- "What maintenance is due on the kilns?"
- "How much of each material do I weigh out for 10 kg of the studio celadon?"

## Keeping it safe

- **Treat the token like a password.** Anyone who has it can read what you can read in this
  studio.
- **What a token can't reach:** the studio's money, member imports, the data export, webhooks
  and other tokens, whatever its scopes.
- **Members' text is just data.** Problem reports and notes are written by members. The
  server tells the assistant to treat that text as information and never as instructions, and
  no tool could act on such an instruction anyway.
