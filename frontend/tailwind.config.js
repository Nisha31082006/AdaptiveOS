/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        ink: { 950: '#080b10', 900: '#0b0f14', 850: '#0f151c', 800: '#141b24', 700: '#1c2632', 600: '#2a3745' },
        rr: '#38bdf8',       // Round Robin
        srtf: '#f59e0b',     // SRTF
        adaptive: '#34d399', // AdaptiveOS
      },
      fontFamily: {
        mono: ['ui-monospace', 'SFMono-Regular', 'Menlo', 'Consolas', 'Liberation Mono', 'monospace'],
        sans: ['Inter', 'ui-sans-serif', 'system-ui', '-apple-system', 'Segoe UI', 'Roboto', 'sans-serif'],
      },
    },
  },
  plugins: [],
};
