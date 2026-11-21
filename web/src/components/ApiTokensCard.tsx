import {
  createApiTokenMutation,
  linkMcpIdentityMutation,
  listApiTokensOptions,
  listApiTokensQueryKey,
  listMyIdentitiesOptions,
  listMyIdentitiesQueryKey,
  revokeApiTokenMutation,
  unlinkMyIdentityMutation,
  type ApiToken,
  type TokenScope,
} from '@studiomanager/api-client';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useId, useState } from 'react';

import { ErrorMessage } from './ErrorMessage';

const scopeChoices: { scope: TokenScope; label: string }[] = [
  { scope: 'read', label: 'Read everything a token can reach' },
  { scope: 'propose', label: 'Suggest changes for staff to approve (AI assistants)' },
  { scope: 'write:pieces', label: 'Change pieces' },
  { scope: 'write:kilns', label: 'Change kilns and firings' },
  { scope: 'write:maintenance', label: 'Change maintenance' },
  { scope: 'write:schedule', label: 'Change bookings, shifts and duties' },
  { scope: 'write:supplies', label: 'Change supplies and purchase requests' },
  { scope: 'write:glazes', label: 'Change the glaze book' },
  { scope: 'write:notices', label: 'Send notices' },
];

/**
 * Personal API tokens, for a script or an AI assistant to act as the signed-in staff member in
 * this studio. A token never reaches money, imports, the export, webhooks or other tokens.
 */
export function ApiTokensCard({ studioId }: { studioId: string }) {
  const id = useId();
  const queryClient = useQueryClient();
  const tokens = useQuery(listApiTokensOptions({ path: { studioId } }));
  const [name, setName] = useState('');
  const [scopes, setScopes] = useState<TokenScope[]>(['read']);
  const [days, setDays] = useState('90');
  const [secret, setSecret] = useState<string>();
  const refresh = () => queryClient.invalidateQueries({ queryKey: listApiTokensQueryKey({ path: { studioId } }) });
  const create = useMutation({
    ...createApiTokenMutation(),
    onSuccess: (created) => {
      setSecret(created.secret);
      setName('');
      void refresh();
    },
  });
  const revoke = useMutation({ ...revokeApiTokenMutation(), onSuccess: refresh });

  return (
    <section className="card stack" aria-labelledby={`${id}-title`}>
      <h2 id={`${id}-title`}>API access</h2>
      <p>
        A token lets a script or an AI assistant act as you, in this studio only, within what you allow. It stops working
        if you stop being staff. Tokens can never reach the studio&apos;s money, imports, the data export or webhooks.
      </p>
      {secret && (
        <div className="card stack" role="status">
          <p>Copy the token now: it won&apos;t be shown again.</p>
          <code>{secret}</code>
          <div>
            <button type="button" onClick={() => setSecret(undefined)}>
              Done
            </button>
          </div>
        </div>
      )}
      {tokens.isError && <ErrorMessage error={tokens.error} />}
      {tokens.data && tokens.data.length > 0 && (
        <table>
          <caption>Tokens</caption>
          <thead>
            <tr>
              <th scope="col">Name</th>
              <th scope="col">Can</th>
              <th scope="col">Status</th>
              <th scope="col" />
            </tr>
          </thead>
          <tbody>
            {tokens.data.map((t) => (
              <tr key={t.id}>
                <td>
                  {t.name} <span className="badge">…{t.lastFour}</span>
                  {t.ownerName && <div className="badge">{t.ownerName}</div>}
                </td>
                <td>{t.scopes.join(', ')}</td>
                <td>{status(t)}</td>
                <td>
                  {!t.revokedAt && new Date(t.expiresAt) > new Date() && (
                    <button
                      type="button"
                      className="danger"
                      disabled={revoke.isPending}
                      onClick={() => revoke.mutate({ path: { studioId, tokenId: t.id } })}
                    >
                      Revoke
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
      {revoke.isError && <ErrorMessage error={revoke.error} />}
      <form
        className="stack"
        onSubmit={(event) => {
          event.preventDefault();
          create.mutate({ path: { studioId }, body: { name: name.trim(), scopes, expiresInDays: Number(days) } });
        }}
      >
        <h3>New token</h3>
        <div className="field">
          <label htmlFor={`${id}-name`}>What will use it</label>
          <input
            id={`${id}-name`}
            required
            maxLength={80}
            placeholder="Claude desktop"
            value={name}
            onChange={(e) => setName(e.target.value)}
          />
        </div>
        <fieldset className="field">
          <legend>It may</legend>
          {scopeChoices.map((c) => (
            <label key={c.scope} className="choice">
              <input
                type="checkbox"
                checked={scopes.includes(c.scope)}
                onChange={(e) => setScopes(e.target.checked ? [...scopes, c.scope] : scopes.filter((s) => s !== c.scope))}
              />
              {c.label}
            </label>
          ))}
        </fieldset>
        <div className="field">
          <label htmlFor={`${id}-days`}>Expires after</label>
          <select id={`${id}-days`} value={days} onChange={(e) => setDays(e.target.value)}>
            <option value="7">7 days</option>
            <option value="30">30 days</option>
            <option value="90">90 days</option>
            <option value="365">1 year</option>
          </select>
        </div>
        {create.isError && <ErrorMessage error={create.error} />}
        <div>
          <button type="submit" className="primary" disabled={create.isPending || scopes.length === 0}>
            Make token
          </button>
        </div>
      </form>
      <LinkedMcpIdentities />
    </section>
  );
}

function LinkedMcpIdentities() {
  const id = useId();
  const queryClient = useQueryClient();
  const identities = useQuery(listMyIdentitiesOptions());
  const [mcpToken, setMcpToken] = useState('');
  const refresh = () => queryClient.invalidateQueries({ queryKey: listMyIdentitiesQueryKey() });
  const link = useMutation({
    ...linkMcpIdentityMutation(),
    onSuccess: () => {
      setMcpToken('');
      void refresh();
    },
  });
  const unlink = useMutation({ ...unlinkMyIdentityMutation(), onSuccess: refresh });

  return (
    <div className="stack">
      <h3>Linked MCP OAuth identities</h3>
      <p>
        When connecting an assistant over OAuth 2.1 instead of a personal token, link a signed MCP OAuth access token
        here so it acts as your account.
      </p>
      {identities.isError && <ErrorMessage error={identities.error} />}
      {identities.data && identities.data.length > 0 && (
        <table>
          <caption>Linked external identities</caption>
          <thead>
            <tr>
              <th scope="col">Subject</th>
              <th scope="col">Issuer</th>
              <th scope="col">Linked</th>
              <th scope="col" />
            </tr>
          </thead>
          <tbody>
            {identities.data.map((item) => (
              <tr key={item.id}>
                <td>
                  <code>{item.subject}</code>
                </td>
                <td>{item.issuer}</td>
                <td>{new Date(item.createdAt).toLocaleDateString()}</td>
                <td>
                  <button
                    type="button"
                    className="danger"
                    disabled={unlink.isPending}
                    onClick={() => unlink.mutate({ path: { identityId: item.id } })}
                  >
                    Unlink
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
      {unlink.isError && <ErrorMessage error={unlink.error} />}
      <form
        className="row"
        onSubmit={(event) => {
          event.preventDefault();
          link.mutate({ body: { token: mcpToken.trim() } });
        }}
      >
        <div className="field">
          <label htmlFor={`${id}-mcp-token`}>MCP OAuth access token</label>
          <input
            id={`${id}-mcp-token`}
            type="password"
            minLength={20}
            maxLength={8192}
            placeholder="Paste signed MCP JWT"
            value={mcpToken}
            onChange={(e) => setMcpToken(e.target.value)}
          />
        </div>
        <button type="submit" disabled={link.isPending || mcpToken.trim().length < 20}>
          Link MCP identity
        </button>
      </form>
      {link.isError && <ErrorMessage error={link.error} />}
    </div>
  );
}

function status(t: ApiToken): string {
  if (t.revokedAt) return 'Revoked';
  if (new Date(t.expiresAt) <= new Date()) return 'Expired';
  const used = t.lastUsedAt ? `last used ${new Date(t.lastUsedAt).toLocaleDateString()}` : 'never used';
  const from = t.lastUsedFrom ? `. Last used from ${t.lastUsedFrom}` : '';
  return `Until ${new Date(t.expiresAt).toLocaleDateString()}, ${used}${from}`;
}
