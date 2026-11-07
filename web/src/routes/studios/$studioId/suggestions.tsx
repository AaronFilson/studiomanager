import { decidePendingActionMutation, listPendingActionsOptions, type PendingAction } from '@studiomanager/api-client';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { createFileRoute } from '@tanstack/react-router';
import { useId, useState } from 'react';

import { CappedNote } from '../../../components/CappedNote';
import { ErrorMessage } from '../../../components/ErrorMessage';

export const Route = createFileRoute('/studios/$studioId/suggestions')({
  component: SuggestionsPage,
});

/**
 * Changes an AI assistant proposed over MCP, waiting for staff. Approving makes the change as the
 * person who approves, through the same rules as making it by hand.
 */
function SuggestionsPage() {
  const { studioId } = Route.useParams();
  const waiting = useQuery(listPendingActionsOptions({ path: { studioId } }));

  return (
    <div className="stack">
      <h1>Assistant suggestions</h1>
      <p>
        An assistant connected with a token that has the &ldquo;suggest changes&rdquo; scope can propose these. Nothing changes until
        someone approves.
      </p>
      {waiting.isError && <ErrorMessage error={waiting.error} />}
      {waiting.isSuccess && waiting.data.items.length === 0 && <p>Nothing is waiting.</p>}
      <ul className="studio-list">
        {waiting.data?.items.map((action) => (
          <SuggestionRow key={action.id} studioId={studioId} action={action} />
        ))}
      </ul>
      <CappedNote list={waiting.data} order="oldest" noun="suggestions" findTheRest="Decide these to see the rest." />
    </div>
  );
}

function SuggestionRow({ studioId, action }: { studioId: string; action: PendingAction }) {
  const id = useId();
  const queryClient = useQueryClient();
  const [note, setNote] = useState('');
  const decide = useMutation({ ...decidePendingActionMutation(), onSuccess: () => queryClient.invalidateQueries() });
  const send = (decision: 'APPROVED' | 'REJECTED') =>
    decide.mutate({
      path: { studioId, actionId: action.id },
      body: { decision, expectedVersion: action.version, ...(decision === 'REJECTED' && note.trim() ? { note: note.trim() } : {}) },
    });

  return (
    <li>
      <div className="stack">
        <div>
          <strong>{describe(action)}</strong>
          <div className="badge">
            from {action.proposedByName ?? 'someone'}
            {action.proposedViaTokenName && <>, through &lsquo;{action.proposedViaTokenName}&rsquo;</>} ·{' '}
            {new Date(action.proposedAt).toLocaleString()}
          </div>
          {/* The assistant's own words: plain text, never markup */}
          {action.reason && <p>{action.reason}</p>}
        </div>
        <div className="row">
          <button type="button" className="primary" disabled={decide.isPending} onClick={() => send('APPROVED')}>
            Approve
          </button>
          <div className="field">
            <label htmlFor={`${id}-note`}>Why not (optional)</label>
            <input id={`${id}-note`} maxLength={500} value={note} onChange={(e) => setNote(e.target.value)} />
          </div>
          <button type="button" disabled={decide.isPending} onClick={() => send('REJECTED')}>
            Reject
          </button>
        </div>
        {decide.isError && <ErrorMessage error={decide.error} />}
      </div>
    </li>
  );
}

function describe(action: PendingAction): string {
  switch (action.kind) {
    case 'PURCHASE_REQUEST': {
      const request = action.purchaseRequest;
      const quantity = request ? `${request.quantity} × ` : '';
      const note = request?.note ? ` (${request.note})` : '';
      return `Request ${quantity}${action.subjectName}${note}`;
    }
    case 'MAINTENANCE_TASK': {
      const task = action.maintenanceTask;
      return `Add “${task?.title ?? 'a task'}” on ${action.subjectName}${task ? `, ${interval(task.everyDays, task.everyFirings)}` : ''}`;
    }
  }
}

function interval(everyDays: number | undefined, everyFirings: number | undefined): string {
  const parts = [
    ...(everyDays !== undefined ? [`every ${String(everyDays)} days`] : []),
    ...(everyFirings !== undefined ? [`every ${String(everyFirings)} firings`] : []),
  ];
  return parts.join(' or ');
}
