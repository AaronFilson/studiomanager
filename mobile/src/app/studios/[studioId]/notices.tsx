import { useCan } from '@studiomanager/api-client';
import { useLocalSearchParams } from 'expo-router';

import { NoticeList } from '../../../features/NoticeList';
import { Screen } from '../../../ui';

export default function NoticesScreen() {
  const { studioId } = useLocalSearchParams<{ studioId: string }>();
  const { can } = useCan(studioId);
  return (
    <Screen>
      <NoticeList studioId={studioId} canSend={can('notices.send')} />
    </Screen>
  );
}
