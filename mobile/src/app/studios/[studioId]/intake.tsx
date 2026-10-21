import { getStudioOptions } from '@studiomanager/api-client';
import { useQuery } from '@tanstack/react-query';
import { useLocalSearchParams } from 'expo-router';

import { IntakeForm } from '../../../features/IntakeForm';
import { isStaff } from '../../../labels';
import { ErrorText, Loading, Screen } from '../../../ui';

export default function IntakeScreen() {
  const { studioId } = useLocalSearchParams<{ studioId: string }>();
  const studio = useQuery(getStudioOptions({ path: { studioId } }));

  return (
    <Screen>
      {studio.isPending && <Loading />}
      {studio.isError && <ErrorText error={studio.error} />}
      {studio.isSuccess && (
        <IntakeForm studioId={studioId} unitSystem={studio.data.unitSystem} staff={isStaff(studio.data.myRole)} />
      )}
    </Screen>
  );
}
