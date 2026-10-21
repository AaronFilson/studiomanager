import { completeMaintenanceTaskMutation, type MaintenanceTask } from '@studiomanager/api-client';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useId, useState } from 'react';

import { dueText, dutyLabel, formatDate, scheduleText } from '../labels';
import { ErrorMessage } from './ErrorMessage';

/**
 * One recurring task: what it is, how often, who does it and when it's due, with a form to mark
 * it done. Staff see it on the Maintenance page; whoever it's assigned to sees it on their home page.
 */
export function MaintenanceTaskRow({
  studioId,
  task,
  showAsset = false,
}: {
  studioId: string;
  task: MaintenanceTask;
  showAsset?: boolean;
}) {
  const id = useId();
  const queryClient = useQueryClient();
  const [open, setOpen] = useState(false);
  const [checked, setChecked] = useState<Set<string>>(new Set());
  const [note, setNote] = useState('');
  const [cost, setCost] = useState('');
  const complete = useMutation({
    ...completeMaintenanceTaskMutation(),
    onSuccess: async () => {
      setOpen(false);
      setChecked(new Set());
      setNote('');
      setCost('');
      await queryClient.invalidateQueries();
    },
  });
  const assignee = task.assignedDuty ? `any ${dutyLabel(task.assignedDuty).toLowerCase()}` : task.assignedToName;
  const due = dueText(task);

  function toggle(line: string) {
    setChecked((current) => {
      const next = new Set(current);
      if (next.has(line)) next.delete(line);
      else next.add(line);
      return next;
    });
  }

  return (
    <li className="stack">
      <div>
        <strong>
          {showAsset && `${task.assetName}: `}
          {task.title}
        </strong>{' '}
        <span className={task.status === 'OK' ? 'badge' : 'badge warning'}>
          {scheduleText(task)}
          {due && ` · ${due}`}
        </span>
        <div className="badge">
          Last done {formatDate(task.lastDoneAt)}
          {assignee && ` · for ${assignee}`}
        </div>
      </div>
      {!open ? (
        <div>
          <button type="button" onClick={() => setOpen(true)}>
            Mark done
          </button>
        </div>
      ) : (
        <form
          className="stack"
          aria-label={`Mark ${task.title} done`}
          onSubmit={(event) => {
            event.preventDefault();
            complete.mutate({
              path: { studioId, taskId: task.id },
              body: {
                checked: [...checked],
                ...(note.trim() ? { note: note.trim() } : {}),
                ...(cost ? { cost } : {}),
              },
            });
          }}
        >
          {task.checklist.length > 0 && (
            <fieldset>
              <legend>Checklist</legend>
              {task.checklist.map((line) => (
                <label key={line} className="choice">
                  <input type="checkbox" checked={checked.has(line)} onChange={() => toggle(line)} />
                  {line}
                </label>
              ))}
            </fieldset>
          )}
          <div className="row">
            <div className="field">
              <label htmlFor={`${id}-note`}>Notes (optional)</label>
              <input id={`${id}-note`} maxLength={1000} value={note} onChange={(e) => setNote(e.target.value)} />
            </div>
            <div className="field">
              <label htmlFor={`${id}-cost`}>Cost (optional)</label>
              <input id={`${id}-cost`} inputMode="decimal" value={cost} onChange={(e) => setCost(e.target.value.trim())} />
            </div>
          </div>
          {complete.isError && <ErrorMessage error={complete.error} />}
          <div className="row">
            <button type="submit" className="primary" disabled={complete.isPending}>
              Done
            </button>
            <button type="button" onClick={() => setOpen(false)}>
              Cancel
            </button>
          </div>
        </form>
      )}
    </li>
  );
}
