import { getStudioOptions, useCan } from '@studiomanager/api-client';
import { useQuery } from '@tanstack/react-query';
import { useLocalSearchParams } from 'expo-router';

import { IntakeForm } from '../../../features/IntakeForm';
import { ErrorText, Loading, Screen } from '../../../ui';

export default function IntakeScreen() {
  const { studioId } = useLocalSearchParams<{ studioId: string }>();
  const studio = useQuery(getStudioOptions({ path: { studioId } }));
  const permissions = useCan(studioId);

  return (
    <Screen>
      {(studio.isPending || permissions.isPending) && <Loading />}
      {studio.isError && <ErrorText error={studio.error} />}
      {studio.isSuccess && !permissions.isPending && (
        <IntakeForm
          studioId={studioId}
          unitSystem={studio.data.unitSystem}
          staff={permissions.can('pieces.intake_for_others')}
        />
      )}
    </Screen>
  );
}
