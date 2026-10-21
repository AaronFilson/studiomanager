import { useLocalSearchParams } from 'expo-router';

import { ProblemReportForm } from '../../../features/ProblemReportForm';
import { Screen } from '../../../ui';

export default function ReportScreen() {
  const { studioId } = useLocalSearchParams<{ studioId: string }>();

  return (
    <Screen>
      <ProblemReportForm studioId={studioId} />
    </Screen>
  );
}
