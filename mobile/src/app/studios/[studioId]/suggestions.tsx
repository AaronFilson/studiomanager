import { useLocalSearchParams } from 'expo-router';

import { SuggestionList } from '../../../features/SuggestionList';
import { Screen } from '../../../ui';

export default function SuggestionsScreen() {
  const { studioId } = useLocalSearchParams<{ studioId: string }>();
  return (
    <Screen>
      <SuggestionList studioId={studioId} />
    </Screen>
  );
}
