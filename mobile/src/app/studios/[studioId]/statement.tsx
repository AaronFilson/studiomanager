import { useLocalSearchParams } from 'expo-router';

import { MyStatement } from '../../../features/MyStatement';
import { Screen } from '../../../ui';

export default function StatementScreen() {
  const { studioId } = useLocalSearchParams<{ studioId: string }>();
  return (
    <Screen>
      <MyStatement studioId={studioId} />
    </Screen>
  );
}
