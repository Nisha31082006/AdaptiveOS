import { CartesianGrid, Line, LineChart, ReferenceLine, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import type { ParametersResponse, PolicyTransition, Snapshot } from '../types/simulation';

interface Props {
  snapshots: Snapshot[];
  parameters: ParametersResponse;
  transitions: PolicyTransition[];
  currentTime?: number;
}

/** EWMA interactivity score over time (one point per tick, from the engine) with LOW/HIGH thresholds. */
export default function ScoreChart({ snapshots, parameters, transitions, currentTime }: Props) {
  const data = snapshots.filter((s) => s.interactivityScore !== null).map((s) => ({ time: s.time, score: Number((s.interactivityScore as number).toFixed(1)) }));
  const last = data.length ? data[data.length - 1].time : 0;
  return (
    <div>
      <h3 className="panel-title mb-2">Interactivity score (EWMA)</h3>
      <div className="h-56" role="img" aria-label="Line chart of the interactivity score over time with low and high thresholds">
        <ResponsiveContainer width="100%" height="100%">
          <LineChart data={data} margin={{ top: 8, right: 16, bottom: 4, left: 0 }}>
            <CartesianGrid stroke="#1c2632" />
            <XAxis dataKey="time" type="number" domain={[0, last]} tick={{ fill: '#94a3b8', fontSize: 11 }} stroke="#2a3745" />
            <YAxis domain={[0, parameters.maxScore]} ticks={[0, parameters.lowThreshold, 512, parameters.highThreshold, parameters.maxScore]} tick={{ fill: '#94a3b8', fontSize: 11 }} stroke="#2a3745" width={44} />
            <Tooltip contentStyle={{ background: '#080b10', border: '1px solid #2a3745', fontSize: 12 }} formatter={(v: number) => [v, 'score']} labelFormatter={(l) => `t=${l}`} />
            <ReferenceLine y={parameters.highThreshold} stroke="#f59e0b" strokeDasharray="5 4" label={{ value: `HIGH ${parameters.highThreshold}`, fill: '#f59e0b', fontSize: 10, position: 'insideTopRight' }} />
            <ReferenceLine y={parameters.lowThreshold} stroke="#38bdf8" strokeDasharray="5 4" label={{ value: `LOW ${parameters.lowThreshold}`, fill: '#38bdf8', fontSize: 10, position: 'insideBottomRight' }} />
            {transitions.map((t, i) => (
              <ReferenceLine key={i} x={t.time} stroke="#34d399" strokeDasharray="3 3" label={{ value: `${t.fromPolicy}→${t.toPolicy}`, fill: '#34d399', fontSize: 10, position: 'top' }} />
            ))}
            {currentTime !== undefined && <ReferenceLine x={currentTime} stroke="#f8fafc" strokeOpacity={0.7} />}
            <Line type="stepAfter" dataKey="score" stroke="#34d399" strokeWidth={2} dot={false} isAnimationActive={false} />
          </LineChart>
        </ResponsiveContainer>
      </div>
      <p className="kbd-hint mt-1">Score below LOW: RR→SRTF is considered. Above HIGH: SRTF→RR is considered. Between them nothing changes (hysteresis).</p>
    </div>
  );
}
