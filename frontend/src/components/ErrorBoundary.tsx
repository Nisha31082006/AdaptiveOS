import { Component, type ErrorInfo, type ReactNode } from 'react';

interface Props { children: ReactNode }
interface State { error: Error | null }

/** Catches rendering crashes so one bad view doesn't blank the whole app. Never shows a raw stack trace. */
export default class ErrorBoundary extends Component<Props, State> {
  state: State = { error: null };

  static getDerivedStateFromError(error: Error): State {
    return { error };
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('AdaptiveOS UI crashed:', error, info.componentStack);
  }

  render() {
    if (this.state.error) {
      return (
        <div className="mx-auto max-w-lg px-4 py-20 text-center">
          <h1 className="text-xl font-semibold text-slate-100">Something went wrong displaying this page</h1>
          <p className="mt-2 text-sm text-slate-400">Try reloading. If the problem continues, use the thumbs-down / feedback option to report it.</p>
          <button type="button" className="btn btn-primary mt-4" onClick={() => this.setState({ error: null })}>
            Try again
          </button>
        </div>
      );
    }
    return this.props.children;
  }
}
