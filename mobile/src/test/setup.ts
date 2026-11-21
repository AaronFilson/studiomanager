import { configure } from '@testing-library/react-native';

import { cleanupTestQueries } from './api';

// findBy and waitFor give up after 1 s by default, too soon on a cold CI runner
configure({ asyncUtilTimeout: 10000 });

afterEach(async () => {
  await cleanupTestQueries();
});
