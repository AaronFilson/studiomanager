import { router, useLocalSearchParams } from 'expo-router';

import { MaintenanceStaff } from '../../../features/MaintenanceStaff';
import { Screen } from '../../../ui';

export default function MaintenanceScreen() {
  const { studioId } = useLocalSearchParams<{ studioId: string }>();

  return (
    <Screen>
      <MaintenanceStaff
        studioId={studioId}
        onOpenKiln={(kilnId) => router.push(`/studios/${studioId}/kilns/${kilnId}`)}
      />
    </Screen>
  );
}
