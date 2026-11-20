import { useLocalSearchParams } from 'expo-router';

import { GlazeDetail } from '../../../../features/GlazeDetail';
import { Screen } from '../../../../ui';

export default function GlazeDetailScreen() {
  const { studioId, glazeId } = useLocalSearchParams<{ studioId: string; glazeId: string }>();

  return (
    <Screen>
      <GlazeDetail studioId={studioId} glazeId={glazeId} />
    </Screen>
  );
}
