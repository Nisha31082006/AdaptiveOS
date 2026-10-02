import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import EventLog from '../EventLog';
import type { EngineEvent } from '../../types/simulation';

const events: EngineEvent[] = [
  { time: 4, type: 'QUANTUM_EXPIRED', processId: 'P1', policy: 'RR', message: 'P1 quantum expired', adaptive: false },
  { time: 7, type: 'IO_STARTED', processId: 'P2', policy: 'RR', message: 'P2 enters IO for 4 ticks', adaptive: false },
  { time: 15, type: 'POLICY_SWITCH', processId: null, policy: 'SRTF', message: 'Adaptive controller: RR -> SRTF', adaptive: true },
];

describe('EventLog', () => {
  it('renders every event by default', () => {
    render(<EventLog events={events} />);
    expect(screen.getByText(/3 of 3/)).toBeInTheDocument();
  });

  it('filters by process', async () => {
    render(<EventLog events={events} />);
    await userEvent.selectOptions(screen.getByLabelText('Process'), 'P1');
    expect(screen.getByText(/1 of 3/)).toBeInTheDocument();
  });

  it('filters to adaptive-only events', async () => {
    render(<EventLog events={events} />);
    await userEvent.click(screen.getByLabelText(/adaptive events only/i));
    expect(screen.getByText(/1 of 3/)).toBeInTheDocument();
  });

  it('shows an empty state when filters exclude everything', async () => {
    render(<EventLog events={events} />);
    await userEvent.selectOptions(screen.getByLabelText('Process'), 'P1');
    await userEvent.click(screen.getByLabelText(/adaptive events only/i));
    expect(screen.getByText(/no events match/i)).toBeInTheDocument();
  });
});
