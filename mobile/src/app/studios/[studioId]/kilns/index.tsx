import { router, useLocalSearchParams } from 'expo-router';

import { KilnList } from '../../../../features/KilnList';
import { Screen } from '../../../../ui';

export default function KilnsScreen() {
  const { studioId } = useLocalSearchParams<{ studioId: string }>();

  return (
    <Screen>
      <KilnList studioId={studioId} onOpen={(kilnId) => router.push(`/studios/${studioId}/kilns/${kilnId}`)} />
    </Screen>
  );
}
