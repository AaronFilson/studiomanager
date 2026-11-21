import { getMeOptions } from '@studiomanager/api-client';
import { useQuery } from '@tanstack/react-query';
import { Redirect, router, useLocalSearchParams } from 'expo-router';
import { ScrollView, StyleSheet } from 'react-native';

import { useAuth } from '../auth/AuthProvider';
import { JoinWithCodeCard } from '../features/AcceptInvite';
import { Body, Button, ErrorText, Loading, Screen, Title } from '../ui';

/** Signs in, then opens the studio: straight away when the person belongs to just one. */
export default function Home() {
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
  const { choose } = useLocalSearchParams<{ choose?: string }>();
  const me = useQuery(getMeOptions());

  if (me.isPending) {
    return (
      <Screen edges={['top', 'bottom']}>
        <Loading />
      </Screen>
    );
  }
  if (me.isError) {
    return (
      <Screen edges={['top', 'bottom']}>
        <ErrorText error={me.error} />
        <Button label="Try again" onPress={() => void me.refetch()} />
        <Button label="Sign out" onPress={() => void auth.signOut()} />
      </Screen>
    );
  }
  const [only, ...others] = me.data.memberships;
  if (only && others.length === 0 && choose !== '1') {
    return <Redirect href={`/studios/${only.studio.id}`} />;
  }
  return (
    <Screen edges={['top', 'bottom']}>
      <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
        <Title>Your studios</Title>
        {me.data.memberships.length === 0 && (
          <Body>You aren&apos;t in a studio yet. Enter an invite code below or open the link your studio sent you.</Body>
        )}
        {me.data.memberships.map((m) => (
          <Button key={m.studio.id} label={m.studio.name} onPress={() => router.push(`/studios/${m.studio.id}`)} />
        ))}
        <JoinWithCodeCard onJoined={(studioId) => router.replace(`/studios/${studioId}`)} />
        <Button label="Sign out" onPress={() => void auth.signOut()} />
      </ScrollView>
    </Screen>
  );
}

const styles = StyleSheet.create({
  content: { gap: 12, paddingBottom: 32 },
});
