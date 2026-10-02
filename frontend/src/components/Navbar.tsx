import { NavLink } from 'react-router-dom';
import { useWorkspace } from '../state/WorkspaceContext';

const LINKS = [
  { to: '/', label: 'Home', end: true },
  { to: '/simulator', label: 'Simulator' },
  { to: '/comparison', label: 'Comparison' },
  { to: '/how-it-works', label: 'How It Works' },
  { to: '/about', label: 'About' },
];

/** Top navigation with a live backend status indicator. */
export default function Navbar() {
  const { backend } = useWorkspace();
  const dot = backend === 'online' ? 'bg-emerald-400' : backend === 'offline' ? 'bg-red-400' : 'bg-slate-500';
  const text = backend === 'online' ? 'Backend online' : backend === 'offline' ? 'Backend unreachable' : 'Checking backend…';

  return (
    <header className="sticky top-0 z-30 border-b border-ink-700/80 bg-ink-950/90 backdrop-blur">
      <nav className="mx-auto flex max-w-7xl items-center justify-between gap-4 px-4 py-3 sm:px-6" aria-label="Primary">
        <NavLink to="/" className="flex items-center gap-2 font-mono text-sm font-bold tracking-wide text-slate-100">
          <span className="flex h-6 w-6 items-center justify-center rounded bg-adaptive/20 text-adaptive">◧</span>
          AdaptiveOS
        </NavLink>
        <ul className="flex flex-wrap items-center gap-1 text-sm">
          {LINKS.map((l) => (
            <li key={l.to}>
              <NavLink
                to={l.to}
                end={l.end}
                className={({ isActive }) =>
                  `rounded-md px-3 py-1.5 transition-colors ${isActive ? 'bg-ink-800 text-slate-100' : 'text-slate-400 hover:bg-ink-800 hover:text-slate-100'}`
                }
              >
                {l.label}
              </NavLink>
            </li>
          ))}
        </ul>
        <span className="hidden items-center gap-2 font-mono text-xs text-slate-500 sm:flex" role="status">
          <span className={`h-2 w-2 rounded-full ${dot}`} aria-hidden />
          {text}
        </span>
      </nav>
    </header>
  );
}
