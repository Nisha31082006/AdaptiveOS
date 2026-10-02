import { Route, Routes } from 'react-router-dom';
import Navbar from './components/Navbar';
import StatusBanner from './components/StatusBanner';
import ErrorBoundary from './components/ErrorBoundary';
import Home from './pages/Home';
import Simulator from './pages/Simulator';
import Comparison from './pages/Comparison';
import HowItWorks from './pages/HowItWorks';
import About from './pages/About';
import { WorkspaceProvider } from './state/WorkspaceContext';

export default function App() {
  return (
    <WorkspaceProvider>
      <div className="min-h-screen">
        <a href="#main" className="sr-only focus:not-sr-only focus:absolute focus:z-50 focus:m-2 focus:rounded focus:bg-ink-800 focus:px-3 focus:py-2">
          Skip to main content
        </a>
        <Navbar />
        <StatusBanner />
        <main id="main">
          <ErrorBoundary>
            <Routes>
              <Route path="/" element={<Home />} />
              <Route path="/simulator" element={<Simulator />} />
              <Route path="/comparison" element={<Comparison />} />
              <Route path="/how-it-works" element={<HowItWorks />} />
              <Route path="/about" element={<About />} />
              <Route path="*" element={<NotFound />} />
            </Routes>
          </ErrorBoundary>
        </main>
        <footer className="border-t border-ink-700 py-6 text-center text-xs text-slate-500">
          AdaptiveOS — an experimental, rule-based scheduling simulator. Not a production OS scheduler.
        </footer>
      </div>
    </WorkspaceProvider>
  );
}

function NotFound() {
  return (
    <div className="mx-auto max-w-md px-4 py-24 text-center">
      <p className="font-mono text-5xl text-adaptive">404</p>
      <p className="mt-2 text-slate-400">This page does not exist.</p>
    </div>
  );
}
