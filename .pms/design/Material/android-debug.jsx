// Android: Debug tab + Settings + Onboarding

function ScreenDebug() {
  const logs = [
    { t:"09:41:02.312", lvl:"INFO",  msg:"Session started · id=7f3a" },
    { t:"09:41:02.488", lvl:"INFO",  msg:"WebSocket connected to partner.corp.local" },
    { t:"09:41:03.201", lvl:"DEBUG", msg:"Model whisper-large-v3 loaded (1.2 s)" },
    { t:"09:41:08.003", lvl:"INFO",  msg:"Utterance detected · speaker=A · 2.1 s" },
    { t:"09:41:08.541", lvl:"DEBUG", msg:"Translation latency: 312 ms" },
    { t:"09:41:09.220", lvl:"WARN",  msg:"Audio dropout 40 ms — packet loss 2%" },
    { t:"09:41:12.100", lvl:"INFO",  msg:"Utterance detected · speaker=B · 1.8 s" },
    { t:"09:41:12.540", lvl:"ERROR", msg:"Speaker diarisation timeout — fallback used" },
  ];
  const lvlColor = { INFO:"var(--ok)", DEBUG:"var(--ink-3)", WARN:"var(--warn)", ERROR:"var(--err)" };

  return (
    <Phone title="Debug" back bg="#0f0f0e">
      {/* Stat row */}
      <div style={{display:'flex',gap:8,padding:'10px 12px',borderBottom:'1px solid rgba(255,255,255,0.06)'}}>
        {[["RTT","42ms","ok"],["Drop","2%","warn"],["Session","7f3a","neutral"],["Model","wlv3","neutral"]].map(([k,v,tone])=>(
          <div key={k} style={{flex:1,background:'rgba(255,255,255,0.05)',borderRadius:8,padding:'7px 6px',textAlign:'center'}}>
            <div style={{fontSize:10,color:'rgba(255,255,255,0.35)',marginBottom:3,fontFamily:'var(--font-mono)'}}>{k}</div>
            <div style={{fontSize:13,fontWeight:600,color:tone==='ok'?'var(--ok)':tone==='warn'?'var(--warn)':'rgba(255,255,255,0.75)',fontFamily:'var(--font-mono)'}}>{v}</div>
          </div>
        ))}
      </div>

      {/* Log list */}
      <div style={{padding:'8px 10px',display:'flex',flexDirection:'column',gap:2,overflowY:'auto',height:480}}>
        {logs.map((l,i)=>(
          <div key={i} style={{display:'flex',gap:8,padding:'5px 6px',borderRadius:6,background:l.lvl==='ERROR'?'rgba(255,80,60,0.07)':l.lvl==='WARN'?'rgba(255,180,60,0.05)':'transparent'}}>
            <span style={{fontSize:9,color:'rgba(255,255,255,0.25)',fontFamily:'var(--font-mono)',whiteSpace:'nowrap',paddingTop:1}}>{l.t}</span>
            <span style={{fontSize:10,fontWeight:700,color:lvlColor[l.lvl],fontFamily:'var(--font-mono)',whiteSpace:'nowrap',width:38}}>{l.lvl}</span>
            <span style={{fontSize:11,color:'rgba(255,255,255,0.65)',fontFamily:'var(--font-mono)',lineHeight:1.4}}>{l.msg}</span>
          </div>
        ))}
      </div>
    </Phone>
  );
}

function ScreenSettings() {
  const rows = [
    { section:"Account" },
    { icon:ICONS.user, label:"Profile", sub:"you@example.com" },
    { icon:ICONS.server, label:"Server", sub:"partner.corp.local" },
    { section:"Translation" },
    { icon:ICONS.globe, label:"Source language", sub:"English" },
    { icon:ICONS.globe, label:"Target language", sub:"普通话" },
    { icon:ICONS.speaker, label:"TTS voice", sub:"Female · Alloy" },
    { section:"Developer" },
    { icon:ICONS.bug, label:"Debug console", sub:"Log level: INFO" },
    { icon:ICONS.copy, label:"Export session log", sub:"" },
  ];
  return (
    <Phone title="Settings" bg="var(--bg)">
      <div style={{overflowY:'auto',height:'100%'}}>
        {rows.map((r,i) => r.section ? (
          <div key={i} style={{padding:'18px 16px 6px',fontSize:11,fontWeight:700,letterSpacing:0.6,color:'var(--ink-4)',textTransform:'uppercase'}}>{r.section}</div>
        ) : (
          <div key={i} style={{display:'flex',alignItems:'center',gap:12,padding:'13px 16px',borderBottom:'1px solid var(--line)'}}>
            <div style={{width:18,height:18,color:'var(--ink-3)',flexShrink:0}}>{r.icon}</div>
            <div style={{flex:1}}>
              <div style={{fontSize:14,color:'var(--ink)'}}>{r.label}</div>
              {r.sub && <div style={{fontSize:12,color:'var(--ink-4)'}}>{r.sub}</div>}
            </div>
            <div style={{width:16,height:16,color:'var(--ink-4)'}}>{ICONS.chev}</div>
          </div>
        ))}
      </div>
    </Phone>
  );
}

function ScreenOnboarding() {
  return (
    <Phone noChrome bg="var(--bg)">
      <div style={{height:'100%',display:'flex',flexDirection:'column',alignItems:'center',padding:'48px 28px 24px',textAlign:'center'}}>
        <div style={{width:80,height:80,borderRadius:24,background:'var(--accent)',display:'flex',alignItems:'center',justifyContent:'center',marginBottom:24}}>
          <div style={{width:44,height:44,color:'#fff'}}>{ICONS.globe}</div>
        </div>
        <div style={{fontSize:24,fontWeight:700,letterSpacing:-0.6,color:'var(--ink)',fontFamily:'var(--font-display)',marginBottom:10}}>Real-time translation, together</div>
        <div style={{fontSize:14,color:'var(--ink-3)',lineHeight:1.6,marginBottom:36}}>Hear and speak across languages instantly. Multiple speakers, one conversation.</div>

        {/* Steps */}
        {[
          [ICONS.server,"Connect your server","Point the app to your self-hosted backend"],
          [ICONS.mic,"Start speaking","The app detects speakers automatically"],
          [ICONS.globe,"Read translations","Each speaker's words appear in your language"],
        ].map(([icon,title,sub],i)=>(
          <div key={i} style={{display:'flex',gap:12,alignItems:'flex-start',textAlign:'left',marginBottom:20,width:'100%'}}>
            <div style={{width:36,height:36,borderRadius:10,background:'var(--accent-2)',display:'flex',alignItems:'center',justifyContent:'center',flexShrink:0}}>
              <div style={{width:18,height:18,color:'var(--accent)'}}>{icon}</div>
            </div>
            <div>
              <div style={{fontSize:13,fontWeight:600,color:'var(--ink)'}}>{title}</div>
              <div style={{fontSize:12,color:'var(--ink-3)'}}>{sub}</div>
            </div>
          </div>
        ))}

        <div style={{marginTop:'auto',display:'flex',flexDirection:'column',gap:10,width:'100%'}}>
          <button style={{padding:'14px',background:'var(--accent)',border:'none',borderRadius:12,color:'#fff',fontSize:15,fontWeight:600,cursor:'pointer'}}>Get Started</button>
          <button style={{padding:'13px',background:'none',border:'none',color:'var(--ink-3)',fontSize:14,cursor:'pointer'}}>I have an account</button>
        </div>
      </div>
    </Phone>
  );
}

Object.assign(window, { ScreenDebug, ScreenSettings, ScreenOnboarding });
