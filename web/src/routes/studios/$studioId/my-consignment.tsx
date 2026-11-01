import { getMyConsignmentOptions, getStudioOptions } from '@studiomanager/api-client';
import { useQuery, useSuspenseQuery } from '@tanstack/react-query';
import { createFileRoute } from '@tanstack/react-router';

import { ErrorMessage } from '../../../components/ErrorMessage';
import { formatDate, formatMoney } from '../../../labels';

export const Route = createFileRoute('/studios/$studioId/my-consignment')({
  component: MyConsignmentPage,
});

/** An artist's own view of the work the studio sells for them: what's on the shelf, what sold, what they're owed. */
function MyConsignmentPage() {
  const { studioId } = Route.useParams();
  const { data: studio } = useSuspenseQuery(getStudioOptions({ path: { studioId } }));
  const mine = useQuery({ ...getMyConsignmentOptions({ path: { studioId } }), retry: false });
  const money = (amount: string) => formatMoney(amount, studio.currency);
  const day = (date: string) => formatDate(`${date}T12:00:00`);

  return (
    <div className="stack">
      <h1>My work for sale</h1>
      {mine.isPending && <p>Loading…</p>}
      {mine.isError && <ErrorMessage error={mine.error} />}
      {mine.data && (
        <>
          <section className="card stack" aria-label="What you're owed">
            <p>
              The studio owes you <strong>{money(mine.data.owed)}</strong>.
            </p>
          </section>
          <section className="card stack" aria-label="Your work">
            <h2>Your work</h2>
            {mine.data.items.length === 0 && <p>Nothing taken in yet.</p>}
            <ul className="studio-list">
              {mine.data.items.map((item) => (
                <li key={item.id}>
                  <span>
                    {item.description} · {money(item.retailPrice)}
                    {item.dueBack && <span className="badge warning"> · ready to collect</span>}
                  </span>
                  <span>
                    {item.inStock} on the shelf · {item.sold} sold
                    {item.returned > 0 && ` · ${String(item.returned)} returned`}
                    {item.returnBy && item.inStock > 0 && !item.dueBack && ` · until ${day(item.returnBy)}`}
                  </span>
                </li>
              ))}
            </ul>
          </section>
          <section className="card stack" aria-label="Sales">
            <h2>Sales</h2>
            {mine.data.sales.length === 0 && <p>No sales yet.</p>}
            <ol className="timeline">
              {mine.data.sales.map((sale) => (
                <li key={sale.id}>
                  {day(sale.soldOn)} · {sale.quantity} × {sale.description} · {money(sale.consignorAmount)} to you
                  {sale.refundedOn && <span className="badge"> · refunded</span>}
                </li>
              ))}
            </ol>
          </section>
        </>
      )}
    </div>
  );
}
