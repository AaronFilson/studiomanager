import { getStudioOptions, listMyNotificationsOptions, useCan } from '@studiomanager/api-client';
import { useQuery } from '@tanstack/react-query';
import { router, Tabs, useLocalSearchParams } from 'expo-router';
import { Pressable, StyleSheet, Text } from 'react-native';

import { usePalette } from '../../../ui';

function TabGlyph({ glyph, color }: { glyph: string; color: string }) {
  return (
    <Text accessible={false} style={[styles.glyph, { color }]}>
      {glyph}
    </Text>
  );
}

export default function StudioLayout() {
  const { studioId } = useLocalSearchParams<{ studioId: string }>();
  const palette = usePalette();
  const studio = useQuery(getStudioOptions({ path: { studioId } }));
  const { can } = useCan(studioId);
  const notices = useQuery(listMyNotificationsOptions({ path: { studioId } }));
  const unread = notices.data?.items.filter((n) => !n.readAt).length ?? 0;

  function goBack() {
    if (router.canGoBack()) {
      router.back();
    } else {
      router.replace(`/studios/${studioId}/more`);
    }
  }

  const hiddenHeader = (title: string) => ({
    href: null,
    title,
    headerTitle: title,
    headerLeft: () => (
      <Pressable accessibilityRole="button" accessibilityLabel="Back" onPress={goBack} style={styles.backButton}>
        <Text style={[styles.backText, { color: palette.accent }]}>← Back</Text>
      </Pressable>
    ),
  });

  return (
    <Tabs
      backBehavior="history"
      screenOptions={{
        headerTitle: studio.data?.name ?? '',
        tabBarActiveTintColor: palette.accent,
        sceneStyle: { backgroundColor: palette.background },
      }}
    >
      <Tabs.Screen
        name="index"
        options={{
          title: can('pieces.move') ? 'Pieces' : 'My pieces',
          tabBarIcon: ({ color }) => <TabGlyph glyph="◈" color={color} />,
        }}
      />
      <Tabs.Screen
        name="intake"
        options={{
          title: 'Drop off',
          tabBarIcon: ({ color }) => <TabGlyph glyph="＋" color={color} />,
        }}
      />
      <Tabs.Screen
        name="tasks"
        options={{
          title: 'Tasks',
          tabBarIcon: ({ color }) => <TabGlyph glyph="✓" color={color} />,
        }}
      />
      <Tabs.Screen
        name="notices"
        options={{
          title: 'Notices',
          tabBarBadge: unread > 0 ? unread : undefined,
          tabBarIcon: ({ color }) => <TabGlyph glyph="✉" color={color} />,
        }}
      />
      <Tabs.Screen
        name="more"
        options={{
          title: 'Studio',
          tabBarIcon: ({ color }) => <TabGlyph glyph="☰" color={color} />,
        }}
      />

      <Tabs.Screen name="today" options={hiddenHeader('Today')} />
      <Tabs.Screen name="kilns/index" options={hiddenHeader('Kilns')} />
      <Tabs.Screen name="kilns/[kilnId]" options={hiddenHeader('Kiln')} />
      <Tabs.Screen name="supplies" options={hiddenHeader('Supplies')} />
      <Tabs.Screen name="glazes/index" options={hiddenHeader('Glaze book')} />
      <Tabs.Screen name="glazes/[glazeId]" options={hiddenHeader('Glaze recipe')} />
      <Tabs.Screen name="roster" options={hiddenHeader('Roster')} />
      <Tabs.Screen name="calendar" options={hiddenHeader('Calendar')} />
      <Tabs.Screen name="maintenance" options={hiddenHeader('Maintenance')} />
      <Tabs.Screen name="members" options={hiddenHeader('Members')} />
      <Tabs.Screen name="money" options={hiddenHeader('Money')} />
      <Tabs.Screen name="suggestions" options={hiddenHeader('Assistant suggestions')} />
      <Tabs.Screen name="statement" options={hiddenHeader('My statement')} />
      <Tabs.Screen name="pieces/[pieceId]" options={hiddenHeader('Piece')} />
      <Tabs.Screen name="report" options={hiddenHeader('Report a problem')} />
    </Tabs>
  );
}

const styles = StyleSheet.create({
  glyph: { fontSize: 18, fontWeight: '600' },
  backButton: { paddingHorizontal: 12, paddingVertical: 6 },
  backText: { fontSize: 16, fontWeight: '600' },
});
