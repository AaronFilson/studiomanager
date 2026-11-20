import { router, useLocalSearchParams } from 'expo-router';

import { useAuth } from '../../../auth/AuthProvider';
import { type StudioDestination, StudioMenu } from '../../../features/StudioMenu';
import { Screen } from '../../../ui';

export default function MoreScreen() {
  const { studioId } = useLocalSearchParams<{ studioId: string }>();
  const auth = useAuth();

  function open(destination: StudioDestination) {
    router.push(`/studios/${studioId}/${destination}`);
  }

  return (
    <Screen>
      <StudioMenu
        studioId={studioId}
        onNavigate={open}
        onSwitchStudio={() => router.replace('/?choose=1')}
        onSignOut={() => {
          void auth.signOut().then(() => {
            router.replace('/');
          });
        }}
      />
    </Screen>
  );
}
