import { getStudioOptions } from '@studiomanager/api-client';
import { useQuery } from '@tanstack/react-query';
import { router, useLocalSearchParams } from 'expo-router';

import { PieceList } from '../../../features/PieceList';
import { isStaff } from '../../../labels';
import { Screen } from '../../../ui';

export default function PiecesScreen() {
  const { studioId } = useLocalSearchParams<{ studioId: string }>();
  const studio = useQuery(getStudioOptions({ path: { studioId } }));
  const staff = studio.data ? isStaff(studio.data.myRole) : false;

  return (
    <Screen>
      <PieceList
        studioId={studioId}
        staff={staff}
        onOpen={(pieceId) => router.push(`/studios/${studioId}/pieces/${pieceId}`)}
      />
    </Screen>
  );
}
