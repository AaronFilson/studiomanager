import { getMeOptions } from '@studiomanager/api-client';
import { useQuery } from '@tanstack/react-query';
import { Redirect, router } from 'expo-router';

import { useAuth } from '../auth/AuthProvider';
import { Body, Button, ErrorText, Loading, Screen, Title } from '../ui';

/** Signs in, then opens the studio: straight away when the person belongs to just one. */
export default function Home() {
  const auth = useAuth();

  if (auth.status === 'starting') {
    return (
      <Screen>
        <Loading />
      </Screen>
    );
  }
  if (auth.status === 'signedOut') {
    return (
      <Screen>
        <Title>Studio Manager</Title>
        <Body>Your pieces, notices and the shelf, on your phone.</Body>
        <Button primary label="Sign in" disabled={!auth.ready} onPress={() => void auth.signIn()} />
        {auth.error && <Body>{auth.error}</Body>}
      </Screen>
    );
  }
  return <Studios />;
}

function Studios() {
  const auth = useAuth();
  const me = useQuery(getMeOptions());

  if (me.isPending) {
    return (
      <Screen>
        <Loading />
      </Screen>
    );
  }
  if (me.isError) {
    return (
      <Screen>
        <ErrorText error={me.error} />
        <Button label="Try again" onPress={() => void me.refetch()} />
        <Button label="Sign out" onPress={() => void auth.signOut()} />
      </Screen>
    );
  }
  const [only, ...others] = me.data.memberships;
  if (only && others.length === 0) {
    return <Redirect href={`/studios/${only.studio.id}`} />;
  }
  return (
    <Screen>
      <Title>Your studios</Title>
      {me.data.memberships.length === 0 && (
        <Body>You aren&apos;t in a studio yet. Open the invite link your studio sent you.</Body>
      )}
      {me.data.memberships.map((m) => (
        <Button key={m.studio.id} label={m.studio.name} onPress={() => router.push(`/studios/${m.studio.id}`)} />
      ))}
      <Button label="Sign out" onPress={() => void auth.signOut()} />
    </Screen>
  );
}
