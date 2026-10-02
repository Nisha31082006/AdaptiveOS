import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import ProcessTable from '../ProcessTable';
import type { ProcessDef } from '../../types/simulation';

const processes: ProcessDef[] = [
  { pid: 'P1', arrivalTime: 0, bursts: [{ type: 'CPU', duration: 10 }] },
  { pid: 'P2', arrivalTime: 1, bursts: [{ type: 'CPU', duration: 3 }, { type: 'IO', duration: 4 }, { type: 'CPU', duration: 2 }] },
];

describe('ProcessTable', () => {
  it('shows an empty state when there are no processes', () => {
    render(<ProcessTable processes={[]} onAdd={vi.fn()} onEdit={vi.fn()} onDelete={vi.fn()} />);
    expect(screen.getByText(/workload is empty/i)).toBeInTheDocument();
  });

  it('lists every process with its burst chips', () => {
    render(<ProcessTable processes={processes} onAdd={vi.fn()} onEdit={vi.fn()} onDelete={vi.fn()} />);
    expect(screen.getByText('P1')).toBeInTheDocument();
    expect(screen.getByText('P2')).toBeInTheDocument();
    expect(screen.getByText('CPU 10')).toBeInTheDocument();
    expect(screen.getByText('IO 4')).toBeInTheDocument();
  });

  it('calls onDelete with the right index', async () => {
    const onDelete = vi.fn();
    render(<ProcessTable processes={processes} onAdd={vi.fn()} onEdit={vi.fn()} onDelete={onDelete} />);
    await userEvent.click(screen.getByRole('button', { name: 'Delete P2' }));
    expect(onDelete).toHaveBeenCalledWith(1);
  });

  it('calls onAdd when the add button is clicked', async () => {
    const onAdd = vi.fn();
    render(<ProcessTable processes={[]} onAdd={onAdd} onEdit={vi.fn()} onDelete={vi.fn()} />);
    await userEvent.click(screen.getByRole('button', { name: '+ Add process' }));
    expect(onAdd).toHaveBeenCalled();
  });
});
