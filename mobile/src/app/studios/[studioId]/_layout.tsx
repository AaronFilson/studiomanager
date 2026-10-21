import { getStudioOptions, listMyNotificationsOptions } from '@studiomanager/api-client';
import { useQuery } from '@tanstack/react-query';
import { Tabs, useLocalSearchParams } from 'expo-router';

import { usePalette } from '../../../ui';

export default function StudioLayout() {
  const { studioId } = useLocalSearchParams<{ studioId: string }>();
  const palette = usePalette();
  const studio = useQuery(getStudioOptions({ path: { studioId } }));
  const notices = useQuery(listMyNotificationsOptions({ path: { studioId } }));
  const unread = notices.data?.filter((n) => !n.readAt).length ?? 0;

  return (
    <Tabs
      screenOptions={{
        headerTitle: studio.data?.name ?? '',
        tabBarActiveTintColor: palette.accent,
        sceneStyle: { backgroundColor: palette.background },
      }}
    >
      <Tabs.Screen name="index" options={{ title: 'Pieces' }} />
      <Tabs.Screen name="intake" options={{ title: 'Drop off' }} />
      <Tabs.Screen name="tasks" options={{ title: 'Tasks' }} />
      <Tabs.Screen name="notices" options={{ title: 'Notices', tabBarBadge: unread > 0 ? unread : undefined }} />
      <Tabs.Screen name="pieces/[pieceId]" options={{ href: null, title: 'Piece' }} />
      <Tabs.Screen name="report" options={{ href: null, title: 'Report a problem' }} />
    </Tabs>
  );
}
