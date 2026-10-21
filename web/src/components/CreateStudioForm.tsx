import {
  createStudioMutation,
  getMeQueryKey,
  getStudioQueryKey,
  type UnitSystem,
} from '@studiomanager/api-client';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from '@tanstack/react-router';
import { type SubmitEvent, useId, useState } from 'react';

import { unitSystemLabel } from '../labels';
import { ErrorMessage } from './ErrorMessage';

const unitSystems: UnitSystem[] = ['US_CUSTOMARY', 'METRIC'];

function browserTimeZone(): string {
  return Intl.DateTimeFormat().resolvedOptions().timeZone;
}

/** Every ISO 3166-1 alpha-2 code the browser can name, sorted by name. */
function countries(): { code: string; name: string }[] {
  const names = new Intl.DisplayNames(undefined, { type: 'region', fallback: 'none' });
  const letters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';
  const result: { code: string; name: string }[] = [];
  for (const first of letters) {
    for (const second of letters) {
      const code = first + second;
      const name = names.of(code);
      // Skip the user-assigned and reserved codes the browser still names (for example "Unknown Region")
      if (name && name !== code && !/^(AA|Q[M-Z]|X[A-Z]|ZZ)$/.test(code)) result.push({ code, name });
    }
  }
  return result.sort((a, b) => a.name.localeCompare(b.name));
}

function browserCountry(): string {
  return new Intl.Locale(navigator.language).maximize().region ?? 'US';
}

export function CreateStudioForm() {
  const id = useId();
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const [name, setName] = useState('');
  const [unitSystem, setUnitSystem] = useState<UnitSystem>('US_CUSTOMARY');
  const [timeZone, setTimeZone] = useState(browserTimeZone);
  const [timeZones] = useState(() => Intl.supportedValuesOf('timeZone'));
  const [countryOptions] = useState(countries);
  const [country, setCountry] = useState(browserCountry);

  const create = useMutation({
    ...createStudioMutation(),
    onSuccess: async (studio) => {
      queryClient.setQueryData(getStudioQueryKey({ path: { studioId: studio.id } }), studio);
      void queryClient.invalidateQueries({ queryKey: getMeQueryKey() });
      await navigate({ to: '/studios/$studioId', params: { studioId: studio.id } });
    },
  });

  function submit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    // The currency is left to the server, which uses the country's
    create.mutate({ body: { name: name.trim(), unitSystem, timeZone: timeZone.trim(), country } });
  }

  return (
    <form className="card stack" onSubmit={submit} aria-labelledby={`${id}-title`}>
      <h2 id={`${id}-title`}>Create a studio</h2>

      <div className="field">
        <label htmlFor={`${id}-name`}>Studio name</label>
        <input
          id={`${id}-name`}
          value={name}
          onChange={(e) => setName(e.target.value)}
          required
          maxLength={120}
          autoComplete="organization"
        />
      </div>

      <fieldset className="field">
        <legend>Units</legend>
        {unitSystems.map((option) => (
          <label key={option} className="choice">
            <input
              type="radio"
              name={`${id}-units`}
              value={option}
              checked={unitSystem === option}
              onChange={() => setUnitSystem(option)}
            />
            {unitSystemLabel(option)}
          </label>
        ))}
      </fieldset>

      <div className="field">
        <label htmlFor={`${id}-tz`}>Time zone</label>
        <input
          id={`${id}-tz`}
          value={timeZone}
          onChange={(e) => setTimeZone(e.target.value)}
          list={`${id}-tz-list`}
          required
          maxLength={64}
        />
        <datalist id={`${id}-tz-list`}>
          {timeZones.map((zone) => (
            <option key={zone} value={zone} />
          ))}
        </datalist>
      </div>

      <div className="field">
        <label htmlFor={`${id}-country`}>Country</label>
        <select id={`${id}-country`} value={country} onChange={(e) => setCountry(e.target.value)} required>
          {countryOptions.map(({ code, name: countryName }) => (
            <option key={code} value={code}>
              {countryName}
            </option>
          ))}
        </select>
      </div>

      {create.isError && <ErrorMessage error={create.error} />}

      <div>
        <button type="submit" className="primary" disabled={create.isPending}>
          {create.isPending ? 'Creating…' : 'Create studio'}
        </button>
      </div>
    </form>
  );
}
