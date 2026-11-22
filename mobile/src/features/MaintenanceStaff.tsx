import {
  type Asset,
  cappedNote,
  getStudioOptions,
  listAssetsOptions,
  listMaintenanceRecordsOptions,
  listMaintenanceRecordsQueryKey,
  listProblemReportsOptions,
  type MaintenanceRecord,
  type ProblemReport,
  recordRepairMutation,
  resolveProblemReportMutation,
  setAssetAvailabilityMutation,
  uploadRecordPhoto,
  useCan,
} from '@studiomanager/api-client';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Image, RefreshControl, ScrollView, StyleSheet, View } from 'react-native';

import { assetKindLabel, dueText, formatDateTime, formatMoney } from '../labels';
import { pickPhoto } from '../photos';
import { Badge, Body, Button, Card, ErrorText, Field, Loading, SectionTitle, Title } from '../ui';

const MAX_RECORD_PHOTOS = 4;
const decimal = /^\d{1,9}(\.\d{1,4})?$/;

/** Staff maintenance view: open problem reports to resolve, and equipment assets with repairs, history, and photos. */
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
  const [loggingRepair, setLoggingRepair] = useState(false);
  const [repairNote, setRepairNote] = useState('');
  const [repairCost, setRepairCost] = useState('');
  const [showHistory, setShowHistory] = useState(false);

  const availability = useMutation({
    ...setAssetAvailabilityMutation(),
    onSuccess: async () => {
      setTakingDown(false);
      setReason('');
      await queryClient.invalidateQueries();
    },
  });

  const repair = useMutation({
    ...recordRepairMutation(),
    onSuccess: async () => {
      setLoggingRepair(false);
      setRepairNote('');
      setRepairCost('');
      setShowHistory(true);
      await queryClient.invalidateQueries({
        queryKey: listMaintenanceRecordsQueryKey({ path: { studioId, assetId: asset.id } }),
      });
    },
  });

  const kilnId = asset.kilnId;
  const costTrimmed = repairCost.trim();
  const costValid = !costTrimmed || (decimal.test(costTrimmed) && Number(costTrimmed) >= 0);

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

      <View style={styles.rowWrap}>
        <Button
          compact
          selected={showHistory}
          label={showHistory ? 'Hide history' : 'History & photos'}
          onPress={() => setShowHistory((open) => !open)}
        />
        {!loggingRepair && (
          <Button
            compact
            label="Log a repair"
            onPress={() => {
              repair.reset();
              setLoggingRepair(true);
            }}
          />
        )}
      </View>

      {loggingRepair && (
        <View style={styles.group}>
          <Field
            label="Repair or other work"
            value={repairNote}
            onChangeText={setRepairNote}
            maxLength={1000}
            multiline
            placeholder="Replaced drive belt and tested pedal"
          />
          <Field
            label="Cost (optional)"
            value={repairCost}
            onChangeText={setRepairCost}
            keyboardType="decimal-pad"
            placeholder="0.00"
          />
          {!costValid && <Body>Enter cost as a number, or leave blank.</Body>}
          {repair.isError && <ErrorText error={repair.error} />}
          <View style={styles.rowWrap}>
            <Button
              compact
              primary
              label={repair.isPending ? 'Saving…' : 'Save repair'}
              disabled={repair.isPending || !repairNote.trim() || !costValid}
              onPress={() =>
                repair.mutate({
                  path: { studioId, assetId: asset.id },
                  body: {
                    note: repairNote.trim(),
                    ...(costTrimmed ? { cost: costTrimmed } : {}),
                  },
                })
              }
            />
            <Button
              compact
              label="Cancel"
              disabled={repair.isPending}
              onPress={() => {
                repair.reset();
                setLoggingRepair(false);
              }}
            />
          </View>
        </View>
      )}

      {showHistory && <AssetHistory studioId={studioId} assetId={asset.id} />}
    </Card>
  );
}

function AssetHistory({ studioId, assetId }: { studioId: string; assetId: string }) {
  const records = useQuery(listMaintenanceRecordsOptions({ path: { studioId, assetId } }));
  const hasCost = records.data?.items.some((r) => Boolean(r.cost)) ?? false;
  const studio = useQuery({ ...getStudioOptions({ path: { studioId } }), enabled: hasCost });
  const currency = studio.data?.currency ?? 'USD';

  const capNote = cappedNote(
    records.data,
    { order: 'newest', noun: 'records' },
    'Export maintenance history on the web to see older ones.',
  );

  return (
    <View style={styles.history}>
      {records.isPending && <Loading />}
      {records.isError && <ErrorText error={records.error} />}
      {capNote && <Body muted>{capNote}</Body>}
      {records.isSuccess && records.data.items.length === 0 && <Body muted>Nothing logged yet.</Body>}
      {records.data?.items.map((record) => (
        <MaintenanceRecordRow
          key={record.id}
          studioId={studioId}
          assetId={assetId}
          record={record}
          currency={currency}
        />
      ))}
    </View>
  );
}

function MaintenanceRecordRow({
  studioId,
  assetId,
  record,
  currency,
}: {
  studioId: string;
  assetId: string;
  record: MaintenanceRecord;
  currency: string;
}) {
  const queryClient = useQueryClient();
  const addPhoto = useMutation({
    mutationFn: async (source: 'camera' | 'library') => {
      const photo = await pickPhoto(source);
      if (!photo) return false;
      await uploadRecordPhoto(studioId, record.id, photo);
      return true;
    },
    onSuccess: async (uploaded) => {
      if (uploaded) {
        await queryClient.invalidateQueries({
          queryKey: listMaintenanceRecordsQueryKey({ path: { studioId, assetId } }),
        });
      }
    },
  });

  const title = record.taskTitle ?? 'Repair';
  const metaParts = [
    formatDateTime(record.doneAt),
    record.doneByName ? `${record.doneByName}${record.viaToken ? ` via ${record.viaToken}` : ''}` : undefined,
    record.atFiring !== undefined ? `at firing ${String(record.atFiring)}` : undefined,
    record.cost ? formatMoney(record.cost, currency) : undefined,
  ].filter(Boolean);

  return (
    <View style={styles.recordRow}>
      <Body>{title}</Body>
      <Body muted>{metaParts.join(' · ')}</Body>
      {record.note && <Body>{record.note}</Body>}
      {record.checked.length > 0 && (
        <Badge>
          {String(record.checked.length)} {record.checked.length === 1 ? 'item' : 'items'} checked
        </Badge>
      )}
      {record.parts.length > 0 && (
        <Body muted>
          Parts: {record.parts.map((p) => `${p.quantity} ${p.unit} ${p.itemName}`.replace(/\s+/g, ' ')).join('; ')}
        </Body>
      )}
      {record.photos.length > 0 && (
        <View style={styles.photoGrid}>
          {record.photos.map((photo, index) => (
            <Image
              key={photo.id}
              source={{ uri: photo.thumbnailUrl }}
              style={styles.recordPhoto}
              accessibilityLabel={`Photo ${String(index + 1)} of ${title}`}
            />
          ))}
        </View>
      )}
      {record.photos.length < MAX_RECORD_PHOTOS && (
        <View style={styles.rowWrap}>
          <Button compact label="Take a photo" disabled={addPhoto.isPending} onPress={() => addPhoto.mutate('camera')} />
          <Button
            compact
            label="Choose a photo"
            disabled={addPhoto.isPending}
            onPress={() => addPhoto.mutate('library')}
          />
        </View>
      )}
      {addPhoto.isPending && <Body muted>Uploading the photo…</Body>}
      {addPhoto.isError && <ErrorText error={addPhoto.error} />}
    </View>
  );
}

const styles = StyleSheet.create({
  content: { gap: 12, paddingBottom: 32 },
  section: { gap: 10 },
  badges: { flexDirection: 'row', flexWrap: 'wrap', gap: 6 },
  group: { gap: 6, marginTop: 4 },
  rowWrap: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginTop: 4 },
  taskRow: { gap: 4, paddingVertical: 2 },
  history: { gap: 10, marginTop: 8 },
  recordRow: { gap: 6, paddingTop: 8 },
  photoGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  recordPhoto: { width: 96, height: 96, borderRadius: 8 },
});
