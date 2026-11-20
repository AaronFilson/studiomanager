import { useLocalSearchParams } from 'expo-router';

import { Financials } from '../../../features/Financials';
import { Screen } from '../../../ui';

export default function MoneyScreen() {
  const { studioId } = useLocalSearchParams<{ studioId: string }>();
  return (
    <Screen>
      <Financials studioId={studioId} />
    </Screen>
  );
}
