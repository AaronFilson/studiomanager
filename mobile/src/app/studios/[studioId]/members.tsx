import { useLocalSearchParams } from 'expo-router';

import { MemberList } from '../../../features/MemberList';
import { Screen } from '../../../ui';

export default function MembersScreen() {
  const { studioId } = useLocalSearchParams<{ studioId: string }>();
  return (
    <Screen>
      <MemberList studioId={studioId} />
    </Screen>
  );
}
