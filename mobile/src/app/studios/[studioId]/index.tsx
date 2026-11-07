import { useCan } from '@studiomanager/api-client';
import { router, useLocalSearchParams } from 'expo-router';

import { PieceList } from '../../../features/PieceList';
import { Screen } from '../../../ui';

export default function PiecesScreen() {
  const { studioId } = useLocalSearchParams<{ studioId: string }>();
  const { can } = useCan(studioId);
  // Whoever moves pieces through the studio sees everyone's
  const staff = can('pieces.move');

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
