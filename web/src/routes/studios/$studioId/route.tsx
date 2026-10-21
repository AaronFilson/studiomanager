import { getStudioOptions, isApiError } from '@studiomanager/api-client';
import { useQuery } from '@tanstack/react-query';
import { createFileRoute, Link, Outlet } from '@tanstack/react-router';

import { ErrorMessage } from '../../../components/ErrorMessage';
import { isStaff } from '../../../labels';

/** The frame around every page of one studio: its name, the studio's sections, and access checks. */
export const Route = createFileRoute('/studios/$studioId')({
  component: StudioLayout,
});

function StudioLayout() {
  const { studioId } = Route.useParams();
  const studio = useQuery(getStudioOptions({ path: { studioId } }));

  if (studio.isPending) {
    return <p>Loading studio…</p>;
  }
  if (studio.isError) {
    if (isApiError(studio.error) && studio.error.status === 403) {
      return <NoAccess />;
    }
    return <ErrorMessage error={studio.error} />;
  }

  const staff = isStaff(studio.data.myRole);
  return (
    <div className="stack">
      <nav aria-label={studio.data.name} className="studio-nav">
        <Link to="/studios/$studioId" params={{ studioId }} activeOptions={{ exact: true }}>
          {studio.data.name}
        </Link>
        {staff && (
          <Link to="/studios/$studioId/today" params={{ studioId }}>
            Today
          </Link>
        )}
        <Link to="/studios/$studioId/pieces" params={{ studioId }}>
          {staff ? 'Pieces' : 'My pieces'}
        </Link>
        <Link to="/studios/$studioId/calendar" params={{ studioId }}>
          Calendar
        </Link>
        <Link to="/studios/$studioId/roster" params={{ studioId }}>
          Roster
        </Link>
        {staff && (
          <>
            <Link to="/studios/$studioId/kilns" params={{ studioId }}>
              Kilns
            </Link>
            <Link to="/studios/$studioId/maintenance" params={{ studioId }}>
              Maintenance
            </Link>
            <Link to="/studios/$studioId/supplies" params={{ studioId }}>
              Supplies
            </Link>
            <Link to="/studios/$studioId/money" params={{ studioId }}>
              Money
            </Link>
            <Link to="/studios/$studioId/payables" params={{ studioId }}>
              Artists and instructors
            </Link>
            <Link to="/studios/$studioId/members" params={{ studioId }}>
              Members
            </Link>
            <Link to="/studios/$studioId/settings" params={{ studioId }}>
              Settings
            </Link>
          </>
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
