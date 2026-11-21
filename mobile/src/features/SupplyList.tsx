import {
  createPurchaseRequestMutation,
  decidePurchaseRequestMutation,
  getStudioOptions,
  listMembersOptions,
  listPurchaseRequestsOptions,
  listSuppliesOptions,
  type Member,
  type PurchaseRequest,
  recordStockMovementMutation,
  sellSupplyMutation,
  type SupplyCategory,
  type SupplyItem,
  useCan,
} from '@studiomanager/api-client';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { RefreshControl, ScrollView, StyleSheet, View } from 'react-native';

import { formatDate, formatMoney, supplyCategories, supplyCategoryLabel } from '../labels';
import { Badge, Body, Button, Card, ErrorText, Field, Loading, SectionTitle, Title } from '../ui';

const decimal = /^\d{1,9}(\.\d{1,4})?$/;

/**
 * Clay and supplies on the go: search and filter by category, record stock movements, sell to a
 * member, request more when low, and advance open purchase requests.
 */
export function SupplyList({ studioId }: { studioId: string }) {
  const studio = useQuery(getStudioOptions({ path: { studioId } }));
  const permissions = useCan(studioId);
  const { can } = permissions;
  const canManage = can('supplies.manage');
  const canRequest = can('supplies.purchase.request');
  const canSell = can('supplies.sell') && can('members.read');
  const canApprove = can('supplies.purchase.approve');

  const supplies = useQuery(listSuppliesOptions({ path: { studioId } }));
  const [search, setSearch] = useState('');
  const [category, setCategory] = useState<SupplyCategory | undefined>(undefined);

  if (studio.isPending || supplies.isPending || permissions.isPending) return <Loading />;
  if (studio.isError) return <ErrorText error={studio.error} />;
  if (supplies.isError) return <ErrorText error={supplies.error} />;
  if (permissions.isError) return <ErrorText error={permissions.error} />;

  const currency = studio.data.currency;
  const term = search.trim().toLowerCase();
  const filtered = supplies.data.filter((item) => {
    if (category && item.category !== category) return false;
    if (!term) return true;
    return (
      item.name.toLowerCase().includes(term) ||
      (item.supplier?.toLowerCase().includes(term) ?? false) ||
      item.unit.toLowerCase().includes(term)
    );
  });

  return (
    <ScrollView
      contentContainerStyle={styles.content}
      keyboardShouldPersistTaps="handled"
      refreshControl={<RefreshControl refreshing={supplies.isRefetching} onRefresh={() => void supplies.refetch()} />}
    >
      <Title>Supplies</Title>

      <Field
        label="Search supplies"
        value={search}
        onChangeText={setSearch}
        autoCorrect={false}
        placeholder="Clay, glaze, supplier…"
      />

      <View style={styles.rowWrap} accessibilityRole="radiogroup" accessibilityLabel="Category">
        <Button compact label="All" primary={!category} selected={!category} onPress={() => setCategory(undefined)} />
        {supplyCategories.map((c) => (
          <Button
            key={c}
            compact
            label={supplyCategoryLabel(c)}
            primary={category === c}
            selected={category === c}
            onPress={() => setCategory(c)}
          />
        ))}
      </View>

      {filtered.length === 0 && (
        <Body muted>{supplies.data.length === 0 ? 'Nothing is stocked yet.' : 'No supplies match your filter.'}</Body>
      )}

      {filtered.map((item) => (
        <SupplyCard
          key={item.id}
          studioId={studioId}
          item={item}
          canManage={canManage}
          canRequest={canRequest}
          canSell={canSell}
          currency={currency}
        />
      ))}

      {canRequest && <PurchaseRequestsSection studioId={studioId} canApprove={canApprove} />}
    </ScrollView>
  );
}

function SupplyCard({
  studioId,
  item,
  canManage,
  canRequest,
  canSell,
  currency,
}: {
  studioId: string;
  item: SupplyItem;
  canManage: boolean;
  canRequest: boolean;
  canSell: boolean;
  currency: string;
}) {
  const queryClient = useQueryClient();
  const onSuccess = () => queryClient.invalidateQueries();
  const move = useMutation({ ...recordStockMovementMutation(), onSuccess });
  const request = useMutation({ ...createPurchaseRequestMutation(), onSuccess });
  const sell = useMutation({
    ...sellSupplyMutation(),
    onSuccess: async () => {
      setBuyer(null);
      setSelling(false);
      setMemberSearch('');
      await queryClient.invalidateQueries();
    },
  });
  const [quantity, setQuantity] = useState('1');
  const [selling, setSelling] = useState(false);
  const [memberSearch, setMemberSearch] = useState('');
  const [buyer, setBuyer] = useState<Member | null>(null);

  const members = useQuery({
    ...listMembersOptions({ path: { studioId } }),
    enabled: canSell && item.salePrice !== undefined && selling,
  });

  const qtyTrimmed = quantity.trim();
  const qtyValid = decimal.test(qtyTrimmed);
  const qtyPositive = qtyValid && Number(qtyTrimmed) > 0;
  const error = move.error ?? request.error ?? sell.error;
  const canSellItem = canSell && item.salePrice !== undefined;
  const canRequestItem = canRequest && item.needsOrdering;
  const showControls = canManage || canRequestItem || canSellItem;

  const memberTerm = memberSearch.trim().toLowerCase();
  const memberMatches = (members.data ?? [])
    .filter((m) => !memberTerm || (m.displayName?.toLowerCase().includes(memberTerm) ?? false))
    .slice(0, 6);

  return (
    <Card>
      <SectionTitle>{item.name}</SectionTitle>
      <View style={styles.badges}>
        <Badge>{supplyCategoryLabel(item.category)}</Badge>
        <Badge>
          {item.onHand} × {item.unit}
        </Badge>
        {item.salePrice !== undefined && <Badge>{formatMoney(item.salePrice, currency)} each</Badge>}
        {item.lastPurchaseUnitPrice !== undefined && (
          <Badge>
            Last paid {formatMoney(item.lastPurchaseUnitPrice, currency)}/{item.unit}
            {item.lastPurchasedAt ? ` on ${formatDate(item.lastPurchasedAt)}` : ''}
          </Badge>
        )}
        {item.low && (
          <Badge warning>
            Low{item.needsOrdering ? ', not on order' : `, ${item.onOrder} on order`}
          </Badge>
        )}
      </View>

      {item.supplier && <Body muted>Supplier: {item.supplier}</Body>}
      {item.low && item.substitute && <Badge warning>Substitute: {item.substitute}</Badge>}

      {showControls && (
        <View style={styles.group}>
          {(canManage || canSellItem) && (
            <Field
              label={`Quantity for ${item.name}`}
              value={quantity}
              onChangeText={setQuantity}
              keyboardType="decimal-pad"
            />
          )}
          <View style={styles.rowWrap}>
            {canManage && (
              <>
                <Button
                  compact
                  label="Received"
                  disabled={move.isPending || !qtyPositive}
                  onPress={() =>
                    move.mutate({ path: { studioId, itemId: item.id }, body: { kind: 'RECEIVED', quantity: qtyTrimmed } })
                  }
                />
                <Button
                  compact
                  label="Used"
                  disabled={move.isPending || !qtyPositive}
                  onPress={() =>
                    move.mutate({ path: { studioId, itemId: item.id }, body: { kind: 'USED', quantity: qtyTrimmed } })
                  }
                />
                <Button
                  compact
                  label="Counted"
                  disabled={move.isPending || !qtyValid}
                  onPress={() =>
                    move.mutate({ path: { studioId, itemId: item.id }, body: { kind: 'COUNTED', quantity: qtyTrimmed } })
                  }
                />
              </>
            )}
            {canRequestItem && (
              <Button
                compact
                primary
                label="Request more"
                disabled={request.isPending}
                onPress={() => request.mutate({ path: { studioId }, body: { itemId: item.id } })}
              />
            )}
            {canSellItem && (
              <Button
                compact
                label={selling ? 'Cancel sale' : 'Sell to member'}
                onPress={() => {
                  setSelling((open) => !open);
                  setBuyer(null);
                  setMemberSearch('');
                }}
              />
            )}
          </View>

          {selling && canSellItem && (
            <View style={styles.sellBox}>
              {buyer ? (
                <View style={styles.group}>
                  <Body>
                    Sell {qtyTrimmed} × {item.name} to {buyer.displayName ?? 'Unnamed member'}
                  </Body>
                  <View style={styles.rowWrap}>
                    <Button
                      compact
                      primary
                      label="Confirm sale"
                      disabled={sell.isPending || !qtyPositive}
                      onPress={() =>
                        sell.mutate({
                          path: { studioId, itemId: item.id },
                          body: { memberId: buyer.accountId, quantity: qtyTrimmed },
                        })
                      }
                    />
                    <Button compact label="Change member" onPress={() => setBuyer(null)} />
                  </View>
                </View>
              ) : (
                <View style={styles.group}>
                  <Field
                    label={`Sell ${item.name} to`}
                    value={memberSearch}
                    onChangeText={setMemberSearch}
                    autoCorrect={false}
                    placeholder="Type a member's name"
                  />
                  {members.isPending && <Loading />}
                  {members.isError && <ErrorText error={members.error} />}
                  <View style={styles.rowWrap}>
                    {memberMatches.map((m) => (
                      <Button
                        key={m.accountId}
                        compact
                        label={m.displayName ?? 'Unnamed member'}
                        onPress={() => setBuyer(m)}
                      />
                    ))}
                  </View>
                </View>
              )}
            </View>
          )}
        </View>
      )}

      {error && <ErrorText error={error} />}
    </Card>
  );
}

function PurchaseRequestsSection({ studioId, canApprove }: { studioId: string; canApprove: boolean }) {
  const queryClient = useQueryClient();
  const requests = useQuery(listPurchaseRequestsOptions({ path: { studioId } }));
  const decide = useMutation({
    ...decidePurchaseRequestMutation(),
    onSuccess: () => queryClient.invalidateQueries(),
  });

  if (requests.isError) return <ErrorText error={requests.error} />;
  if (!requests.isSuccess || requests.data.length === 0) return null;

  const act = (r: PurchaseRequest, to: 'APPROVED' | 'REJECTED' | 'ORDERED' | 'RECEIVED') =>
    decide.mutate({ path: { studioId, requestId: r.id }, body: { to, expectedVersion: r.version } });

  return (
    <Card>
      <SectionTitle>Open purchase requests</SectionTitle>
      {requests.data.map((r) => (
        <View key={r.id} style={styles.requestRow}>
          <Body>
            {r.quantity} × {r.itemName}
          </Body>
          <View style={styles.badges}>
            <Badge>{r.status.toLowerCase()}</Badge>
            {r.requestedByName && (
              <Badge>
                asked by {r.requestedByName}
                {r.requestedViaToken ? ` via ${r.requestedViaToken}` : ''}
              </Badge>
            )}
            {r.decidedByName && (
              <Badge>
                decided by {r.decidedByName}
                {r.decidedViaToken ? ` via ${r.decidedViaToken}` : ''}
              </Badge>
            )}
          </View>
          {r.note && <Body muted>{r.note}</Body>}
          <View style={styles.rowWrap}>
            {r.status === 'REQUESTED' && canApprove && (
              <>
                <Button compact primary label="Approve" disabled={decide.isPending} onPress={() => act(r, 'APPROVED')} />
                <Button compact label="Reject" disabled={decide.isPending} onPress={() => act(r, 'REJECTED')} />
              </>
            )}
            {r.status === 'APPROVED' && (
              <Button compact label="Ordered" disabled={decide.isPending} onPress={() => act(r, 'ORDERED')} />
            )}
            {(r.status === 'APPROVED' || r.status === 'ORDERED') && (
              <Button compact primary label="Received" disabled={decide.isPending} onPress={() => act(r, 'RECEIVED')} />
            )}
          </View>
        </View>
      ))}
      {decide.isError && <ErrorText error={decide.error} />}
    </Card>
  );
}

const styles = StyleSheet.create({
  content: { gap: 12, paddingBottom: 32 },
  badges: { flexDirection: 'row', flexWrap: 'wrap', gap: 6 },
  rowWrap: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, alignItems: 'center' },
  group: { gap: 8, marginTop: 4 },
  sellBox: { paddingTop: 6, gap: 6 },
  requestRow: { gap: 6, paddingTop: 8 },
});
