import { acceptInviteMutation, getMeQueryKey } from '@studiomanager/api-client';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { createFileRoute, useNavigate } from '@tanstack/react-router';

import { requireSignIn } from '../../auth/requireSignIn';
import { ErrorMessage } from '../../components/ErrorMessage';

export const Route = createFileRoute('/invites/$code')({
  beforeLoad: requireSignIn,
  pendingComponent: () => <p>Taking you to sign in…</p>,
  component: AcceptInvitePage,
});

function AcceptInvitePage() {
  const { code } = Route.useParams();
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const accept = useMutation({
    ...acceptInviteMutation(),
    onSuccess: async (membership) => {
      await queryClient.invalidateQueries({ queryKey: getMeQueryKey() });
      await navigate({ to: '/studios/$studioId', params: { studioId: membership.studio.id } });
    },
  });

  return (
    <div className="card stack">
      <h1>Join a studio</h1>
      <p>You&apos;ve been invited to join a studio on Studio Manager.</p>
      {accept.isError && <ErrorMessage error={accept.error} />}
      <div>
        <button
          type="button"
          className="primary"
          disabled={accept.isPending}
          onClick={() => accept.mutate({ path: { code } })}
        >
          {accept.isPending ? 'Joining…' : 'Accept the invite'}
        </button>
      </div>
    </div>
  );
}
