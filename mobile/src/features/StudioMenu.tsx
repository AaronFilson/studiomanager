import { getMyConsignmentOptions, getStudioOptions, useCan } from '@studiomanager/api-client';
import { useQuery } from '@tanstack/react-query';
import { ScrollView, StyleSheet, View } from 'react-native';

import { roleLabel } from '../labels';
import { Badge, Body, Button, Card, ErrorText, Loading, SectionTitle, Title } from '../ui';

export type StudioDestination =
  | 'today'
  | 'kilns'
  | 'supplies'
  | 'glazes'
  | 'roster'
  | 'calendar'
  | 'maintenance'
  | 'members'
  | 'money'
  | 'suggestions'
  | 'statement'
  | 'notices'
  | 'report';

/**
 * The More / Studio hub: every screen outside the 5 main bottom tabs, strictly gated with the
 * caller's studio permissions to match the web sidebar, plus Switch studio and Sign out.
 */
export function StudioMenu({
  studioId,
  onNavigate,
  onSwitchStudio,
  onSignOut,
}: {
  studioId: string;
  onNavigate: (destination: StudioDestination) => void;
  onSwitchStudio: () => void;
  onSignOut: () => void;
}) {
  const studio = useQuery(getStudioOptions({ path: { studioId } }));
  const permissions = useCan(studioId);
  const { can } = permissions;
  const consignment = useQuery({
    ...getMyConsignmentOptions({ path: { studioId } }),
    enabled: studio.isSuccess && !permissions.isPending && !can('money.read'),
    retry: false,
  });

  if (studio.isPending || permissions.isPending) return <Loading />;
  if (studio.isError) return <ErrorText error={studio.error} />;
  if (permissions.isError) return <ErrorText error={permissions.error} />;

  const staffLinks: { destination: StudioDestination; label: string }[] = [
    ...(can('today.read') ? [{ destination: 'today' as const, label: 'Today' }] : []),
    ...(can('kilns.manage') || can('firings.manage') ? [{ destination: 'kilns' as const, label: 'Kilns' }] : []),
    ...(can('supplies.manage') || can('supplies.sell') || can('supplies.purchase.request')
      ? [{ destination: 'supplies' as const, label: 'Supplies' }]
      : []),
    ...(can('maintenance.manage') ? [{ destination: 'maintenance' as const, label: 'Maintenance' }] : []),
    ...(can('proposals.review') ? [{ destination: 'suggestions' as const, label: 'Assistant suggestions' }] : []),
    ...(can('money.read') || can('money.stored_value.manage') || can('money.payables.manage')
      ? [
          {
            destination: 'money' as const,
            label:
              can('money.read') || can('money.stored_value.manage')
                ? 'Money & payables'
                : 'Artists and instructors',
          },
        ]
      : []),
    ...(can('members.read') ? [{ destination: 'members' as const, label: 'Members' }] : []),
    ...(can('notices.send') ? [{ destination: 'notices' as const, label: 'Send a studio notice' }] : []),
  ];

  const memberLinks: { destination: StudioDestination; label: string }[] = [
    { destination: 'glazes', label: 'Glaze book' },
    { destination: 'roster', label: 'Roster' },
    { destination: 'calendar', label: 'Calendar' },
    {
      destination: 'statement',
      label: !can('money.read') && consignment.isSuccess ? 'My statement & consignment' : 'My statement',
    },
    { destination: 'report', label: 'Report a problem' },
  ];

  return (
    <ScrollView contentContainerStyle={styles.content}>
      <Card>
        <Title>{studio.data.name}</Title>
        <View style={styles.badges}>
          <Badge>{roleLabel(studio.data.myRole)}</Badge>
          <Badge>{String(studio.data.memberCount)} members</Badge>
        </View>
      </Card>

      {staffLinks.length > 0 && (
        <Card>
          <SectionTitle>Studio operations</SectionTitle>
          <View style={styles.list}>
            {staffLinks.map((item) => (
              <Button key={`${item.destination}-${item.label}`} label={item.label} onPress={() => onNavigate(item.destination)} />
            ))}
          </View>
        </Card>
      )}

      <Card>
        <SectionTitle>Studio</SectionTitle>
        <View style={styles.list}>
          {memberLinks.map((item) => (
            <Button key={`${item.destination}-${item.label}`} label={item.label} onPress={() => onNavigate(item.destination)} />
          ))}
        </View>
      </Card>

      <Card>
        <SectionTitle>Account</SectionTitle>
        <Body muted>Switch between studios or sign out on this device.</Body>
        <View style={styles.list}>
          <Button label="Switch studio" onPress={onSwitchStudio} />
          <Button label="Sign out" onPress={onSignOut} />
        </View>
      </Card>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  content: { gap: 12, paddingBottom: 32 },
  badges: { flexDirection: 'row', flexWrap: 'wrap', gap: 6 },
  list: { gap: 8 },
});
