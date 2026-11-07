import {
  addOwnerMutation,
  createCertificationTypeMutation,
  createInviteMutation,
  type Duty,
  emailInvitesMutation,
  getBillingContactOptions,
  getBillingContactQueryKey,
  getMeOptions,
  grantCertificationMutation,
  listCertificationsOptions,
  listCertificationTypesOptions,
  listAssignableRolesOptions,
  listDutiesOptions,
  listMembersOptions,
  listOpenInvitesOptions,
  listOpenInvitesQueryKey,
  type Member,
  removeOwnerMutation,
  resendInviteMutation,
  revokeInviteMutation,
  type Role,
  setBillingContactMutation,
  setDutiesMutation,
  setMemberRoleMutation,
  type SkippedEmail,
  type StudioRole,
  useCan,
} from '@studiomanager/api-client';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
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
  const { can } = useCan(studioId);
  const members = useQuery(listMembersOptions({ path: { studioId } }));
  const managesOwners = can('owners.manage');
  // Built-in and custom roles the caller may give: none that holds more than they do
  const assignable = useQuery({
    ...listAssignableRolesOptions({ path: { studioId } }),
    enabled: can('members.invite'),
  });
  const roles = assignable.data ?? [];

  return (
    <div className="stack">
      <h1>Members</h1>
      {managesOwners && members.isSuccess && <BillingContactCard studioId={studioId} members={members.data} />}
      <EmailInviteForm studioId={studioId} roles={roles} />
      <InviteForm studioId={studioId} roles={roles} />
      <OpenInvites studioId={studioId} />
      {members.isPending && <p>Loading members…</p>}
      {members.isError && <ErrorMessage error={members.error} />}
      {members.isSuccess && (
        <ul className="studio-list">
          {members.data.map((member) => (
            <MemberRow
              key={member.accountId}
              studioId={studioId}
              member={member}
              managesOwners={managesOwners}
              ownerCount={members.data.filter((m) => m.role === 'OWNER').length}
              roles={can('members.role.assign') ? roles : []}
            />
          ))}
        </ul>
      )}
      <CertificationTypeForm studioId={studioId} />
    </div>
  );
}

/**
 * The one owner the platform bills. When that person stops being an owner the studio has none, and
 * the remaining owners are asked here to pick another.
 */
function BillingContactCard({ studioId, members }: { studioId: string; members: Member[] }) {
  const id = useId();
  const queryClient = useQueryClient();
  const contact = useQuery(getBillingContactOptions({ path: { studioId } }));
  const choose = useMutation({
    ...setBillingContactMutation(),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: getBillingContactQueryKey({ path: { studioId } }) }),
  });
  const owners = members.filter((m) => m.role === 'OWNER');

  return (
    <section className="card stack" aria-labelledby={`${id}-title`}>
      <h2 id={`${id}-title`}>Billing contact</h2>
      {contact.isError && <ErrorMessage error={contact.error} />}
      {contact.data?.chooseNeeded && (
        <p className="badge warning" role="status">
          The billing contact is no longer an owner. Pick one of the owners; until then, billing notices go to every
          owner.
        </p>
      )}
      {contact.isSuccess && (
        <div className="field">
          <label htmlFor={`${id}-owner`}>Owner the studio is billed through</label>
          <select
            id={`${id}-owner`}
            value={contact.data.accountId ?? ''}
            disabled={choose.isPending}
            onChange={(e) => {
              if (e.target.value) choose.mutate({ path: { studioId }, body: { accountId: e.target.value } });
            }}
          >
            {!contact.data.accountId && <option value="">Choose an owner</option>}
            {owners.map((owner) => (
              <option key={owner.accountId} value={owner.accountId}>
                {owner.displayName ?? 'Unnamed owner'}
              </option>
            ))}
          </select>
        </div>
      )}
      {choose.isError && <ErrorMessage error={choose.error} />}
    </section>
  );
}

/** Make a member an owner, or an owner staff; refused for the last owner, so not offered. */
function OwnerControls({ studioId, member, ownerCount }: { studioId: string; member: Member; ownerCount: number }) {
  const queryClient = useQueryClient();
  const me = useQuery(getMeOptions());
  const onSuccess = () => queryClient.invalidateQueries();
  const add = useMutation({ ...addOwnerMutation(), onSuccess });
  const remove = useMutation({ ...removeOwnerMutation(), onSuccess });
  const name = member.displayName ?? 'this member';
  const isMe = me.data?.account.id === member.accountId;
  const path = { studioId, accountId: member.accountId };

  return (
    <div className="row">
      {member.role !== 'OWNER' && (
        <button type="button" disabled={add.isPending} onClick={() => add.mutate({ path })}>
          Make {name} an owner
        </button>
      )}
      {member.role === 'OWNER' && ownerCount > 1 && (
        <button type="button" disabled={remove.isPending} onClick={() => remove.mutate({ path })}>
          {isMe ? 'Step down as owner' : `Remove ${name} as owner`}
        </button>
      )}
      {member.role === 'OWNER' && ownerCount <= 1 && (
        <span className="badge">The only owner. Add another owner before they can step down.</span>
      )}
      {(add.error ?? remove.error) && <ErrorMessage error={add.error ?? remove.error} />}
    </div>
  );
}

/**
 * Choose a member's role among the built-in and custom roles the caller may give. Owners are made
 * and unmade with the owner controls, and a member whose role the caller couldn't give isn't
 * offered a choice.
 */
function RolePicker({ studioId, member, roles }: { studioId: string; member: Member; roles: Role[] }) {
  const id = useId();
  const queryClient = useQueryClient();
  const assign = useMutation({ ...setMemberRoleMutation(), onSuccess: () => queryClient.invalidateQueries() });
  const choices = roles.filter((role) => role.role !== 'OWNER');
  if (member.role === 'OWNER' || !choices.some((role) => role.id === member.roleId)) return null;

  return (
    <div className="field">
      <label htmlFor={`${id}-role`}>{member.displayName ?? 'Member'}&apos;s role</label>
      <select
        id={`${id}-role`}
        value={member.roleId}
        disabled={assign.isPending}
        onChange={(e) =>
          assign.mutate({ path: { studioId, accountId: member.accountId }, body: { roleId: e.target.value } })
        }
      >
        {choices.map((role) => (
          <option key={role.id} value={role.id}>
            {role.name}
          </option>
        ))}
      </select>
      {assign.isError && <ErrorMessage error={assign.error} />}
    </div>
  );
}

function MemberRow({
  studioId,
  member,
  managesOwners,
  ownerCount,
  roles,
}: {
  studioId: string;
  member: Member;
  managesOwners: boolean;
  ownerCount: number;
  roles: Role[];
}) {
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
            {member.roleName} · joined {formatDate(member.joinedAt)}
          </span>
        </div>
        {managesOwners && <OwnerControls studioId={studioId} member={member} ownerCount={ownerCount} />}
        <RolePicker studioId={studioId} member={member} roles={roles} />
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

/**
 * What an invite form sends for the chosen role: a built-in role by name, a custom role by id with
 * its base. Before the roles load, or with none chosen, the built-in member role.
 */
function inviteRole(roles: Role[], chosenId: string): { role: StudioRole; roleId?: string } {
  const chosen = roles.find((role) => role.id === chosenId) ?? roles.find((role) => role.key === 'member');
  if (!chosen) return { role: 'MEMBER' };
  return chosen.custom ? { role: chosen.role, roleId: chosen.id } : { role: chosen.role };
}

/** The role options, the built-in member role first chosen. */
function RoleSelect({
  id,
  roles,
  value,
  onChange,
}: {
  id: string;
  roles: Role[];
  value: string;
  onChange: (roleId: string) => void;
}) {
  const selected = value || (roles.find((role) => role.key === 'member')?.id ?? '');
  return (
    <select id={id} value={selected} onChange={(e) => onChange(e.target.value)}>
      {roles.map((role) => (
        <option key={role.id} value={role.id}>
          {role.name}
        </option>
      ))}
    </select>
  );
}

const skipReasons: Record<SkippedEmail['reason'], string> = {
  ALREADY_MEMBER: 'already a member',
  ALREADY_INVITED: 'already invited; resend it below',
};

function EmailInviteForm({ studioId, roles }: { studioId: string; roles: Role[] }) {
  const id = useId();
  const queryClient = useQueryClient();
  const [text, setText] = useState('');
  const [roleId, setRoleId] = useState('');
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
        send.mutate({ path: { studioId }, body: { text, ...inviteRole(roles, roleId) } });
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
        <RoleSelect id={`${id}-role`} roles={roles} value={roleId} onChange={setRoleId} />
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
                {invite.roleName ?? roleLabel(invite.role)} · {invite.expired ? 'expired' : 'until'}{' '}
                {formatDate(invite.expiresAt)}
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

function InviteForm({ studioId, roles }: { studioId: string; roles: Role[] }) {
  const id = useId();
  const queryClient = useQueryClient();
  const [roleId, setRoleId] = useState('');
  const invite = useMutation({
    ...createInviteMutation(),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: listOpenInvitesQueryKey({ path: { studioId } }) }),
  });
  const link = invite.data ? `${window.location.origin}/invites/${invite.data.code}` : undefined;

  return (
    <section className="card stack" aria-labelledby={`${id}-title`}>
      <h2 id={`${id}-title`}>Invite with a link</h2>
      <div className="field">
        <label htmlFor={`${id}-role`}>As</label>
        <RoleSelect id={`${id}-role`} roles={roles} value={roleId} onChange={setRoleId} />
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
          onClick={() => invite.mutate({ path: { studioId }, body: inviteRole(roles, roleId) })}
        >
          Create an invite link
        </button>
      </div>
    </section>
  );
}
