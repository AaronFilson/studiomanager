import {
  cappedNote,
  FROM_THE_EXPORT,
  getStudioOptions,
  listBalancesOptions,
  listMembersOptions,
  listPayeesOptions,
  listStoredValuesOptions,
  type Member,
  type Provider,
  recordPaymentMutation,
  type StoredValue,
  useCan,
  useStoredValueMutation,
} from '@studiomanager/api-client';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { RefreshControl, ScrollView, StyleSheet, View } from 'react-native';

import { formatDate, formatMoney, providerLabels, providers, storedValueKindLabel } from '../labels';
import { Badge, Body, Button, Card, ErrorText, Field, Loading, SectionTitle, Title } from '../ui';

type FinancialsTab = 'BALANCES' | 'PREPAID' | 'PAYABLES';

const decimal = /^\d{1,9}(\.\d{1,4})?$/;

function percentText(share: string): string {
  return `${String(Number((Number(share) * 100).toFixed(2)))}%`;
}

/**
 * Staff counter financials: member balances & payment recording, prepaid card lookup & redemption
 * (with a member name picker), and artist/instructor payables.
 */
export function Financials({ studioId }: { studioId: string }) {
  const studio = useQuery(getStudioOptions({ path: { studioId } }));
  const permissions = useCan(studioId);
  const { can } = permissions;
  const canReadMoney = can('money.read');
  const canManagePayables = can('money.payables.manage');

  const tabs: { key: FinancialsTab; label: string }[] = [
    ...(canReadMoney
      ? [
          { key: 'BALANCES' as const, label: 'Balances' },
          { key: 'PREPAID' as const, label: 'Prepaid cards' },
        ]
      : []),
    ...(canManagePayables ? [{ key: 'PAYABLES' as const, label: 'Payables' }] : []),
  ];

  const [chosenTab, setChosenTab] = useState<FinancialsTab | null>(null);
  const activeTab = chosenTab && tabs.some((t) => t.key === chosenTab) ? chosenTab : tabs[0]?.key;

  if (studio.isPending || permissions.isPending) return <Loading />;
  if (studio.isError) return <ErrorText error={studio.error} />;
  if (permissions.isError) return <ErrorText error={permissions.error} />;

  if (!activeTab) {
    return (
      <Card>
        <Title>Money</Title>
        <Body muted>You don&apos;t have permission to view studio financials.</Body>
      </Card>
    );
  }

  const currency = studio.data.currency;

  return (
    <View style={styles.container}>
      <View style={styles.rowWrap} accessibilityRole="tablist" accessibilityLabel="Financial sections">
        {tabs.map((tab) => (
          <Button
            key={tab.key}
            compact
            primary={activeTab === tab.key}
            label={tab.label}
            onPress={() => setChosenTab(tab.key)}
          />
        ))}
      </View>

      {activeTab === 'BALANCES' && <BalancesPanel studioId={studioId} currency={currency} />}
      {activeTab === 'PREPAID' && <PrepaidPanel studioId={studioId} currency={currency} />}
      {activeTab === 'PAYABLES' && <PayablesPanel studioId={studioId} currency={currency} />}
    </View>
  );
}

function MemberPicker({
  members,
  selected,
  onSelect,
  label,
}: {
  members: Member[];
  selected: Member | null;
  onSelect: (member: Member | null) => void;
  label: string;
}) {
  const [search, setSearch] = useState('');
  const term = search.trim().toLowerCase();
  const matches = members
    .filter((m) => !term || (m.displayName?.toLowerCase().includes(term) ?? false))
    .slice(0, 6);

  if (selected) {
    return (
      <View style={styles.rowWrap}>
        <Body>Member: {selected.displayName ?? 'Unnamed member'}</Body>
        <Button compact label="Change" onPress={() => onSelect(null)} />
      </View>
    );
  }

  return (
    <View style={styles.group}>
      <Field label={label} value={search} onChangeText={setSearch} autoCorrect={false} placeholder="Type a member's name" />
      <View style={styles.rowWrap}>
        {matches.map((m) => (
          <Button
            key={m.accountId}
            compact
            label={m.displayName ?? 'Unnamed member'}
            onPress={() => {
              onSelect(m);
              setSearch('');
            }}
          />
        ))}
      </View>
    </View>
  );
}

function BalancesPanel({ studioId, currency }: { studioId: string; currency: string }) {
  const queryClient = useQueryClient();
  const balances = useQuery(listBalancesOptions({ path: { studioId } }));
  const members = useQuery(listMembersOptions({ path: { studioId } }));
  const [search, setSearch] = useState('');
  const [payer, setPayer] = useState<Member | null>(null);
  const [amount, setAmount] = useState('');
  const [provider, setProvider] = useState<Provider>('CASH');
  const [reference, setReference] = useState('');

  const pay = useMutation({
    ...recordPaymentMutation(),
    onSuccess: async () => {
      setAmount('');
      setReference('');
      await queryClient.invalidateQueries();
    },
  });

  const money = (v: string) => formatMoney(v, currency);
  const term = search.trim().toLowerCase();
  const filtered = (balances.data ?? []).filter(
    (b) => !term || (b.memberName?.toLowerCase().includes(term) ?? false),
  );
  const amountTrimmed = amount.trim();
  const amountValid = decimal.test(amountTrimmed) && Number(amountTrimmed) > 0;

  return (
    <ScrollView
      contentContainerStyle={styles.content}
      keyboardShouldPersistTaps="handled"
      refreshControl={<RefreshControl refreshing={balances.isRefetching} onRefresh={() => void balances.refetch()} />}
    >
      <Field label="Search members" value={search} onChangeText={setSearch} autoCorrect={false} />

      {balances.isPending && <Loading />}
      {balances.isError && <ErrorText error={balances.error} />}
      {balances.isSuccess && filtered.length === 0 && (
        <Body muted>{balances.data.length === 0 ? 'Nobody owes anything.' : 'No members match your search.'}</Body>
      )}

      {filtered.map((b) => {
        const owedNum = Number(b.owed);
        return (
          <Card key={b.memberId}>
            <SectionTitle>{b.memberName ?? 'Unnamed member'}</SectionTitle>
            <View style={styles.rowWrap}>
              {owedNum > 0 && <Badge warning>Owes {money(b.owed)}</Badge>}
              {owedNum < 0 && <Badge>{money(String(-owedNum))} in credit</Badge>}
              {owedNum === 0 && <Badge>Settled</Badge>}
              <Button
                compact
                label="Record payment"
                onPress={() => {
                  const found = members.data?.find((m) => m.accountId === b.memberId);
                  setPayer(
                    found ?? {
                      accountId: b.memberId,
                      displayName: b.memberName,
                      role: 'MEMBER',
                      roleId: 'member',
                      roleName: 'Member',
                      joinedAt: '',
                    },
                  );
                  if (owedNum > 0) setAmount(b.owed);
                }}
              />
            </View>
          </Card>
        );
      })}

      <Card>
        <SectionTitle>Record a payment</SectionTitle>
        {members.isError && <ErrorText error={members.error} />}
        <MemberPicker
          members={members.data ?? []}
          selected={payer}
          onSelect={setPayer}
          label="Payment from"
        />
        <Field
          label={`Amount (${currency})`}
          value={amount}
          onChangeText={setAmount}
          keyboardType="decimal-pad"
        />
        <Body muted>Paid by</Body>
        <View style={styles.rowWrap}>
          {providers.map((p) => (
            <Button
              key={p}
              compact
              primary={provider === p}
              label={providerLabels[p]}
              onPress={() => setProvider(p)}
            />
          ))}
        </View>
        <Field label="Reference (optional)" value={reference} onChangeText={setReference} maxLength={80} />
        {pay.isError && <ErrorText error={pay.error} />}
        {pay.isSuccess && <Body>Payment recorded.</Body>}
        <Button
          primary
          label="Save payment"
          disabled={pay.isPending || !payer || !amountValid}
          onPress={() => {
            if (!payer) return;
            pay.mutate({
              path: { studioId },
              body: {
                memberId: payer.accountId,
                amount: amountTrimmed,
                provider,
                ...(reference.trim() ? { reference: reference.trim() } : {}),
              },
            });
          }}
        />
      </Card>
    </ScrollView>
  );
}

function PrepaidPanel({ studioId, currency }: { studioId: string; currency: string }) {
  const storedValues = useQuery(listStoredValuesOptions({ path: { studioId } }));
  const members = useQuery(listMembersOptions({ path: { studioId } }));
  const capNote = cappedNote(storedValues.data, { order: 'newest', noun: 'balances' }, FROM_THE_EXPORT);

  return (
    <ScrollView
      contentContainerStyle={styles.content}
      keyboardShouldPersistTaps="handled"
      refreshControl={
        <RefreshControl refreshing={storedValues.isRefetching} onRefresh={() => void storedValues.refetch()} />
      }
    >
      {storedValues.isPending && <Loading />}
      {storedValues.isError && <ErrorText error={storedValues.error} />}
      {members.isError && <ErrorText error={members.error} />}
      {capNote && <Body muted>{capNote}</Body>}
      {storedValues.isSuccess && storedValues.data.items.length === 0 && (
        <Body muted>No active prepaid balances.</Body>
      )}
      {storedValues.data?.items.map((sv) => (
        <PrepaidCard key={sv.id} studioId={studioId} sv={sv} currency={currency} members={members.data ?? []} />
      ))}
    </ScrollView>
  );
}

function PrepaidCard({
  studioId,
  sv,
  currency,
  members,
}: {
  studioId: string;
  sv: StoredValue;
  currency: string;
  members: Member[];
}) {
  const queryClient = useQueryClient();
  const [redeeming, setRedeeming] = useState(false);
  const [member, setMember] = useState<Member | null>(null);
  const [amount, setAmount] = useState('');
  const redeem = useMutation({
    ...useStoredValueMutation(),
    onSuccess: async () => {
      setRedeeming(false);
      setAmount('');
      await queryClient.invalidateQueries();
    },
  });

  const amountTrimmed = amount.trim();
  const amountValid = decimal.test(amountTrimmed) && Number(amountTrimmed) > 0;

  function openRedeem() {
    if (!redeeming && sv.holder) {
      const matched = members.find((m) => m.accountId === sv.holder);
      if (matched) {
        setMember(matched);
      } else if (sv.holderName) {
        setMember({
          accountId: sv.holder,
          displayName: sv.holderName,
          role: 'MEMBER',
          roleId: 'member',
          roleName: 'Member',
          joinedAt: '',
        });
      }
    }
    setRedeeming((open) => !open);
  }

  return (
    <Card>
      <SectionTitle>
        {storedValueKindLabel(sv.kind)}
        {sv.label ? ` · ${sv.label}` : ''}
      </SectionTitle>
      <View style={styles.badges}>
        <Badge>{formatMoney(sv.remaining, currency)} left</Badge>
        {sv.holderName && <Badge>{sv.holderName}</Badge>}
        {sv.code && <Badge>{sv.code}</Badge>}
        {sv.expiresOn && <Badge>Expires {formatDate(`${sv.expiresOn}T12:00:00`)}</Badge>}
      </View>

      <Button compact label={redeeming ? 'Cancel' : 'Redeem'} onPress={openRedeem} />

      {redeeming && (
        <View style={styles.group}>
          <MemberPicker members={members} selected={member} onSelect={setMember} label="Pays for member" />
          <Field
            label={`Redeem amount (${currency})`}
            value={amount}
            onChangeText={setAmount}
            keyboardType="decimal-pad"
          />
          {redeem.isError && <ErrorText error={redeem.error} />}
          <Button
            primary
            label="Apply to balance"
            disabled={redeem.isPending || !member || !amountValid}
            onPress={() => {
              if (!member) return;
              redeem.mutate({
                path: { studioId, storedValueId: sv.id },
                body: { memberId: member.accountId, amount: amountTrimmed },
              });
            }}
          />
        </View>
      )}
    </Card>
  );
}

function PayablesPanel({ studioId, currency }: { studioId: string; currency: string }) {
  const payees = useQuery(listPayeesOptions({ path: { studioId } }));
  const capNote = cappedNote(payees.data, { order: 'first', noun: 'payees' }, FROM_THE_EXPORT);
  const money = (v: string) => formatMoney(v, currency);

  return (
    <ScrollView
      contentContainerStyle={styles.content}
      refreshControl={<RefreshControl refreshing={payees.isRefetching} onRefresh={() => void payees.refetch()} />}
    >
      {payees.isPending && <Loading />}
      {payees.isError && <ErrorText error={payees.error} />}
      {capNote && <Body muted>{capNote}</Body>}
      {payees.isSuccess && payees.data.items.length === 0 && (
        <Body muted>No artists or instructors set up yet.</Body>
      )}
      {payees.data?.items.map((p) => {
        const owedNum = Number(p.owed);
        return (
          <Card key={p.id}>
            <SectionTitle>{p.name}</SectionTitle>
            <View style={styles.badges}>
              <Badge warning={owedNum > 0}>
                {owedNum < 0 ? `Owes ${money(String(-owedNum))}` : `Owed ${money(p.owed)}`}
              </Badge>
              {p.consignorShare && <Badge>{percentText(p.consignorShare)} to artist</Badge>}
              {p.files1099 && !p.w9ReceivedOn && <Badge warning>W-9 missing</Badge>}
              {p.archived && <Badge>Archived</Badge>}
            </View>
          </Card>
        );
      })}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, gap: 10 },
  content: { gap: 12, paddingBottom: 32 },
  rowWrap: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, alignItems: 'center' },
  badges: { flexDirection: 'row', flexWrap: 'wrap', gap: 6 },
  group: { gap: 8, marginTop: 4 },
});
