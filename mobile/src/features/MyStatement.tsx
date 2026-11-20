import {
  getMeOptions,
  getMyConsignmentOptions,
  getStatementOptions,
  getStudioOptions,
} from '@studiomanager/api-client';
import { useQuery } from '@tanstack/react-query';
import { RefreshControl, ScrollView, StyleSheet, View } from 'react-native';

import { formatDate, formatMoney, storedValueKindLabel } from '../labels';
import { Badge, Body, Card, ErrorText, Loading, SectionTitle, Title } from '../ui';

/** Member's own studio balance, active prepaid cards, and consignment summary when applicable. */
export function MyStatement({ studioId }: { studioId: string }) {
  const me = useQuery(getMeOptions());
  const studio = useQuery(getStudioOptions({ path: { studioId } }));
  const accountId = me.data?.account.id;
  const statement = useQuery({
    ...getStatementOptions({ path: { studioId, accountId: accountId ?? '' } }),
    enabled: accountId !== undefined,
  });
  const consignment = useQuery({
    ...getMyConsignmentOptions({ path: { studioId } }),
    retry: false,
  });

  if (me.isPending || studio.isPending || (accountId !== undefined && statement.isPending)) {
    return <Loading />;
  }
  if (me.isError) return <ErrorText error={me.error} />;
  if (studio.isError) return <ErrorText error={studio.error} />;
  if (statement.isError) return <ErrorText error={statement.error} />;

  const currency = studio.data.currency;
  const money = (amount: string) => formatMoney(amount, currency);
  const day = (date: string) => formatDate(`${date}T12:00:00`);
  const owed = Number(statement.data?.owed ?? '0');
  const prepaid = (statement.data?.storedValues ?? []).filter((sv) => Number(sv.remaining) > 0);

  return (
    <ScrollView
      contentContainerStyle={styles.content}
      refreshControl={
        <RefreshControl
          refreshing={statement.isRefetching || consignment.isRefetching}
          onRefresh={() => {
            void statement.refetch();
            void consignment.refetch();
          }}
        />
      }
    >
      <Title>My statement</Title>

      <Card>
        <SectionTitle>Balance</SectionTitle>
        {owed > 0 && <Badge warning>You owe {money(statement.data?.owed ?? '0')}</Badge>}
        {owed < 0 && <Badge>You have {money(String(-owed))} in credit</Badge>}
        {owed === 0 && <Badge>Settled ({money('0')})</Badge>}
      </Card>

      <Card>
        <SectionTitle>Prepaid cards &amp; credits</SectionTitle>
        {prepaid.length === 0 ? (
          <Body muted>No active prepaid balances.</Body>
        ) : (
          prepaid.map((sv) => (
            <View key={sv.id} style={styles.itemRow}>
              <Body>
                {sv.label ?? storedValueKindLabel(sv.kind)}: {money(sv.remaining)} left
              </Body>
              <View style={styles.badges}>
                {sv.code && <Badge>{sv.code}</Badge>}
                {sv.expiresOn && <Badge>Expires {day(sv.expiresOn)}</Badge>}
              </View>
            </View>
          ))
        )}
      </Card>

      {consignment.isSuccess && (
        <>
          <Card>
            <SectionTitle>My consignment</SectionTitle>
            <Body>The studio owes you {money(consignment.data.owed)}.</Body>
          </Card>

          <Card>
            <SectionTitle>Your work on the shelf</SectionTitle>
            {consignment.data.items.length === 0 ? (
              <Body muted>Nothing taken in yet.</Body>
            ) : (
              consignment.data.items.map((item) => (
                <View key={item.id} style={styles.itemRow}>
                  <Body>
                    {item.description} · {money(item.retailPrice)}
                  </Body>
                  <View style={styles.badges}>
                    <Badge>{String(item.inStock)} on the shelf</Badge>
                    <Badge>{String(item.sold)} sold</Badge>
                    {item.returned > 0 && <Badge>{String(item.returned)} returned</Badge>}
                    {item.dueBack && <Badge warning>Ready to collect</Badge>}
                    {item.returnBy && item.inStock > 0 && !item.dueBack && (
                      <Badge>Until {day(item.returnBy)}</Badge>
                    )}
                  </View>
                </View>
              ))
            )}
          </Card>

          <Card>
            <SectionTitle>Consignment sales</SectionTitle>
            {consignment.data.sales.length === 0 ? (
              <Body muted>No sales yet.</Body>
            ) : (
              consignment.data.sales.map((sale) => (
                <View key={sale.id} style={styles.itemRow}>
                  <Body>
                    {day(sale.soldOn)} · {String(sale.quantity)} × {sale.description} · {money(sale.consignorAmount)} to
                    you
                  </Body>
                  {sale.refundedOn && <Badge>Refunded</Badge>}
                </View>
              ))
            )}
          </Card>
        </>
      )}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  content: { gap: 12, paddingBottom: 32 },
  itemRow: { gap: 4, paddingVertical: 4 },
  badges: { flexDirection: 'row', flexWrap: 'wrap', gap: 6 },
});
