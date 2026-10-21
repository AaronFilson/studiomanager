import { configure } from '@testing-library/react-native';

// findBy and waitFor give up after 1 s by default, too soon on a cold CI runner
configure({ asyncUtilTimeout: 5000 });
