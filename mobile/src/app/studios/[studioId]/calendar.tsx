import { useLocalSearchParams } from 'expo-router';

import { CalendarView } from '../../../features/CalendarView';
import { Screen } from '../../../ui';

export default function CalendarScreen() {
  const { studioId } = useLocalSearchParams<{ studioId: string }>();
  return (
    <Screen>
      <CalendarView studioId={studioId} />
    </Screen>
  );
}
