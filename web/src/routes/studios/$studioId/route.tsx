import { getMyConsignmentOptions, getStudioOptions, isApiError, useCan } from '@studiomanager/api-client';
import { useQuery } from '@tanstack/react-query';
import { createFileRoute, Link, Outlet } from '@tanstack/react-router';

import { ErrorMessage } from '../../../components/ErrorMessage';

/** The frame around every page of one studio: its name, the studio's sections, and access checks. */
export const Route = createFileRoute('/studios/$studioId')({
  component: StudioLayout,
});

function StudioLayout() {
  const { studioId } = Route.useParams();
  const studio = useQuery(getStudioOptions({ path: { studioId } }));
  // Loaded here, before any page of the studio renders, so every page's can() has its answer
  const permissions = useCan(studioId);
  const { can } = permissions;
  // Members who sell work here see their own consignment; a 404 just means they don't.
  // Whoever reads the studio's money sees consignment there instead.
  const consignment = useQuery({
    ...getMyConsignmentOptions({ path: { studioId } }),
    enabled: studio.isSuccess && !permissions.isPending && !can('money.read'),
    retry: false,
  });

  if (studio.isPending || (studio.isSuccess && permissions.isPending)) {
    return <p>Loading studio…</p>;
  }
  if (studio.isError) {
    if (isApiError(studio.error) && studio.error.status === 403) {
      return <NoAccess />;
    }
    return <ErrorMessage error={studio.error} />;
  }
  if (permissions.isError) {
    return <ErrorMessage error={permissions.error} />;
  }

  return (
    <div className="stack">
      <nav aria-label={studio.data.name} className="studio-nav">
        <Link to="/studios/$studioId" params={{ studioId }} activeOptions={{ exact: true }}>
          {studio.data.name}
        </Link>
        {can('today.read') && (
          <Link to="/studios/$studioId/today" params={{ studioId }}>
            Today
          </Link>
        )}
        <Link to="/studios/$studioId/pieces" params={{ studioId }}>
          {can('pieces.move') ? 'Pieces' : 'My pieces'}
        </Link>
        <Link to="/studios/$studioId/calendar" params={{ studioId }}>
          Calendar
        </Link>
        <Link to="/studios/$studioId/roster" params={{ studioId }}>
          Roster
        </Link>
        <Link to="/studios/$studioId/glazes" params={{ studioId }}>
          Glaze book
        </Link>
        {can('kilns.manage') && (
          <Link to="/studios/$studioId/kilns" params={{ studioId }}>
            Kilns
          </Link>
        )}
        {can('maintenance.manage') && (
          <Link to="/studios/$studioId/maintenance" params={{ studioId }}>
            Maintenance
          </Link>
        )}
        {can('supplies.manage') && (
          <Link to="/studios/$studioId/supplies" params={{ studioId }}>
            Supplies
          </Link>
        )}
        {can('proposals.review') && (
          <Link to="/studios/$studioId/suggestions" params={{ studioId }}>
            Assistant suggestions
          </Link>
        )}
        {can('money.read') && (
          <Link to="/studios/$studioId/money" params={{ studioId }}>
            Money
          </Link>
        )}
        {can('money.payables.manage') && (
          <Link to="/studios/$studioId/payables" params={{ studioId }}>
            Artists and instructors
          </Link>
        )}
        {can('members.read') && (
          <Link to="/studios/$studioId/members" params={{ studioId }}>
            Members
          </Link>
        )}
        {can('imports.run') && (
          <Link to="/studios/$studioId/import" params={{ studioId }}>
            Import
          </Link>
        )}
        {can('studio.settings.edit') && (
          <Link to="/studios/$studioId/settings" params={{ studioId }}>
            Settings
          </Link>
        )}
        {can('webhooks.manage') && (
          <Link to="/studios/$studioId/webhooks" params={{ studioId }}>
            Webhooks
          </Link>
        )}
        {can('activity.read') && (
          <Link to="/studios/$studioId/activity" params={{ studioId }}>
            Activity
          </Link>
        )}
        {!can('money.read') && consignment.isSuccess && (
          <Link to="/studios/$studioId/my-consignment" params={{ studioId }}>
            My work for sale
          </Link>
        )}
      </nav>
      <Outlet />
    </div>
  );
}

function NoAccess() {
  return (
    <div className="stack">
      <h1>No access</h1>
      <p role="alert" className="error">
        You don&apos;t have access to this studio. If you should, ask the studio&apos;s owner to invite you.
      </p>
      <p>
        <Link to="/studios">Back to your studios</Link>
      </p>
    </div>
  );
}
