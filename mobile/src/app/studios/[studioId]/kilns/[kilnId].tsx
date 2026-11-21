import { useLocalSearchParams } from 'expo-router';

import { KilnDetail } from '../../../../features/KilnDetail';
import { Screen } from '../../../../ui';

export default function KilnDetailScreen() {
  const { studioId, kilnId } = useLocalSearchParams<{ studioId: string; kilnId: string }>();

  return (
    <Screen>
      <KilnDetail key={`${studioId}:${kilnId}`} studioId={studioId} kilnId={kilnId} />
    </Screen>
  );
}
