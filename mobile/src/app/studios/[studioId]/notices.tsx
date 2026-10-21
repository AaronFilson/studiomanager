import { useLocalSearchParams } from 'expo-router';

import { NoticeList } from '../../../features/NoticeList';
import { Screen } from '../../../ui';

export default function NoticesScreen() {
  const { studioId } = useLocalSearchParams<{ studioId: string }>();
  return (
    <Screen>
      <NoticeList studioId={studioId} />
    </Screen>
  );
}
