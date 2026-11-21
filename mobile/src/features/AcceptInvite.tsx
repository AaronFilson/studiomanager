import { acceptInviteMutation, getMeQueryKey } from '@studiomanager/api-client';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { StyleSheet, View } from 'react-native';

import { Body, Button, Card, ErrorText, Field, SectionTitle, Title } from '../ui';

/** Extracts the raw invite code whether the person typed a code or pasted an invite link. */
export function extractInviteCode(input: string): string {
  const trimmed = input.trim();
  const match = /\/invites\/([^/?#]+)/i.exec(trimmed);
  if (match?.[1]) {
    try {
      return decodeURIComponent(match[1]);
    } catch {
      return match[1];
    }
  }
  return trimmed;
}

/** Accepts an invite opened via deep link (studiomanager://invites/<code>). */
export function AcceptInvite({
  code,
  onJoined,
  onCancel,
}: {
  code?: string | string[];
  onJoined: (studioId: string) => void;
  onCancel?: () => void;
}) {
  const queryClient = useQueryClient();
  const normalizedCode = Array.isArray(code) ? (code[0] ?? '') : (code ?? '');
  const cleanCode = extractInviteCode(normalizedCode);
  const accept = useMutation({
    ...acceptInviteMutation(),
    onSuccess: async (membership) => {
      await queryClient.invalidateQueries({ queryKey: getMeQueryKey() });
      onJoined(membership.studio.id);
    },
  });

  return (
    <View style={styles.container}>
      <Card>
        <Title>Join a studio</Title>
        <Body>You&apos;ve been invited to join a studio on Studio Manager.</Body>
        {cleanCode ? <Body muted selectable>Invite code: {cleanCode}</Body> : null}
        {accept.isError && <ErrorText error={accept.error} />}
        <Button
          primary
          label={accept.isPending ? 'Joining…' : 'Accept invite'}
          disabled={accept.isPending || !cleanCode}
          onPress={() => accept.mutate({ path: { code: cleanCode } })}
        />
        {onCancel && <Button label="Back to studios" onPress={onCancel} />}
      </Card>
    </View>
  );
}

/** Lets a signed-in user paste an invite code or link directly on the studio picker. */
export function JoinWithCodeCard({ onJoined }: { onJoined: (studioId: string) => void }) {
  const queryClient = useQueryClient();
  const [rawCode, setRawCode] = useState('');
  const cleanCode = extractInviteCode(rawCode);
  const accept = useMutation({
    ...acceptInviteMutation(),
    onSuccess: async (membership) => {
      setRawCode('');
      await queryClient.invalidateQueries({ queryKey: getMeQueryKey() });
      onJoined(membership.studio.id);
    },
  });

  return (
    <Card>
      <SectionTitle>Join with invite code</SectionTitle>
      <Body muted>Paste the invite code or link your studio sent you.</Body>
      <Field
        label="Invite code"
        value={rawCode}
        onChangeText={setRawCode}
        autoCapitalize="none"
        autoCorrect={false}
        placeholder="Paste invite code or link"
      />
      {accept.isError && <ErrorText error={accept.error} />}
      <Button
        primary
        label={accept.isPending ? 'Joining…' : 'Join studio'}
        disabled={accept.isPending || !cleanCode}
        onPress={() => accept.mutate({ path: { code: cleanCode } })}
      />
    </Card>
  );
}

const styles = StyleSheet.create({
  container: { gap: 12 },
});
