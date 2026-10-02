import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import BurstEditor from '../BurstEditor';
import type { Burst } from '../../types/simulation';

describe('BurstEditor', () => {
  it('adds a CPU burst', async () => {
    const onChange = vi.fn();
    render(<BurstEditor bursts={[]} onChange={onChange} />);
    await userEvent.click(screen.getByRole('button', { name: '+ CPU burst' }));
    expect(onChange).toHaveBeenCalledWith([{ type: 'CPU', duration: 4 }]);
  });

  it('deletes a burst', async () => {
    const onChange = vi.fn();
    const bursts: Burst[] = [{ type: 'CPU', duration: 3 }, { type: 'IO', duration: 2 }];
    render(<BurstEditor bursts={bursts} onChange={onChange} />);
    await userEvent.click(screen.getByRole('button', { name: 'Delete burst 1' }));
    expect(onChange).toHaveBeenCalledWith([{ type: 'IO', duration: 2 }]);
  });

  it('reorders bursts with the up/down buttons', async () => {
    const onChange = vi.fn();
    const bursts: Burst[] = [{ type: 'CPU', duration: 3 }, { type: 'IO', duration: 2 }];
    render(<BurstEditor bursts={bursts} onChange={onChange} />);
    await userEvent.click(screen.getByRole('button', { name: 'Move burst 2 up' }));
    expect(onChange).toHaveBeenCalledWith([{ type: 'IO', duration: 2 }, { type: 'CPU', duration: 3 }]);
  });

  it('disables moving the first burst up and the last burst down', () => {
    const bursts: Burst[] = [{ type: 'CPU', duration: 3 }, { type: 'IO', duration: 2 }];
    render(<BurstEditor bursts={bursts} onChange={vi.fn()} />);
    expect(screen.getByRole('button', { name: 'Move burst 1 up' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Move burst 2 down' })).toBeDisabled();
  });
});
