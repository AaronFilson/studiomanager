import { useEffect, useState } from 'react';

const activityEvents = ['pointerdown', 'pointermove', 'keydown', 'scroll', 'touchstart'] as const;

/**
 * Whether nobody has used the page for `idleAfterMs`. Any pointer, key, scroll or touch counts as
 * use, and so does the tab becoming visible again.
 */
export function useIdle(idleAfterMs: number): boolean {
  const [idle, setIdle] = useState(false);

  useEffect(() => {
    let timer = window.setTimeout(() => setIdle(true), idleAfterMs);
    const markActive = () => {
      window.clearTimeout(timer);
      setIdle(false);
      timer = window.setTimeout(() => setIdle(true), idleAfterMs);
    };
    const markActiveWhenShown = () => {
      if (document.visibilityState === 'visible') {
        markActive();
      }
    };
    for (const eventName of activityEvents) {
      window.addEventListener(eventName, markActive, { passive: true });
    }
    document.addEventListener('visibilitychange', markActiveWhenShown);
    return () => {
      window.clearTimeout(timer);
      for (const eventName of activityEvents) {
        window.removeEventListener(eventName, markActive);
      }
      document.removeEventListener('visibilitychange', markActiveWhenShown);
    };
  }, [idleAfterMs]);

  return idle;
}
