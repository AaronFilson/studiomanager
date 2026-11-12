import { useWakeState } from '../wakeApi';

/** Says the server is starting while the API wakes from idle (plan 23). */
export function ServerWaking() {
  if (useWakeState() !== 'slow') {
    return null;
  }
  return (
    <div className="server-waking" role="status">
      Starting the studio server. After a quiet spell this takes up to a minute.
    </div>
  );
}
