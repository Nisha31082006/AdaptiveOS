import { useCallback, useEffect, useRef, useState } from 'react';

/**
 * Replays an already-computed timeline. The first version does NOT stream live from the engine:
 * the backend returns the complete simulation and this hook only moves a time cursor over it.
 */
export function usePlayback(totalTime: number) {
  const [time, setTimeState] = useState(0);
  const [playing, setPlaying] = useState(false);
  const [speed, setSpeed] = useState(1);
  const timeRef = useRef(0);

  const setTime = useCallback(
    (t: number) => {
      const clamped = Math.max(0, Math.min(totalTime, Math.round(t)));
      timeRef.current = clamped;
      setTimeState(clamped);
    },
    [totalTime],
  );

  // A new simulation resets the cursor.
  useEffect(() => {
    setTime(0);
    setPlaying(false);
  }, [totalTime, setTime]);

  useEffect(() => {
    if (!playing) return;
    const id = window.setInterval(() => {
      if (timeRef.current >= totalTime) {
        setPlaying(false);
        return;
      }
      setTime(timeRef.current + 1);
    }, 500 / speed);
    return () => window.clearInterval(id);
  }, [playing, speed, totalTime, setTime]);

  const toggle = useCallback(() => {
    if (!playing && timeRef.current >= totalTime) setTime(0);
    setPlaying((p) => !p);
  }, [playing, totalTime, setTime]);

  return { time, setTime, playing, toggle, speed, setSpeed, stepBy: (d: number) => setTime(timeRef.current + d) };
}
