import {
  type Asset,
  cappedNote,
  listAssetsOptions,
  listProblemReportsOptions,
  type ProblemReport,
  resolveProblemReportMutation,
  setAssetAvailabilityMutation,
  useCan,
} from '@studiomanager/api-client';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { RefreshControl, ScrollView, StyleSheet, View } from 'react-native';

import { assetKindLabel, dueText, formatDateTime } from '../labels';
import { Badge, Body, Button, Card, ErrorText, Field, Loading, SectionTitle, Title } from '../ui';

/** Staff maintenance view: open problem reports to resolve, and equipment assets with availability toggles. */
export function MaintenanceStaff({
  studioId,
  onOpenKiln,
}: {
  studioId: string;
  onOpenKiln?: (kilnId: string) => void;
}) {
  const assets = useQuery(listAssetsOptions({ path: { studioId } }));
  const reports = useQuery(listProblemReportsOptions({ path: { studioId }, query: { open: true } }));
  const permissions = useCan(studioId);
  const canOpenKilns =
    permissions.can('kilns.read') || permissions.can('kilns.manage') || permissions.can('firings.manage');

  if ((assets.isPending && reports.isPending) || permissions.isPending) return <Loading />;

  const reportsCapNote = cappedNote(
    reports.data,
    { order: 'newest', noun: 'open reports' },
    'Resolve some of these to see older ones.',
  );

  return (
    <ScrollView
      contentContainerStyle={styles.content}
      keyboardShouldPersistTaps="handled"
      refreshControl={
        <RefreshControl
          refreshing={assets.isRefetching || reports.isRefetching}
          onRefresh={() => {
            void assets.refetch();
            void reports.refetch();
          }}
        />
      }
    >
      <Title>Maintenance</Title>

      {permissions.isError && <ErrorText error={permissions.error} />}
      {reports.isError && <ErrorText error={reports.error} />}
      {reports.data && reports.data.items.length > 0 && (
        <View style={styles.section}>
          <SectionTitle>Reported problems ({String(reports.data.items.length)})</SectionTitle>
          {reportsCapNote && <Body muted>{reportsCapNote}</Body>}
          {reports.data.items.map((report) => (
            <ProblemReportCard key={report.id} studioId={studioId} report={report} />
          ))}
        </View>
      )}

      {assets.isError && <ErrorText error={assets.error} />}
      {assets.isSuccess && assets.data.length === 0 && <Body muted>No maintained assets yet.</Body>}
      {assets.data && assets.data.length > 0 && (
        <View style={styles.section}>
          <SectionTitle>Equipment &amp; studio assets</SectionTitle>
          {assets.data.map((asset) => (
            <AssetCard
              key={asset.id}
              studioId={studioId}
              asset={asset}
              onOpenKiln={canOpenKilns ? onOpenKiln : undefined}
            />
          ))}
        </View>
      )}
    </ScrollView>
  );
}

function ProblemReportCard({ studioId, report }: { studioId: string; report: ProblemReport }) {
  const queryClient = useQueryClient();
  const [resolving, setResolving] = useState(false);
  const [resolution, setResolution] = useState('');
  const resolve = useMutation({
    ...resolveProblemReportMutation(),
    onSuccess: async () => {
      setResolving(false);
      setResolution('');
      await queryClient.invalidateQueries();
    },
  });

  return (
    <Card>
      <SectionTitle>{report.assetName ?? 'Something else'}</SectionTitle>
      <Body>{report.description}</Body>
      <Body muted>
        {report.reportedByName ?? 'A member'} · {formatDateTime(report.reportedAt)}
      </Body>
      {resolving ? (
        <View style={styles.group}>
          <Field label="What was done? (optional)" value={resolution} onChangeText={setResolution} maxLength={1000} />
          {resolve.isError && <ErrorText error={resolve.error} />}
          <View style={styles.rowWrap}>
            <Button
              compact
              primary
              label="Confirm resolve"
              disabled={resolve.isPending}
              onPress={() =>
                resolve.mutate({
                  path: { studioId, reportId: report.id },
                  body: {
                    expectedVersion: report.version,
                    ...(resolution.trim() ? { resolution: resolution.trim() } : {}),
                  },
                })
              }
            />
            <Button compact label="Cancel" onPress={() => setResolving(false)} />
          </View>
        </View>
      ) : (
        <Button compact label="Resolve" onPress={() => setResolving(true)} />
      )}
    </Card>
  );
}

function AssetCard({
  studioId,
  asset,
  onOpenKiln,
}: {
  studioId: string;
  asset: Asset;
  onOpenKiln?: (kilnId: string) => void;
}) {
  const queryClient = useQueryClient();
  const [takingDown, setTakingDown] = useState(false);
  const [reason, setReason] = useState('');
  const availability = useMutation({
    ...setAssetAvailabilityMutation(),
    onSuccess: async () => {
      setTakingDown(false);
      setReason('');
      await queryClient.invalidateQueries();
    },
  });

  const kilnId = asset.kilnId;

  return (
    <Card>
      <SectionTitle>{asset.name}</SectionTitle>
      <View style={styles.badges}>
        <Badge>{assetKindLabel(asset.kind)}</Badge>
        {asset.location && <Badge>{asset.location}</Badge>}
        {asset.totalFirings !== undefined && <Badge>{String(asset.totalFirings)} firings</Badge>}
        <Badge warning={!asset.available}>
          {asset.available ? 'In service' : `Out of service: ${asset.downReason ?? ''}`}
        </Badge>
      </View>

      {asset.tasks.length === 0 ? (
        <Body muted>No recurring tasks.</Body>
      ) : (
        <View style={styles.group}>
          {asset.tasks.map((task) => {
            const due = dueText(task);
            return (
              <View key={task.id} style={styles.taskRow}>
                <Body>{task.title}</Body>
                {due ? <Badge warning={task.status !== 'OK'}>{due}</Badge> : null}
              </View>
            );
          })}
        </View>
      )}

      {asset.kind === 'KILN' && kilnId && onOpenKiln && (
        <Button compact label="Open kiln" onPress={() => onOpenKiln(kilnId)} />
      )}

      {asset.kind !== 'KILN' && (
        <View style={styles.group}>
          {asset.available ? (
            takingDown ? (
              <View style={styles.group}>
                <Field
                  label={`Reason ${asset.name} is out of service`}
                  value={reason}
                  onChangeText={setReason}
                  maxLength={200}
                />
                <View style={styles.rowWrap}>
                  <Button
                    compact
                    primary
                    label="Confirm out of service"
                    disabled={availability.isPending || !reason.trim()}
                    onPress={() =>
                      availability.mutate({
                        path: { studioId, assetId: asset.id },
                        body: { downReason: reason.trim(), expectedVersion: asset.version },
                      })
                    }
                  />
                  <Button compact label="Cancel" onPress={() => setTakingDown(false)} />
                </View>
              </View>
            ) : (
              <Button compact label="Take out of service" onPress={() => setTakingDown(true)} />
            )
          ) : (
            <Button
              compact
              primary
              label="Put back in service"
              disabled={availability.isPending}
              onPress={() =>
                availability.mutate({
                  path: { studioId, assetId: asset.id },
                  body: { expectedVersion: asset.version },
                })
              }
            />
          )}
          {availability.isError && <ErrorText error={availability.error} />}
        </View>
      )}
    </Card>
  );
}

const styles = StyleSheet.create({
  content: { gap: 12, paddingBottom: 32 },
  section: { gap: 10 },
  badges: { flexDirection: 'row', flexWrap: 'wrap', gap: 6 },
  group: { gap: 6, marginTop: 4 },
  rowWrap: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  taskRow: { gap: 4, paddingVertical: 2 },
});
