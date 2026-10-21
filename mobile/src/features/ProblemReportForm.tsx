import { listReportableAssetsOptions, reportProblemMutation } from '@studiomanager/api-client';
import { useMutation, useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { ScrollView, StyleSheet, View } from 'react-native';

import { Body, Button, ErrorText, Field } from '../ui';

/**
 * "Wheel 3 grinds": a member reports what they noticed, from where they are standing. The staff
 * and technicians get a notice.
 */
export function ProblemReportForm({ studioId, onDone }: { studioId: string; onDone?: () => void }) {
  const assets = useQuery(listReportableAssetsOptions({ path: { studioId } }));
  const [assetId, setAssetId] = useState<string | null>(null);
  const [description, setDescription] = useState('');
  const report = useMutation({
    ...reportProblemMutation(),
    onSuccess: () => {
      setAssetId(null);
      setDescription('');
      onDone?.();
    },
  });

  return (
    <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
      <Body muted>What is it with?</Body>
      {assets.isError && <ErrorText error={assets.error} />}
      <View style={styles.choices}>
        {[...(assets.data ?? []), { id: '', name: 'Something else', available: true }].map((a) => (
          <Button
            key={a.id || 'other'}
            primary={assetId === a.id}
            label={a.available ? a.name : `${a.name} (already out of service)`}
            onPress={() => setAssetId(a.id)}
          />
        ))}
      </View>
      <Field
        label="What's wrong?"
        value={description}
        onChangeText={setDescription}
        maxLength={1000}
        multiline
      />
      {report.isError && <ErrorText error={report.error} />}
      {report.isSuccess && <Body>Thanks, the staff have been told.</Body>}
      <Button
        primary
        label="Send report"
        disabled={report.isPending || assetId === null || !description.trim()}
        onPress={() =>
          report.mutate({
            path: { studioId },
            body: { description: description.trim(), ...(assetId ? { assetId } : {}) },
          })
        }
      />
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  content: { gap: 12, paddingBottom: 32 },
  choices: { gap: 6 },
});
