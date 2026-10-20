import {
  createCertificationTypeMutation,
  createInviteMutation,
  type Duty,
  getStudioOptions,
  grantCertificationMutation,
  listCertificationsOptions,
  listCertificationTypesOptions,
  listDutiesOptions,
  listMembersOptions,
  type Member,
  setDutiesMutation,
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
      <InviteForm studioId={studioId} myRole={studio.myRole} />
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

function InviteForm({ studioId, myRole }: { studioId: string; myRole: StudioRole }) {
  const id = useId();
  const [role, setRole] = useState<StudioRole>('MEMBER');
  const invite = useMutation(createInviteMutation());
  const roles: StudioRole[] = myRole === 'OWNER' ? ['MEMBER', 'STAFF', 'OWNER'] : ['MEMBER', 'STAFF'];
  const link = invite.data ? `${window.location.origin}/invites/${invite.data.code}` : undefined;

  return (
    <section className="card stack" aria-labelledby={`${id}-title`}>
      <h2 id={`${id}-title`}>Invite someone</h2>
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
