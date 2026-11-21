import { router } from 'expo-router';

import { Body, Button, Card, Screen, Title } from '../ui';

/** Fallback for unknown deep links or paths so the app never shows a raw router error. */
export default function NotFoundScreen() {
  return (
    <Screen edges={['top', 'bottom']}>
      <Card>
        <Title>Page not found</Title>
        <Body muted>That link doesn&apos;t match a screen in Studio Manager.</Body>
        <Button primary label="Go home" onPress={() => router.replace('/')} />
      </Card>
    </Screen>
  );
}
