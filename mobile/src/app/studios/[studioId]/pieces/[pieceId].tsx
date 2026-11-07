import { useCan } from '@studiomanager/api-client';
import { useLocalSearchParams } from 'expo-router';

import { PieceDetail } from '../../../../features/PieceDetail';
import { Screen } from '../../../../ui';

export default function PieceScreen() {
  const { studioId, pieceId } = useLocalSearchParams<{ studioId: string; pieceId: string }>();
  const { can } = useCan(studioId);

  return (
    <Screen>
      <PieceDetail studioId={studioId} pieceId={pieceId} staff={can('pieces.move')} />
    </Screen>
  );
}
