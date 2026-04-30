// Dashboard: reusable shell + Overview/Health screen

function DashShell({ children, active = "overview" }) {
  const nav = [
    { id:"overview", icon:ICONS.server, label:"Overview" },
    { id:"models",   icon:ICONS.download, label:"Models" },
    { id:"sessions", icon:ICONS.globe, label:"Sessions" },
    { id:"users",    icon:ICONS.user, label:"Users" },
    { id:"settings", icon:ICONS.cog, label:"Settings" },
  ];
  return (
    <div style={{width:'100%',height:'100%',display:'flex',fontFamily:'var(--font-sans)',background:'var(--bg)',overflow:'hidden'}}>
      {/* Sidebar */}
      <div style={{width:200,height:'100%',background:'var(--bg)',borderRight:'1px solid var(--line)',display:'flex',flexDirection:'column',flexShrink:0}}>
        <div style={{padding:'20px 16px 16px',display:'flex',alignItems:'center',gap:8}}>
          <div style={{width:28,height:28,borderRadius:8,background:'var(--accent)',display:'flex',alignItems:'center',justifyContent:'center'}}>
            <div style={{width:16,height:16,color:'#fff'}}>{ICONS.globe}</div>
          </div>
          <span style={{fontSize:14,fontWeight:700,color:'var(--ink)',letterSpacing:-0.3}}>LP Dashboard</span>
        </div>
        <div style={{flex:1,padding:'4px 8px',display:'flex',flexDirection:'column',gap:1}}>
          {nav.map(n=>(
            <div key={n.id} style={{display:'flex',alignItems:'center',gap:8,padding:'9px 10px',borderRadius:8,background:n.id===active?'var(--accent-2)':'transparent',cursor:'pointer'}}>
              <div style={{width:16,height:16,color:n.id===active?'var(--accent)':'var(--ink-3)'}}>{n.icon}</div>
              <span style={{fontSize:13,fontWeight:n.id===active?600:400,color:n.id===active?'var(--accent-ink)':'var(--ink-2)'}}>{n.label}</span>
            </div>
          ))}
        </div>
        <div style={{padding:'12px 16px',borderTop:'1px solid var(--line)',display:'flex',alignItems:'center',gap:8}}>
          <div style={{width:28,height:28,borderRadius:14,background:'var(--bg-3)',display:'flex',alignItems:'center',justifyContent:'center',fontSize:12,fontWeight:600,color:'var(--ink-2)'}}>SA</div>
          <div>
            <div style={{fontSize:12,fontWeight:500,color:'var(--ink)'}}>Super Admin</div>
            <div style={{fontSize:10,color:'var(--ink-4)'}}>admin@corp.local</div>
          </div>
        </div>
      </div>
      {/* Content */}
      <div style={{flex:1,overflow:'auto',minWidth:0}}>{children}</div>
    </div>
  );
}

function DashCard({ title, value, sub, tone, spark }) {
  return (
    <div style={{background:'#fff',border:'1px solid var(--line)',borderRadius:12,padding:'16px 18px',minWidth:0}}>
      <div style={{fontSize:12,color:'var(--ink-3)',fontWeight:500,marginBottom:6}}>{title}</div>
      <div style={{fontSize:24,fontWeight:700,color:tone==='ok'?'var(--ok)':tone==='warn'?'var(--warn)':tone==='err'?'var(--err)':'var(--ink)',letterSpacing:-0.5,fontVariantNumeric:'tabular-nums'}}>{value}</div>
      {sub && <div style={{fontSize:11,color:'var(--ink-4)',marginTop:4}}>{sub}</div>}
      {spark && <div style={{marginTop:10}}><Sparkline data={spark} width={100} height={28}/></div>}
    </div>
  );
}

function DashOverview() {
  return (
    <DashShell active="overview">
      <div style={{padding:'24px 28px'}}>
        <div style={{fontSize:20,fontWeight:700,color:'var(--ink)',letterSpacing:-0.4,marginBottom:4}}>System Overview</div>
        <div style={{fontSize:13,color:'var(--ink-3)',marginBottom:22}}>All systems · Last updated 9:41 AM</div>

        {/* KPI cards */}
        <div style={{display:'grid',gridTemplateColumns:'repeat(4,1fr)',gap:12,marginBottom:24}}>
          <DashCard title="Active Sessions" value="7" sub="+2 from yesterday" spark={[2,3,4,3,5,6,7]}/>
          <DashCard title="Avg Latency" value="38ms" tone="ok" sub="↓ 12ms vs last hour" spark={[55,48,42,50,44,38,38]}/>
          <DashCard title="Packet Loss" value="1.2%" tone="ok" sub="Within threshold" spark={[2.1,1.8,1.5,1.9,1.4,1.2,1.2]}/>
          <DashCard title="CPU Usage" value="61%" tone="warn" sub="Whisper model active" spark={[40,45,55,60,58,62,61]}/>
        </div>

        {/* Models status */}
        <div style={{background:'#fff',border:'1px solid var(--line)',borderRadius:12,marginBottom:16}}>
          <div style={{padding:'14px 18px',borderBottom:'1px solid var(--line)',fontSize:13,fontWeight:600,color:'var(--ink)'}}>Active Models</div>
          {[
            {name:"whisper-large-v3",role:"ASR",mem:"6.2 GB",tone:"ok",lat:"38ms"},
            {name:"nllb-200-distilled",role:"MT",mem:"2.1 GB",tone:"ok",lat:"21ms"},
            {name:"coqui-tts-zh",role:"TTS",mem:"1.4 GB",tone:"ok",lat:"95ms"},
          ].map(m=>(
            <div key={m.name} style={{display:'flex',alignItems:'center',gap:14,padding:'12px 18px',borderBottom:'1px solid var(--line)'}}>
              <Dot tone={m.tone}/>
              <span style={{fontSize:13,fontFamily:'var(--font-mono)',color:'var(--ink)',flex:1}}>{m.name}</span>
              <Tag tone="neutral">{m.role}</Tag>
              <span style={{fontSize:12,color:'var(--ink-3)',fontVariantNumeric:'tabular-nums',width:52,textAlign:'right'}}>{m.mem}</span>
              <span style={{fontSize:12,color:'var(--ok)',fontVariantNumeric:'tabular-nums',fontFamily:'var(--font-mono)',width:40,textAlign:'right'}}>{m.lat}</span>
            </div>
          ))}
        </div>

        {/* Latency chart placeholder */}
        <div style={{background:'#fff',border:'1px solid var(--line)',borderRadius:12,padding:'16px 18px'}}>
          <div style={{fontSize:13,fontWeight:600,color:'var(--ink)',marginBottom:14}}>Translation Latency — last 30 min</div>
          <BarChart data={[38,42,36,45,40,38,52,44,39,37,41,38,36,40,42,38,35,37,39,38,40,41,38,36,38,39,37,40,38,38]} width={540} height={72}/>
          <div style={{display:'flex',justifyContent:'space-between',marginTop:6,fontSize:10,color:'var(--ink-4)'}}>
            <span>30 min ago</span><span>now</span>
          </div>
        </div>
      </div>
    </DashShell>
  );
}

Object.assign(window, { DashShell, DashCard, DashOverview });
