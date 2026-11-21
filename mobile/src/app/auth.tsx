import { router } from 'expo-router';
import * as WebBrowser from 'expo-web-browser';
import { useEffect } from 'react';

import { useAuth } from '../auth/AuthProvider';
import { Loading, Screen } from '../ui';

/** Handles studiomanager://auth OAuth redirect callbacks without showing an unmatched route. */
export default function AuthCallbackScreen() {
  const auth = useAuth();

  useEffect(() => {
    WebBrowser.maybeCompleteAuthSession();
  }, []);

  useEffect(() => {
    if (auth.status === 'starting') return;
    if (router.canGoBack()) {
      router.back();
    } else {
      router.replace('/');
    }
  }, [auth.status]);

  return (
    <Screen edges={['top', 'bottom']}>
      <Loading />
    </Screen>
  );
}
