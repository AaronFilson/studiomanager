import { router, useLocalSearchParams } from 'expo-router';

import { GlazeList } from '../../../../features/GlazeList';
import { Screen } from '../../../../ui';

export default function GlazesScreen() {
  const { studioId } = useLocalSearchParams<{ studioId: string }>();

  return (
    <Screen>
      <GlazeList studioId={studioId} onOpen={(glazeId) => router.push(`/studios/${studioId}/glazes/${glazeId}`)} />
    </Screen>
  );
}
