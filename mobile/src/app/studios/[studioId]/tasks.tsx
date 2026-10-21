import { router, useLocalSearchParams } from 'expo-router';
import { StyleSheet, View } from 'react-native';

import { MyTasks } from '../../../features/MyTasks';
import { Button, Screen } from '../../../ui';

export default function TasksScreen() {
  const { studioId } = useLocalSearchParams<{ studioId: string }>();

  return (
    <Screen>
      <MyTasks
        studioId={studioId}
        footer={
          <View style={styles.footer}>
            <Button label="Report a problem" onPress={() => router.push(`/studios/${studioId}/report`)} />
          </View>
        }
      />
    </Screen>
  );
}

const styles = StyleSheet.create({
  footer: { marginTop: 12 },
});
