import {
  createPurchaseRequestMutation,
  createSupplyMutation,
  decidePurchaseRequestMutation,
  getStudioOptions,
  listMembersOptions,
  listPurchaseRequestsOptions,
  listSuppliesOptions,
  type PurchaseRequest,
  recordStockMovementMutation,
  sellSupplyMutation,
  type SupplyCategory,
  type SupplyItem,
} from '@studiomanager/api-client';
import { useMutation, useQuery, useQueryClient, useSuspenseQuery } from '@tanstack/react-query';
import { createFileRoute } from '@tanstack/react-router';
import { type SubmitEvent, useId, useState } from 'react';

import { ErrorMessage } from '../../../components/ErrorMessage';
import { formatMoney, isStaff } from '../../../labels';

export const Route = createFileRoute('/studios/$studioId/supplies')({
  component: SuppliesPage,
});

const categories: SupplyCategory[] = ['CLAY', 'GLAZE', 'GLAZE_MATERIAL', 'TOOL', 'KILN_SUPPLY', 'CLEANING', 'OTHER'];
const categoryLabels: Record<SupplyCategory, string> = {
  CLAY: 'Clay',
  GLAZE: 'Glaze',
  GLAZE_MATERIAL: 'Glaze material',
  TOOL: 'Tool',
  KILN_SUPPLY: 'Kiln supply',
  CLEANING: 'Cleaning',
  OTHER: 'Other',
};

function SuppliesPage() {
  const { studioId } = Route.useParams();
  const { data: studio } = useSuspenseQuery(getStudioOptions({ path: { studioId } }));
  const staff = isStaff(studio.myRole);
  const items = useQuery(listSuppliesOptions({ path: { studioId } }));

  return (
    <div className="stack">
      <h1>Clay and supplies</h1>
      {items.isError && <ErrorMessage error={items.error} />}
      {items.isSuccess && items.data.length === 0 && <p>Nothing is stocked yet.</p>}
      <ul className="studio-list">
        {items.data?.map((item) => (
          <ItemRow key={item.id} studioId={studioId} item={item} staff={staff} currency={studio.currency} />
        ))}
      </ul>
      {staff && <Requests studioId={studioId} canApprove={studio.myRole === 'OWNER'} />}
      {staff && <AddItemForm studioId={studioId} currency={studio.currency} />}
    </div>
  );
}

function ItemRow({ studioId, item, staff, currency }: { studioId: string; item: SupplyItem; staff: boolean; currency: string }) {
  const id = useId();
  const queryClient = useQueryClient();
  const onSuccess = () => queryClient.invalidateQueries();
  const move = useMutation({ ...recordStockMovementMutation(), onSuccess });
  const request = useMutation({ ...createPurchaseRequestMutation(), onSuccess });
  const sell = useMutation({ ...sellSupplyMutation(), onSuccess });
  const members = useQuery({ ...listMembersOptions({ path: { studioId } }), enabled: staff && item.salePrice !== undefined });
  const [quantity, setQuantity] = useState('1');
  const [buyer, setBuyer] = useState('');
  const error = move.error ?? request.error ?? sell.error;

  return (
    <li>
      <div className="stack">
        <div>
          <strong>{item.name}</strong> <span className="badge">{categoryLabels[item.category]}</span>
          <div>
            {item.onHand} × {item.unit}
            {item.low && <strong className="warning"> · low{item.needsOrdering ? ', not on order' : `, ${item.onOrder} on order`}</strong>}
            {item.salePrice && <span className="badge"> · {formatMoney(item.salePrice, currency)} each</span>}
          </div>
          {item.low && item.substitute && <div className="badge">{item.substitute}</div>}
        </div>
        {staff && (
          <div className="row">
            <div className="field">
              <label htmlFor={`${id}-qty`}>Quantity</label>
              <input id={`${id}-qty`} inputMode="decimal" value={quantity} onChange={(e) => setQuantity(e.target.value.trim())} />
            </div>
            <button type="button" onClick={() => move.mutate({ path: { studioId, itemId: item.id }, body: { kind: 'RECEIVED', quantity } })}>
              Received
            </button>
            <button type="button" onClick={() => move.mutate({ path: { studioId, itemId: item.id }, body: { kind: 'USED', quantity } })}>
              Used
            </button>
            <button type="button" onClick={() => move.mutate({ path: { studioId, itemId: item.id }, body: { kind: 'COUNTED', quantity } })}>
              Counted
            </button>
            {item.needsOrdering && (
              <button type="button" onClick={() => request.mutate({ path: { studioId }, body: { itemId: item.id } })}>
                Request more
              </button>
            )}
            {item.salePrice && (
              <>
                <select aria-label={`Sell ${item.name} to`} value={buyer} onChange={(e) => setBuyer(e.target.value)}>
                  <option value="">Sell to…</option>
                  {members.data?.map((m) => (
                    <option key={m.accountId} value={m.accountId}>
                      {m.displayName ?? m.accountId}
                    </option>
                  ))}
                </select>
                <button
                  type="button"
                  disabled={!buyer}
                  onClick={() => sell.mutate({ path: { studioId, itemId: item.id }, body: { memberId: buyer, quantity } })}
                >
                  Sell
                </button>
              </>
            )}
          </div>
        )}
        {error && <ErrorMessage error={error} />}
      </div>
    </li>
  );
}

function Requests({ studioId, canApprove }: { studioId: string; canApprove: boolean }) {
  const queryClient = useQueryClient();
  const requests = useQuery(listPurchaseRequestsOptions({ path: { studioId } }));
  const decide = useMutation({ ...decidePurchaseRequestMutation(), onSuccess: () => queryClient.invalidateQueries() });
  if (!requests.isSuccess || requests.data.length === 0) {
    return null;
  }
  const act = (r: PurchaseRequest, to: 'APPROVED' | 'REJECTED' | 'ORDERED' | 'RECEIVED') =>
    decide.mutate({ path: { studioId, requestId: r.id }, body: { to, expectedVersion: r.version } });

  return (
    <section className="card stack" aria-label="Purchase requests">
      <h2>Purchase requests</h2>
      <ul className="studio-list">
        {requests.data.map((r) => (
          <li key={r.id}>
            <span>
              {r.quantity} × {r.itemName} · {r.status.toLowerCase()}
              {r.requestedByName && <span className="badge"> · asked by {r.requestedByName}</span>}
            </span>
            <span className="row">
              {r.status === 'REQUESTED' && canApprove && (
                <>
                  <button type="button" onClick={() => act(r, 'APPROVED')}>
                    Approve
                  </button>
                  <button type="button" onClick={() => act(r, 'REJECTED')}>
                    Reject
                  </button>
                </>
              )}
              {r.status === 'APPROVED' && (
                <button type="button" onClick={() => act(r, 'ORDERED')}>
                  Ordered
                </button>
              )}
              {(r.status === 'APPROVED' || r.status === 'ORDERED') && (
                <button type="button" onClick={() => act(r, 'RECEIVED')}>
                  Received
                </button>
              )}
            </span>
          </li>
        ))}
      </ul>
      {decide.isError && <ErrorMessage error={decide.error} />}
    </section>
  );
}

function AddItemForm({ studioId, currency }: { studioId: string; currency: string }) {
  const id = useId();
  const queryClient = useQueryClient();
  const [name, setName] = useState('');
  const [category, setCategory] = useState<SupplyCategory>('CLAY');
  const [unit, setUnit] = useState('');
  const [supplier, setSupplier] = useState('');
  const [par, setPar] = useState('');
  const [reorder, setReorder] = useState('');
  const [price, setPrice] = useState('');
  const create = useMutation({
    ...createSupplyMutation(),
    onSuccess: async () => {
      setName('');
      setUnit('');
      await queryClient.invalidateQueries();
    },
  });

  function submit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    create.mutate({
      path: { studioId },
      body: {
        name: name.trim(),
        category,
        unit: unit.trim(),
        ...(supplier.trim() ? { supplier: supplier.trim() } : {}),
        ...(par ? { parLevel: par } : {}),
        ...(reorder ? { reorderQuantity: reorder } : {}),
        ...(price ? { salePrice: price } : {}),
      },
    });
  }

  return (
    <form className="card stack" onSubmit={submit} aria-labelledby={`${id}-title`}>
      <h2 id={`${id}-title`}>Add an item</h2>
      <div className="row">
        <div className="field">
          <label htmlFor={`${id}-name`}>Name</label>
          <input id={`${id}-name`} required maxLength={80} value={name} onChange={(e) => setName(e.target.value)} />
        </div>
        <div className="field">
          <label htmlFor={`${id}-category`}>Kind</label>
          <select id={`${id}-category`} value={category} onChange={(e) => setCategory(e.target.value as SupplyCategory)}>
            {categories.map((c) => (
              <option key={c} value={c}>
                {categoryLabels[c]}
              </option>
            ))}
          </select>
        </div>
        <div className="field">
          <label htmlFor={`${id}-unit`}>Unit</label>
          <input id={`${id}-unit`} required maxLength={30} placeholder="25 lb bag" value={unit} onChange={(e) => setUnit(e.target.value)} />
        </div>
      </div>
      <div className="row">
        <div className="field">
          <label htmlFor={`${id}-supplier`}>Supplier</label>
          <input id={`${id}-supplier`} maxLength={80} value={supplier} onChange={(e) => setSupplier(e.target.value)} />
        </div>
        <div className="field">
          <label htmlFor={`${id}-par`}>Reorder at</label>
          <input id={`${id}-par`} inputMode="decimal" value={par} onChange={(e) => setPar(e.target.value.trim())} />
        </div>
        <div className="field">
          <label htmlFor={`${id}-reorder`}>Order this many</label>
          <input id={`${id}-reorder`} inputMode="decimal" value={reorder} onChange={(e) => setReorder(e.target.value.trim())} />
        </div>
        <div className="field">
          <label htmlFor={`${id}-price`}>Price to members ({currency})</label>
          <input id={`${id}-price`} inputMode="decimal" value={price} onChange={(e) => setPrice(e.target.value.trim())} />
        </div>
      </div>
      {create.isError && <ErrorMessage error={create.error} />}
      <div>
        <button type="submit" className="primary" disabled={create.isPending}>
          Add item
        </button>
      </div>
    </form>
  );
}
