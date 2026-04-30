// Custom Android phone shell — matches our neutral design system rather than M3 teal.
// Status bar + optional app bar + content + nav pill. No keyboard.

function Phone({ children, title, back, action, bg = "var(--bg)", noChrome = false, height = 780 }) {
  return (
    <div style={{
      width: 320, height, borderRadius: 36, padding: 6,
      background: '#1a1a18',
      boxShadow: '0 30px 60px -20px rgba(0,0,0,0.25), 0 0 0 1px rgba(0,0,0,0.06)',
      fontFamily: 'var(--font-sans)',
      flexShrink: 0,
    }}>
      <div style={{
        width: '100%', height: '100%', borderRadius: 30, overflow: 'hidden',
        background: bg, display: 'flex', flexDirection: 'column', position: 'relative',
      }}>
        {/* Status bar */}
        <div style={{ height: 32, padding: '0 18px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', fontSize: 12, color: 'var(--ink)', fontWeight: 500, position: 'relative', zIndex: 2 }}>
          <span className="tnum">9:41</span>
          <div style={{ position: 'absolute', left: '50%', top: 8, transform: 'translateX(-50%)', width: 18, height: 18, borderRadius: 9, background: '#1a1a18' }} />
          <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
            <svg width="14" height="10" viewBox="0 0 14 10" fill="currentColor"><path d="M7 0a8 8 0 0 0-7 4l1 1a6.5 6.5 0 0 1 12 0l1-1a8 8 0 0 0-7-4zm0 3a5 5 0 0 0-4 2l1 1a3.5 3.5 0 0 1 6 0l1-1a5 5 0 0 0-4-2zm0 3a2 2 0 0 0-1.5.7L7 8l1.5-1.3A2 2 0 0 0 7 6z"/></svg>
            <svg width="14" height="10" viewBox="0 0 14 10" fill="currentColor"><path d="M0 9h2V6H0v3zm4 0h2V4H4v5zm4 0h2V2H8v7zm4 0h2V0h-2v9z"/></svg>
            <svg width="22" height="10" viewBox="0 0 22 10" fill="none"><rect x="0.5" y="0.5" width="18" height="9" rx="2" stroke="currentColor"/><rect x="2" y="2" width="13" height="6" rx="1" fill="currentColor"/><rect x="19.5" y="3.5" width="2" height="3" rx="0.5" fill="currentColor"/></svg>
          </div>
        </div>

        {/* App bar */}
        {!noChrome && (
          <div style={{ display: 'flex', alignItems: 'center', padding: '12px 16px 8px', gap: 12 }}>
            {back && <div style={{ width: 28, height: 28, display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--ink)' }}>{ICONS.back}</div>}
            <div style={{ flex: 1, fontSize: 17, fontWeight: 600, letterSpacing: -0.2, color: 'var(--ink)', fontFamily: 'var(--font-display)' }}>{title}</div>
            {action && <div style={{ color: 'var(--ink-3)' }}>{action}</div>}
          </div>
        )}

        {/* Content */}
        <div style={{ flex: 1, overflow: 'hidden', position: 'relative' }}>{children}</div>

        {/* Nav pill */}
        <div style={{ height: 22, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <div style={{ width: 96, height: 4, borderRadius: 2, background: 'var(--ink-3)', opacity: 0.55 }} />
        </div>
      </div>
    </div>
  );
}

Object.assign(window, { Phone });
