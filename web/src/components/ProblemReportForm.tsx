import { listReportableAssetsOptions, reportProblemMutation } from '@studiomanager/api-client';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { type SubmitEvent, useId, useState } from 'react';

import { ErrorMessage } from './ErrorMessage';

/**
 * "Something's wrong with…", for anyone in the studio. The staff and technicians get a notice.
 * Starts closed, so it doesn't crowd the page for someone with nothing to report.
 */
export function ProblemReportForm({ studioId }: { studioId: string }) {
  const id = useId();
  const queryClient = useQueryClient();
  const [open, setOpen] = useState(false);
  const [assetId, setAssetId] = useState('');
  const [description, setDescription] = useState('');
  const assets = useQuery({ ...listReportableAssetsOptions({ path: { studioId } }), enabled: open });
  const report = useMutation({
    ...reportProblemMutation(),
    onSuccess: async () => {
      setAssetId('');
      setDescription('');
      await queryClient.invalidateQueries();
    },
  });

  if (!open) {
    return (
      <div>
        <button type="button" onClick={() => setOpen(true)}>
          Report a problem
        </button>
        {report.isSuccess && <p aria-live="polite">Thanks, the staff have been told.</p>}
      </div>
    );
  }

  function submit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    report.mutate(
      { path: { studioId }, body: { description: description.trim(), ...(assetId ? { assetId } : {}) } },
      { onSuccess: () => setOpen(false) },
    );
  }

  return (
    <form className="card stack" onSubmit={submit} aria-labelledby={`${id}-title`}>
      <h2 id={`${id}-title`}>Report a problem</h2>
      <div className="field">
        <label htmlFor={`${id}-asset`}>With</label>
        <select id={`${id}-asset`} value={assetId} onChange={(e) => setAssetId(e.target.value)}>
          <option value="">Something else</option>
          {assets.data?.map((a) => (
            <option key={a.id} value={a.id}>
              {a.name}
              {!a.available && ' (already out of service)'}
            </option>
          ))}
        </select>
      </div>
      <div className="field">
        <label htmlFor={`${id}-description`}>What’s wrong</label>
        <textarea
          id={`${id}-description`}
          required
          rows={3}
          maxLength={1000}
          value={description}
          onChange={(e) => setDescription(e.target.value)}
        />
      </div>
      {report.isError && <ErrorMessage error={report.error} />}
      <div className="row">
        <button type="submit" className="primary" disabled={report.isPending || !description.trim()}>
          Send report
        </button>
        <button type="button" onClick={() => setOpen(false)}>
          Cancel
        </button>
      </div>
    </form>
  );
}
