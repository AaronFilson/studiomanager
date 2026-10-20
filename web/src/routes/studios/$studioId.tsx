import { getStudioOptions, isApiError } from '@studiomanager/api-client';
import { useQuery } from '@tanstack/react-query';
import { createFileRoute, Link } from '@tanstack/react-router';

import { ErrorMessage } from '../../components/ErrorMessage';
import { roleLabel, unitSystemLabel } from '../../labels';

export const Route = createFileRoute('/studios/$studioId')({
  component: StudioPage,
});

function StudioPage() {
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

  const { name, myRole, memberCount, unitSystem, timeZone, country, currency } = studio.data;
  const countryName = new Intl.DisplayNames(undefined, { type: 'region' }).of(country) ?? country;
  return (
    <article className="stack">
      <h1>Hello, {name}</h1>
      <dl className="facts">
        <dt>Your role</dt>
        <dd>{roleLabel(myRole)}</dd>
        <dt>Members</dt>
        <dd>{memberCount}</dd>
        <dt>Units</dt>
        <dd>{unitSystemLabel(unitSystem)}</dd>
        <dt>Time zone</dt>
        <dd>{timeZone}</dd>
        <dt>Country</dt>
        <dd>{countryName}</dd>
        <dt>Currency</dt>
        <dd>{currency}</dd>
      </dl>
      <p>
        <Link to="/studios">All your studios</Link>
      </p>
    </article>
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
