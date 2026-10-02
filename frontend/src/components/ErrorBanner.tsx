interface Props {
  errors: string[];
  onDismiss?: () => void;
  title?: string;
}

/** Friendly, accessible error list. Only ever shows messages that are safe for users. */
export default function ErrorBanner({ errors, onDismiss, title = 'Please fix the following' }: Props) {
  if (errors.length === 0) return null;
  return (
    <div role="alert" className="rounded-lg border border-red-500/40 bg-red-950/40 p-4 text-sm text-red-200">
      <div className="flex items-start justify-between gap-4">
        <p className="font-semibold">{errors.length === 1 ? 'Something went wrong' : title}</p>
        {onDismiss && (
          <button type="button" className="text-red-300 hover:text-white" onClick={onDismiss} aria-label="Dismiss errors">
            ✕
          </button>
        )}
      </div>
      <ul className="mt-2 list-disc space-y-1 pl-5 font-mono text-[13px]">
        {errors.slice(0, 8).map((e, i) => (
          <li key={i}>{e}</li>
        ))}
        {errors.length > 8 && <li>…and {errors.length - 8} more</li>}
      </ul>
    </div>
  );
}
