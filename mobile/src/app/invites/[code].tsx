import { router, useLocalSearchParams } from 'expo-router';

import { useAuth } from '../../auth/AuthProvider';
import { AcceptInvite } from '../../features/AcceptInvite';
import { Body, Button, Card, Loading, Screen, Title } from '../../ui';

/** Handles studiomanager://invites/<code> deep links. */
export default function InviteCodeScreen() {
  const { code } = useLocalSearchParams<{ code?: string | string[] }>();
  const normalizedCode = Array.isArray(code) ? (code[0] ?? '') : (code ?? '');
  const auth = useAuth();

  if (auth.status === 'starting') {
    return (
      <Screen edges={['top', 'bottom']}>
        <Loading />
      </Screen>
    );
  }
  if (auth.status === 'signedOut') {
    return (
      <Screen edges={['top', 'bottom']}>
        <Card>
          <Title>Join a studio</Title>
          <Body>Sign in first to accept this studio invite.</Body>
          <Button primary label="Sign in" disabled={!auth.ready} onPress={() => void auth.signIn()} />
          {auth.error && <Body>{auth.error}</Body>}
        </Card>
      </Screen>
    );
  }
  return (
    <Screen edges={['top', 'bottom']}>
      <AcceptInvite
        code={normalizedCode}
        onJoined={(studioId) => router.replace(`/studios/${studioId}`)}
        onCancel={() => router.replace('/?choose=1')}
      />
    </Screen>
  );
}
