import { useWorkspace } from '../state/WorkspaceContext';
import { API_BASE_URL } from '../services/api';

/** Full-width banner shown wherever the backend cannot be reached. */
export default function StatusBanner() {
  const { backend } = useWorkspace();
  if (backend !== 'offline') return null;
  return (
    <div role="alert" className="border-b border-red-500/40 bg-red-950/60 px-4 py-2 text-center text-sm text-red-200">
      Cannot reach the AdaptiveOS backend at <span className="font-mono">{API_BASE_URL}</span>. Start the Spring Boot server (see the README) and reload this page.
    </div>
  );
}
