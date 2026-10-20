import { getMeOptions, type Membership } from '@studiomanager/api-client';
import { useQuery } from '@tanstack/react-query';
import { createFileRoute, Link } from '@tanstack/react-router';

import { CreateStudioForm } from '../../components/CreateStudioForm';
import { ErrorMessage } from '../../components/ErrorMessage';
import { roleLabel } from '../../labels';

export const Route = createFileRoute('/studios/')({
  component: StudiosPage,
});

function StudiosPage() {
  return (
    <div className="stack">
      <h1>Your studios</h1>
      <Memberships />
      <CreateStudioForm />
    </div>
  );
}

function Memberships() {
  const me = useQuery(getMeOptions());

  if (me.isPending) {
    return <p>Loading your studios…</p>;
  }
  if (me.isError) {
    return <ErrorMessage error={me.error} />;
  }
  if (me.data.memberships.length === 0) {
    return <p>You&apos;re not in any studios yet. Create one below, or ask your studio for an invite.</p>;
  }
  return (
    <ul className="studio-list">
      {me.data.memberships.map((membership: Membership) => (
        <li key={membership.studio.id}>
          <Link to="/studios/$studioId" params={{ studioId: membership.studio.id }}>
            {membership.studio.name}
          </Link>
          <span className="badge">{roleLabel(membership.role)}</span>
        </li>
      ))}
    </ul>
  );
}
