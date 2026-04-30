// Dashboard: Sessions + Performance + Users + Devices

function DashSessions() {
  const sessions = [
    { id:"7f3a", users:2, langs:"EN↔ZH", model:"whisper-large-v3", dur:"14m", status:"ok",  lat:"38ms" },
    { id:"2c8b", users:3, langs:"EN↔JA", model:"whisper-large-v3", dur:"3m",  status:"ok",  lat:"42ms" },
    { id:"9d1e", users:2, langs:"ES↔EN", model:"whisper-medium",    dur:"28m", status:"warn",lat:"88ms" },
    { id:"4a0f", users:2, langs:"EN↔ZH", model:"whisper-large-v3", dur:"1m",  status:"ok",  lat:"36ms" },
  ];
  const perfData = [38,42,36,45,40,38,52,44,39,37,41,38,36,40,42,38,35,37,39,38];

  return (
    <DashShell active="sessions">
      <div style={{padding:'24px 28px'}}>
        <div style={{fontSize:20,fontWeight:700,color:'var(--ink)',letterSpacing:-0.4,marginBottom:20}}>Sessions & Performance</div>

        {/* Summary */}
        <div style={{display:'grid',gridTemplateColumns:'repeat(3,1fr)',gap:12,marginBottom:24}}>
          <DashCard title="Active Sessions" value="7" spark={[3,4,5,4,6,7,7]}/>
          <DashCard title="Avg Latency" value="38ms" tone="ok" spark={[55,48,42,50,44,38,38]}/>
          <DashCard title="Total Today" value="43" sub="Sessions completed" spark={[5,8,6,9,7,4,4]}/>
        </div>

        {/* Session list */}
        <div style={{background:'#fff',border:'1px solid var(--line)',borderRadius:12,marginBottom:24}}>
          <div style={{padding:'14px 18px',borderBottom:'1px solid var(--line)',fontSize:13,fontWeight:600,color:'var(--ink)'}}>Live Sessions</div>
          <div style={{display:'grid',gridTemplateColumns:'80px 48px 96px 1fr 48px 56px 64px',padding:'8px 18px',fontSize:11,color:'var(--ink-4)',fontWeight:600,letterSpacing:0.3,textTransform:'uppercase',gap:8}}>
            <span>ID</span><span>Users</span><span>Langs</span><span>Model</span><span>Lat</span><span>Time</span><span>Status</span>
          </div>
          {sessions.map(s=>(
            <div key={s.id} style={{display:'grid',gridTemplateColumns:'80px 48px 96px 1fr 48px 56px 64px',padding:'11px 18px',borderTop:'1px solid var(--line)',alignItems:'center',gap:8}}>
              <span style={{fontSize:12,fontFamily:'var(--font-mono)',color:'var(--accent-ink)'}}>{s.id}</span>
              <span style={{fontSize:12,color:'var(--ink-3)',textAlign:'center'}}>{s.users}</span>
              <span style={{fontSize:12,fontFamily:'var(--font-mono)',color:'var(--ink)'}}>{s.langs}</span>
              <span style={{fontSize:11,color:'var(--ink-3)',overflow:'hidden',textOverflow:'ellipsis',whiteSpace:'nowrap',fontFamily:'var(--font-mono)'}}>{s.model}</span>
              <span style={{fontSize:12,color:s.status==='ok'?'var(--ok)':'var(--warn)',fontFamily:'var(--font-mono)'}}>{s.lat}</span>
              <span style={{fontSize:12,color:'var(--ink-3)',fontVariantNumeric:'tabular-nums'}}>{s.dur}</span>
              <Tag tone={s.status}>{s.status==='ok'?'Active':'Degraded'}</Tag>
            </div>
          ))}
        </div>

        {/* Perf chart */}
        <div style={{background:'#fff',border:'1px solid var(--line)',borderRadius:12,padding:'16px 18px'}}>
          <div style={{fontSize:13,fontWeight:600,color:'var(--ink)',marginBottom:4}}>Latency trend — last 20 min</div>
          <div style={{fontSize:11,color:'var(--ink-4)',marginBottom:12}}>p50 translation latency in ms</div>
          <Sparkline data={perfData} width={500} height={56} fill={true}/>
          <div style={{display:'flex',justifyContent:'space-between',marginTop:6,fontSize:10,color:'var(--ink-4)'}}>
            <span>20 min ago</span><span>now</span>
          </div>
        </div>
      </div>
    </DashShell>
  );
}

function DashUsers() {
  const users = [
    { name:"Alice Chen",    email:"alice@corp.local",  role:"User",  device:"Pixel 8",    last:"2 min ago",  status:"ok" },
    { name:"Bob Liu",       email:"bob@corp.local",    role:"User",  device:"Galaxy S24",  last:"5 min ago",  status:"ok" },
    { name:"Carlos García", email:"carlos@corp.local", role:"Admin", device:"Pixel 7a",   last:"1 hr ago",   status:"off" },
    { name:"Diana Wu",      email:"diana@corp.local",  role:"User",  device:"Pixel 8 Pro",last:"12 min ago", status:"ok" },
  ];

  return (
    <DashShell active="users">
      <div style={{padding:'24px 28px'}}>
        <div style={{display:'flex',alignItems:'center',marginBottom:20,gap:12}}>
          <div style={{flex:1}}>
            <div style={{fontSize:20,fontWeight:700,color:'var(--ink)',letterSpacing:-0.4}}>Users & Devices</div>
            <div style={{fontSize:13,color:'var(--ink-3)'}}>4 accounts · 4 logged-in devices</div>
          </div>
          <button style={{fontSize:12,color:'var(--accent)',background:'var(--accent-2)',border:'none',padding:'8px 14px',borderRadius:8,cursor:'pointer',fontWeight:500}}>+ Invite user</button>
        </div>

        <div style={{background:'#fff',border:'1px solid var(--line)',borderRadius:12,marginBottom:24}}>
          <div style={{display:'grid',gridTemplateColumns:'1fr 96px 1fr 80px 80px',padding:'8px 18px',fontSize:11,color:'var(--ink-4)',fontWeight:600,letterSpacing:0.3,textTransform:'uppercase',gap:8}}>
            <span>User</span><span>Role</span><span>Device</span><span>Last seen</span><span>Status</span>
          </div>
          {users.map(u=>(
            <div key={u.email} style={{display:'grid',gridTemplateColumns:'1fr 96px 1fr 80px 80px',padding:'12px 18px',borderTop:'1px solid var(--line)',alignItems:'center',gap:8}}>
              <div>
                <div style={{fontSize:13,fontWeight:500,color:'var(--ink)'}}>{u.name}</div>
                <div style={{fontSize:11,color:'var(--ink-4)'}}>{u.email}</div>
              </div>
              <Tag tone={u.role==='Admin'?'accent':'neutral'}>{u.role}</Tag>
              <div style={{display:'flex',alignItems:'center',gap:6,fontSize:12,color:'var(--ink-3)'}}>
                <div style={{width:13,height:13}}>{ICONS.device}</div>{u.device}
              </div>
              <span style={{fontSize:11,color:'var(--ink-4)'}}>{u.last}</span>
              <div style={{display:'flex',alignItems:'center',gap:4}}>
                <Dot tone={u.status}/><span style={{fontSize:11,color:'var(--ink-4)'}}>{u.status==='ok'?'Online':'Offline'}</span>
              </div>
            </div>
          ))}
        </div>

        {/* Login settings */}
        <div style={{background:'#fff',border:'1px solid var(--line)',borderRadius:12,padding:'16px 18px'}}>
          <div style={{fontSize:13,fontWeight:600,color:'var(--ink)',marginBottom:14}}>Authentication Settings</div>
          {[
            ["Allow self-registration","Off"],
            ["Require email verification","On"],
            ["Session timeout","8 hours"],
            ["Max devices per user","3"],
          ].map(([k,v])=>(
            <div key={k} style={{display:'flex',justifyContent:'space-between',alignItems:'center',padding:'10px 0',borderBottom:'1px solid var(--line)'}}>
              <span style={{fontSize:13,color:'var(--ink)'}}>{k}</span>
              <span style={{fontSize:12,fontWeight:500,color:v==='On'?'var(--ok)':v==='Off'?'var(--ink-4)':'var(--accent-ink)'}}>{v}</span>
            </div>
          ))}
        </div>
      </div>
    </DashShell>
  );
}

Object.assign(window, { DashSessions, DashUsers });
