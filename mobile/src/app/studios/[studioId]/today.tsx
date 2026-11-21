import { router, useLocalSearchParams } from 'expo-router';

import { TodayDashboard } from '../../../features/TodayDashboard';
import { Screen } from '../../../ui';

export default function TodayScreen() {
  const { studioId } = useLocalSearchParams<{ studioId: string }>();

  return (
    <Screen>
      <TodayDashboard
        studioId={studioId}
        onOpenKiln={(kilnId) => router.push(`/studios/${studioId}/kilns/${kilnId}`)}
        onOpenKilns={() => router.push(`/studios/${studioId}/kilns`)}
        onOpenPieces={() => router.push(`/studios/${studioId}`)}
        onOpenSupplies={() => router.push(`/studios/${studioId}/supplies`)}
      />
    </Screen>
  );
}
