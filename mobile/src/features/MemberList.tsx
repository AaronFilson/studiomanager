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
  listAssignableRolesOptions,
  listCertificationsOptions,
  listCertificationTypesOptions,
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
import { useState } from 'react';
import { Pressable, RefreshControl, ScrollView, Share, StyleSheet, View } from 'react-native';

import { allDuties, dutyLabel, formatDate, formatDateTime, roleLabel } from '../labels';
import { Badge, Body, Button, Card, ErrorText, Field, Loading, SectionTitle, Title } from '../ui';

const skipReasons: Record<SkippedEmail['reason'], string> = {
  ALREADY_MEMBER: 'already a member',
  ALREADY_INVITED: 'already invited; resend it below',
};

function inviteRole(roles: Role[], chosenId: string): { role: StudioRole; roleId?: string } {
  const chosen = roles.find((role) => role.id === chosenId) ?? roles.find((role) => role.key === 'member');
  if (!chosen) return { role: 'MEMBER' };
  return chosen.custom ? { role: chosen.role, roleId: chosen.id } : { role: chosen.role };
}

/**
 * Full studio membership management: billing contact, co-owners, role assignment, duties,
 * equipment certifications, invite links, bulk email invites, and open invite management.
 */
export function MemberList({ studioId }: { studioId: string }) {
  const permissions = useCan(studioId);
  const { can } = permissions;
  const managesOwners = can('owners.manage');
  const canInvite = can('members.invite');
  const canAssignRole = can('members.role.assign');
  const canManageDuties = can('members.duties.manage');
  const canCertify = can('members.certify');

  const members = useQuery(listMembersOptions({ path: { studioId } }));
  const duties = useQuery({
    ...listDutiesOptions({ path: { studioId } }),
    enabled: canManageDuties,
  });
  const certTypes = useQuery(listCertificationTypesOptions({ path: { studioId } }));
  const certifications = useQuery(listCertificationsOptions({ path: { studioId } }));
  const assignable = useQuery({
    ...listAssignableRolesOptions({ path: { studioId } }),
    enabled: canInvite,
  });
  const roles = assignable.data ?? [];

  if (members.isPending || permissions.isPending) return <Loading />;
  if (members.isError) return <ErrorText error={members.error} />;
  if (permissions.isError) return <ErrorText error={permissions.error} />;

  const ownerCount = members.data.filter((m) => m.role === 'OWNER').length;

  return (
    <ScrollView
      contentContainerStyle={styles.content}
      keyboardShouldPersistTaps="handled"
      refreshControl={
        <RefreshControl
          refreshing={members.isRefetching}
          onRefresh={() => {
            void members.refetch();
            if (canManageDuties) void duties.refetch();
            void certifications.refetch();
          }}
        />
      }
    >
      <Title>Members</Title>

      {managesOwners && <BillingContactCard studioId={studioId} members={members.data} />}

      {canInvite && (
        <>
          <InviteLinkCard studioId={studioId} roles={roles} />
          <EmailInviteCard studioId={studioId} roles={roles} />
          <OpenInvitesCard studioId={studioId} />
        </>
      )}

      {canManageDuties && duties.isError && <ErrorText error={duties.error} />}
      {certifications.isError && <ErrorText error={certifications.error} />}

      <View style={styles.section}>
        <SectionTitle>Studio members ({String(members.data.length)})</SectionTitle>
        {members.data.map((member) => (
          <MemberCard
            key={member.accountId}
            studioId={studioId}
            member={member}
            managesOwners={managesOwners}
            canManageDuties={canManageDuties}
            canCertify={canCertify}
            ownerCount={ownerCount}
            roles={canAssignRole ? roles : []}
            memberDuties={duties.data?.find((d) => d.accountId === member.accountId)?.duties ?? []}
            certTypes={certTypes.data ?? []}
            heldCerts={certifications.data?.filter((c) => c.accountId === member.accountId) ?? []}
          />
        ))}
      </View>

      {canCertify && <CertificationTypeCard studioId={studioId} />}
    </ScrollView>
  );
}

function BillingContactCard({ studioId, members }: { studioId: string; members: Member[] }) {
  const queryClient = useQueryClient();
  const contact = useQuery(getBillingContactOptions({ path: { studioId } }));
  const choose = useMutation({
    ...setBillingContactMutation(),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: getBillingContactQueryKey({ path: { studioId } }) }),
  });
  const owners = members.filter((m) => m.role === 'OWNER');

  return (
    <Card>
      <SectionTitle>Billing contact</SectionTitle>
      {contact.isPending && <Loading />}
      {contact.isError && <ErrorText error={contact.error} />}
      {contact.data?.chooseNeeded && (
        <Badge warning>
          The billing contact is no longer an owner. Pick one of the owners; until then, billing notices go to every owner.
        </Badge>
      )}
      {contact.isSuccess && (
        <View style={styles.group}>
          <Body muted>Owner the studio is billed through</Body>
          <View style={styles.rowWrap}>
            {owners.map((owner) => {
              const selected = contact.data.accountId === owner.accountId;
              return (
                <Button
                  key={owner.accountId}
                  compact
                  primary={selected}
                  disabled={choose.isPending || selected}
                  label={owner.displayName ?? 'Unnamed owner'}
                  onPress={() => choose.mutate({ path: { studioId }, body: { accountId: owner.accountId } })}
                />
              );
            })}
          </View>
        </View>
      )}
      {choose.isError && <ErrorText error={choose.error} />}
    </Card>
  );
}

function RoleButtons({
  roles,
  selectedId,
  onSelect,
  disabled = false,
}: {
  roles: Role[];
  selectedId: string;
  onSelect: (roleId: string) => void;
  disabled?: boolean;
}) {
  const effective = selectedId || (roles.find((r) => r.key === 'member')?.id ?? '');
  return (
    <View style={styles.rowWrap}>
      {roles.map((role) => (
        <Button
          key={role.id}
          compact
          primary={effective === role.id}
          disabled={disabled}
          label={role.name}
          onPress={() => onSelect(role.id)}
        />
      ))}
    </View>
  );
}

function InviteLinkCard({ studioId, roles }: { studioId: string; roles: Role[] }) {
  const queryClient = useQueryClient();
  const [roleId, setRoleId] = useState('');
  const invite = useMutation({
    ...createInviteMutation(),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: listOpenInvitesQueryKey({ path: { studioId } }) }),
  });

  function shareInvite(code: string) {
    void Share.share({
      message: `Join our studio on Studio Manager. Invite code: ${code} (studiomanager://invites/${code})`,
    }).catch(() => {});
  }

  const createdInvite = invite.data;

  return (
    <Card>
      <SectionTitle>Invite with a link</SectionTitle>
      {roles.length > 0 && (
        <View style={styles.group}>
          <Body muted>Role</Body>
          <RoleButtons roles={roles} selectedId={roleId} onSelect={setRoleId} disabled={invite.isPending} />
        </View>
      )}
      {invite.isError && <ErrorText error={invite.error} />}
      {createdInvite && (
        <View style={styles.group}>
          <Body>
            Share this code with the person you&apos;re inviting. It works once, until{' '}
            {formatDateTime(createdInvite.expiresAt)}.
          </Body>
          <Body selectable>{createdInvite.code}</Body>
          <Button label="Share invite" onPress={() => shareInvite(createdInvite.code)} />
        </View>
      )}
      <Button
        primary
        label="Create an invite link"
        disabled={invite.isPending}
        onPress={() => invite.mutate({ path: { studioId }, body: inviteRole(roles, roleId) })}
      />
    </Card>
  );
}

function EmailInviteCard({ studioId, roles }: { studioId: string; roles: Role[] }) {
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
    <Card>
      <SectionTitle>Invite by email</SectionTitle>
      <Field
        label="Email addresses"
        value={text}
        onChangeText={setText}
        multiline
        autoCapitalize="none"
        autoCorrect={false}
        maxLength={20000}
        placeholder="name@example.com, another@example.com"
      />
      <Body muted>Separate addresses with commas, spaces or new lines. Up to 200 at once.</Body>
      {roles.length > 0 && (
        <View style={styles.group}>
          <Body muted>Role</Body>
          <RoleButtons roles={roles} selectedId={roleId} onSelect={setRoleId} disabled={send.isPending} />
        </View>
      )}
      {send.isError && <ErrorText error={send.error} />}
      {send.data && (
        <View style={styles.group}>
          <Body>
            Sending {String(send.data.invited.length)} {send.data.invited.length === 1 ? 'invite' : 'invites'}.
          </Body>
          {send.data.skipped.map((s) => (
            <Body key={s.email} muted>
              Skipped {s.email}: {skipReasons[s.reason]}
            </Body>
          ))}
        </View>
      )}
      <Button
        primary
        label="Send invites"
        disabled={send.isPending || !text.trim()}
        onPress={() => send.mutate({ path: { studioId }, body: { text, ...inviteRole(roles, roleId) } })}
      />
    </Card>
  );
}

function OpenInvitesCard({ studioId }: { studioId: string }) {
  const queryClient = useQueryClient();
  const invites = useQuery(listOpenInvitesOptions({ path: { studioId } }));
  const onSuccess = () => queryClient.invalidateQueries({ queryKey: listOpenInvitesQueryKey({ path: { studioId } }) });
  const resend = useMutation({ ...resendInviteMutation(), onSuccess });
  const revoke = useMutation({ ...revokeInviteMutation(), onSuccess });

  if (invites.isError) return <ErrorText error={invites.error} />;
  if (!invites.data?.length) return null;

  return (
    <Card>
      <SectionTitle>Waiting to join</SectionTitle>
      {invites.data.map((invite) => (
        <View key={invite.id} style={styles.inviteRow}>
          <Body>{invite.email ?? 'Invite link'}</Body>
          <Badge warning={invite.expired}>
            {invite.roleName ?? roleLabel(invite.role)} · {invite.expired ? 'expired' : 'until'}{' '}
            {formatDate(invite.expiresAt)}
            {invite.email && !invite.lastSentAt ? ' · sending' : ''}
          </Badge>
          <View style={styles.rowWrap}>
            {invite.email && (
              <Button
                compact
                label="Resend"
                disabled={resend.isPending}
                onPress={() => resend.mutate({ path: { studioId, inviteId: invite.id } })}
              />
            )}
            <Button
              compact
              label="Withdraw"
              disabled={revoke.isPending}
              onPress={() => revoke.mutate({ path: { studioId, inviteId: invite.id } })}
            />
          </View>
        </View>
      ))}
      {(resend.error ?? revoke.error) && <ErrorText error={resend.error ?? revoke.error} />}
    </Card>
  );
}

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
    <View style={styles.group}>
      {member.role !== 'OWNER' && (
        <Button compact label={`Make ${name} an owner`} disabled={add.isPending} onPress={() => add.mutate({ path })} />
      )}
      {member.role === 'OWNER' && ownerCount > 1 && (
        <Button
          compact
          label={isMe ? 'Step down as owner' : `Remove ${name} as owner`}
          disabled={remove.isPending}
          onPress={() => remove.mutate({ path })}
        />
      )}
      {member.role === 'OWNER' && ownerCount <= 1 && (
        <Badge>The only owner. Add another owner before they can step down.</Badge>
      )}
      {(add.error ?? remove.error) && <ErrorText error={add.error ?? remove.error} />}
    </View>
  );
}

function RolePicker({ studioId, member, roles }: { studioId: string; member: Member; roles: Role[] }) {
  const queryClient = useQueryClient();
  const assign = useMutation({
    ...setMemberRoleMutation(),
    onSuccess: () => queryClient.invalidateQueries(),
  });
  const choices = roles.filter((role) => role.role !== 'OWNER');
  if (member.role === 'OWNER' || !choices.some((role) => role.id === member.roleId)) return null;

  return (
    <View style={styles.group}>
      <Body muted>{member.displayName ?? 'Member'}&apos;s role</Body>
      <View style={styles.rowWrap}>
        {choices.map((role) => {
          const current = role.id === member.roleId;
          return (
            <Button
              key={role.id}
              compact
              primary={current}
              disabled={assign.isPending || current}
              label={role.name}
              onPress={() =>
                assign.mutate({
                  path: { studioId, accountId: member.accountId },
                  body: { roleId: role.id },
                })
              }
            />
          );
        })}
      </View>
      {assign.isError && <ErrorText error={assign.error} />}
    </View>
  );
}

function MemberCard({
  studioId,
  member,
  managesOwners,
  canManageDuties,
  canCertify,
  ownerCount,
  roles,
  memberDuties,
  certTypes,
  heldCerts,
}: {
  studioId: string;
  member: Member;
  managesOwners: boolean;
  canManageDuties: boolean;
  canCertify: boolean;
  ownerCount: number;
  roles: Role[];
  memberDuties: Duty[];
  certTypes: { id: string; name: string }[];
  heldCerts: { typeId: string; name: string; expiresAt?: string; valid: boolean }[];
}) {
  const queryClient = useQueryClient();
  const onSuccess = () => queryClient.invalidateQueries();
  const setDuties = useMutation({ ...setDutiesMutation(), onSuccess });
  const grant = useMutation({ ...grantCertificationMutation(), onSuccess });
  const mine = new Set(memberDuties);
  const name = member.displayName ?? 'Unnamed member';

  function toggleDuty(duty: Duty) {
    const next = new Set(mine);
    if (next.has(duty)) next.delete(duty);
    else next.add(duty);
    setDuties.mutate({ path: { studioId, accountId: member.accountId }, body: { duties: [...next] } });
  }

  return (
    <Card>
      <SectionTitle>{name}</SectionTitle>
      <Badge>
        {member.roleName} · joined {formatDate(member.joinedAt)}
      </Badge>

      {managesOwners && <OwnerControls studioId={studioId} member={member} ownerCount={ownerCount} />}
      <RolePicker studioId={studioId} member={member} roles={roles} />

      {canManageDuties && (
        <View style={styles.group}>
          <Body muted>Duties</Body>
          <View style={styles.rowWrap}>
            {allDuties.map((duty) => {
              const checked = mine.has(duty);
              return (
                <Pressable
                  key={duty}
                  accessibilityRole="checkbox"
                  accessibilityState={{ checked }}
                  accessibilityLabel={`${name}: ${dutyLabel(duty)}`}
                  disabled={setDuties.isPending}
                  onPress={() => toggleDuty(duty)}
                  style={styles.checkbox}
                >
                  <Body>
                    {checked ? '☑' : '☐'} {dutyLabel(duty)}
                  </Body>
                </Pressable>
              );
            })}
          </View>
        </View>
      )}

      {(heldCerts.length > 0 || (canCertify && certTypes.length > 0)) && (
        <View style={styles.group}>
          <Body muted>Certifications</Body>
          <View style={styles.rowWrap}>
            {heldCerts.map((c) => (
              <Badge key={c.typeId} warning={!c.valid}>
                {c.name}
                {c.expiresAt ? ` (${c.valid ? 'until' : 'expired'} ${formatDate(c.expiresAt)})` : ''}
              </Badge>
            ))}
            {canCertify &&
              certTypes
                .filter((t) => !heldCerts.some((c) => c.typeId === t.id && c.valid))
                .map((t) => (
                  <Button
                    key={t.id}
                    compact
                    disabled={grant.isPending}
                    label={`Certify: ${t.name}`}
                    onPress={() => grant.mutate({ path: { studioId, typeId: t.id, accountId: member.accountId } })}
                  />
                ))}
          </View>
        </View>
      )}

      {(setDuties.error ?? grant.error) && <ErrorText error={setDuties.error ?? grant.error} />}
    </Card>
  );
}

function CertificationTypeCard({ studioId }: { studioId: string }) {
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
  const parsedMonths = months.trim() ? Number(months.trim()) : undefined;
  const monthsValid = parsedMonths === undefined || (Number.isInteger(parsedMonths) && parsedMonths >= 1 && parsedMonths <= 120);

  return (
    <Card>
      <SectionTitle>Add a certification</SectionTitle>
      <Body muted>For sign-offs such as a wheel room orientation or kiln loading.</Body>
      <Field label="Certification name" value={name} onChangeText={setName} maxLength={60} />
      <Field
        label="Renew every … months (optional)"
        value={months}
        onChangeText={setMonths}
        keyboardType="number-pad"
      />
      {!monthsValid && <Body>Enter whole months between 1 and 120, or leave blank.</Body>}
      {create.isError && <ErrorText error={create.error} />}
      <Button
        primary
        label="Add certification"
        disabled={create.isPending || !name.trim() || !monthsValid}
        onPress={() =>
          create.mutate({
            path: { studioId },
            body: { name: name.trim(), ...(parsedMonths !== undefined ? { validMonths: parsedMonths } : {}) },
          })
        }
      />
    </Card>
  );
}

const styles = StyleSheet.create({
  content: { gap: 12, paddingBottom: 32 },
  section: { gap: 10 },
  group: { gap: 6, marginTop: 4 },
  rowWrap: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, alignItems: 'center' },
  inviteRow: { gap: 6, paddingTop: 6 },
  checkbox: { paddingVertical: 4, paddingRight: 8 },
});
