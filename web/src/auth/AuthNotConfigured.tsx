interface Props {
  problems: string[];
}

/** Shown instead of the app when the build has no sign-in settings (e.g. a fresh clone). */
export function AuthNotConfigured({ problems }: Props) {
  return (
    <main className="container">
      <h1>Sign-in is not configured</h1>
      <p>
        This build of Studio Manager doesn&apos;t know which Microsoft Entra External ID tenant to use. Copy{' '}
        <code>web/.env.example</code> to <code>web/.env.local</code>, fill in the values, and restart the dev server.
      </p>
      <ul>
        {problems.map((problem) => (
          <li key={problem}>{problem}</li>
        ))}
      </ul>
    </main>
  );
}
