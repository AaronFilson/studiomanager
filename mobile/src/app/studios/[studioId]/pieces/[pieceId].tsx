import { getStudioOptions } from '@studiomanager/api-client';
import { useQuery } from '@tanstack/react-query';
import { useLocalSearchParams } from 'expo-router';

import { PieceDetail } from '../../../../features/PieceDetail';
import { isStaff } from '../../../../labels';
import { Screen } from '../../../../ui';

export default function PieceScreen() {
  const { studioId, pieceId } = useLocalSearchParams<{ studioId: string; pieceId: string }>();
  const studio = useQuery(getStudioOptions({ path: { studioId } }));

  return (
    <Screen>
      <PieceDetail studioId={studioId} pieceId={pieceId} staff={studio.data ? isStaff(studio.data.myRole) : false} />
    </Screen>
  );
}
