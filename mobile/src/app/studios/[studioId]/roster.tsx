import { useLocalSearchParams } from 'expo-router';

import { RosterSchedule } from '../../../features/RosterSchedule';
import { Screen } from '../../../ui';

export default function RosterScreen() {
  const { studioId } = useLocalSearchParams<{ studioId: string }>();
  return (
    <Screen>
      <RosterSchedule studioId={studioId} />
    </Screen>
  );
}
