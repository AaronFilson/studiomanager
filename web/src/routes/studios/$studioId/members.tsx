import {
  createCertificationTypeMutation,
  createInviteMutation,
  type Duty,
  emailInvitesMutation,
  getStudioOptions,
  grantCertificationMutation,
  listCertificationsOptions,
  listCertificationTypesOptions,
  listDutiesOptions,
  listMembersOptions,
  listOpenInvitesOptions,
  listOpenInvitesQueryKey,
  type Member,
  resendInviteMutation,
  revokeInviteMutation,
  setDutiesMutation,
  type SkippedEmail,
  type StudioRole,
} from '@studiomanager/api-client';
import { useMutation, useQuery, useQueryClient, useSuspenseQuery } from '@tanstack/react-query';
import { createFileRoute } from '@tanstack/react-router';
import { useId, useState } from 'react';

import { ErrorMessage } from '../../../components/ErrorMessage';
import { dutyLabel, formatDate, formatDateTime, roleLabel } from '../../../labels';

export const Route = createFileRoute('/studios/$studioId/members')({
  component: MembersPage,
});

const allDuties: Duty[] = ['TECHNICIAN', 'MONITOR', 'WORK_EXCHANGE', 'VOLUNTEER_OFFICER', 'INSTRUCTOR'];

function MembersPage() {
  const { studioId } = Route.useParams();
  const { data: studio } = useSuspenseQuery(getStudioOptions({ path: { studioId } }));
  const members = useQuery(listMembersOptions({ path: { studioId } }));

  return (
    <div className="stack">
      <h1>Members</h1>
      <EmailInviteForm studioId={studioId} myRole={studio.myRole} />
      <InviteForm studioId={studioId} myRole={studio.myRole} />
      <OpenInvites studioId={studioId} />
      {members.isPending && <p>Loading members…</p>}
      {members.isError && <ErrorMessage error={members.error} />}
      {members.isSuccess && (
        <ul className="studio-list">
          {members.data.map((member) => (
            <MemberRow key={member.accountId} studioId={studioId} member={member} />
          ))}
        </ul>
      )}
      <CertificationTypeForm studioId={studioId} />
    </div>
  );
}

function MemberRow({ studioId, member }: { studioId: string; member: Member }) {
  const queryClient = useQueryClient();
  const duties = useQuery(listDutiesOptions({ path: { studioId } }));
  const types = useQuery(listCertificationTypesOptions({ path: { studioId } }));
  const certifications = useQuery(listCertificationsOptions({ path: { studioId } }));
  const onSuccess = () => queryClient.invalidateQueries();
  const setDuties = useMutation({ ...setDutiesMutation(), onSuccess });
  const grant = useMutation({ ...grantCertificationMutation(), onSuccess });
  const mine = new Set(duties.data?.find((d) => d.accountId === member.accountId)?.duties ?? []);
  const held = certifications.data?.filter((c) => c.accountId === member.accountId) ?? [];

  function toggle(duty: Duty) {
    const next = new Set(mine);
    if (next.has(duty)) next.delete(duty);
    else next.add(duty);
    setDuties.mutate({ path: { studioId, accountId: member.accountId }, body: { duties: [...next] } });
  }

  return (
    <li>
      <div className="stack">
        <div>
          <strong>{member.displayName ?? 'Unnamed member'}</strong>{' '}
          <span className="badge">
            {roleLabel(member.role)} · joined {formatDate(member.joinedAt)}
          </span>
        </div>
        <div className="row" role="group" aria-label={`${member.displayName ?? 'Member'}'s duties`}>
          {allDuties.map((duty) => (
            <label key={duty} className="choice">
              <input type="checkbox" checked={mine.has(duty)} onChange={() => toggle(duty)} />
              {dutyLabel(duty)}
            </label>
          ))}
        </div>
        <div className="row">
          {held.map((c) => (
            <span key={c.typeId} className={c.valid ? 'badge' : 'badge warning'}>
              {c.name}
              {c.expiresAt ? ` (${c.valid ? 'until' : 'expired'} ${formatDate(c.expiresAt)})` : ''}
            </span>
          ))}
          {types.data
            ?.filter((t) => !held.some((c) => c.typeId === t.id && c.valid))
            .map((t) => (
              <button
                key={t.id}
                type="button"
                onClick={() => grant.mutate({ path: { studioId, typeId: t.id, accountId: member.accountId } })}
              >
                Certify: {t.name}
              </button>
            ))}
        </div>
        {(setDuties.error ?? grant.error) && <ErrorMessage error={setDuties.error ?? grant.error} />}
      </div>
    </li>
  );
}

function CertificationTypeForm({ studioId }: { studioId: string }) {
  const id = useId();
  const queryClient = useQueryClient();
  const [name, setName] = useState('');
  const [months, setMonths] = useState('');
  const create = useMutation({
    ...createCertificationTypeMutation(),
    onSuccess: async () => {
      setName('');
      setMonths('');
      await queryClient.invalidateQueries();
    },
  });

  return (
    <form
      className="card stack"
      aria-labelledby={`${id}-title`}
      onSubmit={(event) => {
        event.preventDefault();
        create.mutate({ path: { studioId }, body: { name: name.trim(), ...(months ? { validMonths: Number(months) } : {}) } });
      }}
    >
      <h2 id={`${id}-title`}>Add a certification</h2>
      <p className="badge">For sign-offs such as a wheel room orientation or kiln loading. Equipment can require one.</p>
      <div className="row">
        <div className="field">
          <label htmlFor={`${id}-name`}>Name</label>
          <input id={`${id}-name`} required maxLength={60} value={name} onChange={(e) => setName(e.target.value)} />
        </div>
        <div className="field">
          <label htmlFor={`${id}-months`}>Renew every … months (optional)</label>
          <input id={`${id}-months`} type="number" min={1} max={120} value={months} onChange={(e) => setMonths(e.target.value)} />
        </div>
      </div>
      {create.isError && <ErrorMessage error={create.error} />}
      <div>
        <button type="submit" className="primary" disabled={create.isPending}>
          Add certification
        </button>
      </div>
    </form>
  );
}

function rolesFor(myRole: StudioRole): StudioRole[] {
  return myRole === 'OWNER' ? ['MEMBER', 'STAFF', 'OWNER'] : ['MEMBER', 'STAFF'];
}

const skipReasons: Record<SkippedEmail['reason'], string> = {
  ALREADY_MEMBER: 'already a member',
  ALREADY_INVITED: 'already invited; resend it below',
};

function EmailInviteForm({ studioId, myRole }: { studioId: string; myRole: StudioRole }) {
  const id = useId();
  const queryClient = useQueryClient();
  const [text, setText] = useState('');
  const [role, setRole] = useState<StudioRole>('MEMBER');
  const send = useMutation({
    ...emailInvitesMutation(),
    onSuccess: async () => {
      setText('');
      await queryClient.invalidateQueries({ queryKey: listOpenInvitesQueryKey({ path: { studioId } }) });
    },
  });

  return (
    <form
      className="card stack"
      aria-labelledby={`${id}-title`}
      onSubmit={(event) => {
        event.preventDefault();
        send.mutate({ path: { studioId }, body: { text, role } });
      }}
    >
      <h2 id={`${id}-title`}>Invite by email</h2>
      <div className="field">
        <label htmlFor={`${id}-text`}>Email addresses</label>
        <textarea
          id={`${id}-text`}
          required
          rows={5}
          maxLength={20000}
          value={text}
          onChange={(e) => setText(e.target.value)}
          aria-describedby={`${id}-hint`}
        />
        <p id={`${id}-hint`} className="badge">
          Paste a column from a spreadsheet or a list from an email. Up to 200 at once; each gets their own link.
        </p>
      </div>
      <div className="field">
        <label htmlFor={`${id}-role`}>As</label>
        <select id={`${id}-role`} value={role} onChange={(e) => setRole(e.target.value as StudioRole)}>
          {rolesFor(myRole).map((r) => (
            <option key={r} value={r}>
              {roleLabel(r)}
            </option>
          ))}
        </select>
      </div>
      {send.isError && <ErrorMessage error={send.error} />}
      {send.data && (
        <div className="stack" aria-live="polite">
          <p>
            Sending {send.data.invited.length} {send.data.invited.length === 1 ? 'invite' : 'invites'}.
          </p>
          {send.data.skipped.length > 0 && (
            <ul>
              {send.data.skipped.map((s) => (
                <li key={s.email}>
                  Skipped {s.email}: {skipReasons[s.reason]}
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
      <div>
        <button type="submit" className="primary" disabled={send.isPending}>
          Send invites
        </button>
      </div>
    </form>
  );
}

function OpenInvites({ studioId }: { studioId: string }) {
  const id = useId();
  const queryClient = useQueryClient();
  const invites = useQuery(listOpenInvitesOptions({ path: { studioId } }));
  const onSuccess = () => queryClient.invalidateQueries({ queryKey: listOpenInvitesQueryKey({ path: { studioId } }) });
  const resend = useMutation({ ...resendInviteMutation(), onSuccess });
  const revoke = useMutation({ ...revokeInviteMutation(), onSuccess });

  if (!invites.data?.length) return null;
  return (
    <section className="card stack" aria-labelledby={`${id}-title`}>
      <h2 id={`${id}-title`}>Waiting to join</h2>
      <ul className="studio-list">
        {invites.data.map((invite) => (
          <li key={invite.id} className="row">
            <span>
              <strong>{invite.email ?? 'Invite link'}</strong>{' '}
              <span className={invite.expired ? 'badge warning' : 'badge'}>
                {roleLabel(invite.role)} · {invite.expired ? 'expired' : 'until'} {formatDate(invite.expiresAt)}
                {invite.email && !invite.lastSentAt ? ' · sending' : ''}
              </span>
            </span>
            {invite.email && (
              <button type="button" onClick={() => resend.mutate({ path: { studioId, inviteId: invite.id } })}>
                Resend
              </button>
            )}
            <button type="button" onClick={() => revoke.mutate({ path: { studioId, inviteId: invite.id } })}>
              Withdraw
            </button>
          </li>
        ))}
      </ul>
      {(resend.error ?? revoke.error) && <ErrorMessage error={resend.error ?? revoke.error} />}
    </section>
  );
}

function InviteForm({ studioId, myRole }: { studioId: string; myRole: StudioRole }) {
  const id = useId();
  const queryClient = useQueryClient();
  const [role, setRole] = useState<StudioRole>('MEMBER');
  const invite = useMutation({
    ...createInviteMutation(),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: listOpenInvitesQueryKey({ path: { studioId } }) }),
  });
  const roles = rolesFor(myRole);
  const link = invite.data ? `${window.location.origin}/invites/${invite.data.code}` : undefined;

  return (
    <section className="card stack" aria-labelledby={`${id}-title`}>
      <h2 id={`${id}-title`}>Invite with a link</h2>
      <div className="field">
        <label htmlFor={`${id}-role`}>As</label>
        <select id={`${id}-role`} value={role} onChange={(e) => setRole(e.target.value as StudioRole)}>
          {roles.map((r) => (
            <option key={r} value={r}>
              {roleLabel(r)}
            </option>
          ))}
        </select>
      </div>
      {invite.isError && <ErrorMessage error={invite.error} />}
      {link && invite.data && (
        <div className="stack" aria-live="polite">
          <p>
            Send this link to the person you&apos;re inviting. It works once, until{' '}
            {formatDateTime(invite.data.expiresAt)}.
          </p>
          <input readOnly value={link} aria-label="Invite link" onFocus={(e) => e.target.select()} />
        </div>
      )}
      <div>
        <button
          type="button"
          className="primary"
          disabled={invite.isPending}
          onClick={() => invite.mutate({ path: { studioId }, body: { role } })}
        >
          Create an invite link
        </button>
      </div>
    </section>
  );
}
