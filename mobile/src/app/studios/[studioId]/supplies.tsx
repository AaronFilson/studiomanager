import { useLocalSearchParams } from 'expo-router';

import { SupplyList } from '../../../features/SupplyList';
import { Screen } from '../../../ui';

export default function SuppliesScreen() {
  const { studioId } = useLocalSearchParams<{ studioId: string }>();
  return (
    <Screen>
      <SupplyList studioId={studioId} />
    </Screen>
  );
}
