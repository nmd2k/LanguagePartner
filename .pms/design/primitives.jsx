// Shared atoms used across both Android and Dashboard.

const ICONS = {
  mic: <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round"><rect x="9" y="3" width="6" height="12" rx="3"/><path d="M5 11a7 7 0 0 0 14 0M12 18v3"/></svg>,
  micOff: <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round"><path d="M3 3l18 18M9 9v3a3 3 0 0 0 5.12 2.12M15 12V6a3 3 0 0 0-5.94-.6M5 11a7 7 0 0 0 11.66 5.2M19 11a7 7 0 0 1-.34 2.16M12 18v3"/></svg>,
  swap: <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round"><path d="M7 4l-3 3 3 3M4 7h16M17 14l3 3-3 3M20 17H4"/></svg>,
  speaker: <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round"><path d="M11 5L6 9H3v6h3l5 4V5z"/><path d="M16 9a4 4 0 0 1 0 6"/></svg>,
  check: <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M4 12l5 5L20 6"/></svg>,
  chev: <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round"><path d="M9 6l6 6-6 6"/></svg>,
  back: <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round"><path d="M15 6l-6 6 6 6"/></svg>,
  more: <svg viewBox="0 0 24 24" fill="currentColor"><circle cx="5" cy="12" r="1.6"/><circle cx="12" cy="12" r="1.6"/><circle cx="19" cy="12" r="1.6"/></svg>,
  search: <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round"><circle cx="11" cy="11" r="6"/><path d="M20 20l-4.5-4.5"/></svg>,
  plus: <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round"><path d="M12 5v14M5 12h14"/></svg>,
  cog: <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 1 1-4 0v-.09a1.65 1.65 0 0 0-1-1.51 1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 1 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 1 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 1 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>,
  bug: <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"><rect x="7" y="8" width="10" height="12" rx="5"/><path d="M9 5l1.5 2M15 5l-1.5 2M3 13h4M17 13h4M3 9l3 2M21 9l-3 2M3 17l3-1M21 17l-3-1"/></svg>,
  globe: <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5"><circle cx="12" cy="12" r="9"/><path d="M3 12h18M12 3a14 14 0 0 1 0 18M12 3a14 14 0 0 0 0 18"/></svg>,
  user: <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"><circle cx="12" cy="8" r="4"/><path d="M4 21c0-4 4-7 8-7s8 3 8 7"/></svg>,
  server: <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5"><rect x="3" y="4" width="18" height="7" rx="1.5"/><rect x="3" y="13" width="18" height="7" rx="1.5"/><circle cx="7" cy="7.5" r=".8" fill="currentColor"/><circle cx="7" cy="16.5" r=".8" fill="currentColor"/></svg>,
  power: <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round"><path d="M12 4v8M6.3 7.7a8 8 0 1 0 11.4 0"/></svg>,
  copy: <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5"><rect x="8" y="8" width="12" height="12" rx="2"/><path d="M16 8V5a1 1 0 0 0-1-1H5a1 1 0 0 0-1 1v10a1 1 0 0 0 1 1h3"/></svg>,
  download: <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round"><path d="M12 4v12M7 11l5 5 5-5M5 20h14"/></svg>,
  eye: <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5"><path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z"/><circle cx="12" cy="12" r="3"/></svg>,
  device: <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5"><rect x="6" y="2" width="12" height="20" rx="2"/><path d="M11 19h2"/></svg>,
};

// Status dot
function Dot({ tone = "ok" }) {
  const map = { ok: "var(--ok)", warn: "var(--warn)", err: "var(--err)", off: "var(--ink-4)" };
  return <span style={{ display: 'inline-block', width: 8, height: 8, borderRadius: 4, background: map[tone], marginRight: 8, verticalAlign: 'middle' }} />;
}

// Tag pill
function Tag({ children, tone, mono }) {
  const tones = {
    ok: { bg: "var(--ok-bg)", c: "var(--ok)" },
    warn: { bg: "var(--warn-bg)", c: "var(--warn)" },
    err: { bg: "var(--err-bg)", c: "var(--err)" },
    accent: { bg: "var(--accent-2)", c: "var(--accent-ink)" },
    neutral: { bg: "var(--bg-2)", c: "var(--ink-2)" },
  };
  const t = tones[tone] || tones.neutral;
  return <span style={{ background: t.bg, color: t.c, padding: '3px 8px', borderRadius: 4, fontSize: 11, fontWeight: 500, letterSpacing: 0.2, fontFamily: mono ? 'var(--font-mono)' : 'inherit', whiteSpace: 'nowrap' }}>{children}</span>;
}

// Speaker chip
function SpeakerChip({ id = "A", name, color = "var(--spk-a)", small }) {
  const sz = small ? 22 : 28;
  return (
    <div style={{ display: 'inline-flex', alignItems: 'center', gap: 8 }}>
      <div style={{ width: sz, height: sz, borderRadius: '50%', background: color, color: '#fff', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: small ? 11 : 13, fontWeight: 600 }}>{id}</div>
      {name && <span style={{ fontSize: small ? 12 : 13, color: 'var(--ink-2)' }}>{name}</span>}
    </div>
  );
}

// Tiny waveform (pure CSS bars)
function Waveform({ active = true, bars = 18, color = "currentColor", height = 28 }) {
  const heights = [0.4, 0.7, 0.9, 0.5, 0.3, 0.8, 1.0, 0.6, 0.4, 0.7, 0.5, 0.8, 0.9, 0.4, 0.6, 0.5, 0.3, 0.7];
  return (
    <div style={{ display: 'inline-flex', alignItems: 'center', gap: 2, height }}>
      {Array.from({ length: bars }).map((_, i) => (
        <div key={i} style={{
          width: 2, borderRadius: 1, background: color,
          height: `${(heights[i % heights.length]) * 100}%`,
          opacity: active ? 1 : 0.3,
          transition: 'height .2s',
        }} />
      ))}
    </div>
  );
}

// Sparkline
function Sparkline({ data, color = "var(--accent)", width = 120, height = 32, fill = true }) {
  const max = Math.max(...data), min = Math.min(...data);
  const range = Math.max(1, max - min);
  const pts = data.map((v, i) => [(i / (data.length - 1)) * width, height - ((v - min) / range) * (height - 4) - 2]);
  const d = pts.map((p, i) => (i === 0 ? `M${p[0]},${p[1]}` : `L${p[0]},${p[1]}`)).join(' ');
  const area = `${d} L${width},${height} L0,${height} Z`;
  return (
    <svg width={width} height={height} style={{ display: 'block' }}>
      {fill && <path d={area} fill={color} opacity="0.12" />}
      <path d={d} fill="none" stroke={color} strokeWidth="1.5" strokeLinejoin="round" strokeLinecap="round" />
    </svg>
  );
}

// Bar chart (vertical)
function BarChart({ data, color = "var(--accent)", width = 240, height = 60 }) {
  const max = Math.max(...data);
  const bw = (width - (data.length - 1) * 2) / data.length;
  return (
    <svg width={width} height={height}>
      {data.map((v, i) => (
        <rect key={i} x={i * (bw + 2)} y={height - (v / max) * height} width={bw} height={(v / max) * height} fill={color} opacity={0.85} rx={1} />
      ))}
    </svg>
  );
}

// Voice print pattern (decorative)
function VoicePrint({ color = "var(--accent)", width = 120, height = 36, seed = 0 }) {
  const N = 40;
  const heights = Array.from({ length: N }).map((_, i) => {
    const x = i + seed;
    return 0.2 + Math.abs(Math.sin(x * 0.6) * 0.5 + Math.cos(x * 1.3) * 0.3 + Math.sin(x * 2.1) * 0.2);
  });
  return (
    <svg width={width} height={height}>
      {heights.map((h, i) => {
        const bw = (width - N) / N;
        const bh = Math.min(h * height, height);
        return <rect key={i} x={i * (bw + 1)} y={(height - bh) / 2} width={bw} height={bh} fill={color} rx={0.5} />;
      })}
    </svg>
  );
}

Object.assign(window, { ICONS, Dot, Tag, SpeakerChip, Waveform, Sparkline, BarChart, VoicePrint });
