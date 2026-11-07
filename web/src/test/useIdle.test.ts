import { act, renderHook } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { useIdle } from '../useIdle';

describe('useIdle', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });
  afterEach(() => {
    vi.useRealTimers();
  });

  it('goes idle after the quiet period, and wakes on any key or tap', () => {
    const { result } = renderHook(() => useIdle(1000));
    expect(result.current).toBe(false);

    act(() => {
      vi.advanceTimersByTime(1000);
    });
    expect(result.current).toBe(true);

    act(() => {
      window.dispatchEvent(new KeyboardEvent('keydown'));
    });
    expect(result.current).toBe(false);

    act(() => {
      vi.advanceTimersByTime(999);
    });
    expect(result.current).toBe(false);
  });
});
