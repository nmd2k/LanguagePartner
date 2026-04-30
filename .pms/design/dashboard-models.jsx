// Dashboard: Model Hosting + Model Browser

function DashModels() {
  const hosted = [
    { name:"whisper-large-v3", role:"ASR", mem:"6.2 GB", status:"ok",  lat:"38ms", sessions:7 },
    { name:"nllb-200-distilled", role:"MT", mem:"2.1 GB", status:"ok",  lat:"21ms", sessions:7 },
    { name:"coqui-tts-zh",      role:"TTS", mem:"1.4 GB", status:"ok",  lat:"95ms", sessions:4 },
    { name:"whisper-medium",    role:"ASR", mem:"1.5 GB", status:"off", lat:"—",    sessions:0 },
  ];

  const catalog = [
    { name:"whisper-large-v3",    role:"ASR", size:"6.2 GB", rec:true,  desc:"Best accuracy, supports 99 languages" },
    { name:"whisper-medium",      role:"ASR", size:"1.5 GB", rec:false, desc:"Faster, lighter alternative" },
    { name:"nllb-200-distilled",  role:"MT",  size:"2.1 GB", rec:true,  desc:"Meta's 200-language translation model" },
    { name:"opus-mt-en-zh",       role:"MT",  size:"300 MB", rec:false, desc:"Lightweight EN→ZH only" },
    { name:"coqui-tts-zh",        role:"TTS", size:"1.4 GB", rec:true,  desc:"Natural Mandarin synthesis" },
    { name:"piper-en-us",         role:"TTS", size:"60 MB",  rec:false, desc:"Tiny, offline English TTS" },
  ];

  const roleColor = { ASR:"var(--accent)", MT:"var(--spk-b)", TTS:"var(--spk-c)" };

  return (
    <DashShell active="models">
      <div style={{padding:'24px 28px'}}>
        <div style={{fontSize:20,fontWeight:700,color:'var(--ink)',letterSpacing:-0.4,marginBottom:20}}>Model Hosting</div>

        {/* Hosted table */}
        <div style={{background:'#fff',border:'1px solid var(--line)',borderRadius:12,marginBottom:28}}>
          <div style={{display:'flex',alignItems:'center',padding:'14px 18px',borderBottom:'1px solid var(--line)'}}>
            <span style={{fontSize:13,fontWeight:600,color:'var(--ink)',flex:1}}>Running Models</span>
            <button style={{fontSize:12,color:'var(--accent)',background:'var(--accent-2)',border:'none',padding:'6px 12px',borderRadius:7,cursor:'pointer',fontWeight:500}}>+ Load model</button>
          </div>
          <div style={{display:'grid',gridTemplateColumns:'1fr 64px 80px 56px 56px 80px',padding:'8px 18px',fontSize:11,color:'var(--ink-4)',fontWeight:600,letterSpacing:0.3,textTransform:'uppercase',gap:8}}>
            <span>Name</span><span>Role</span><span>VRAM</span><span>Lat</span><span>Sess</span><span>Action</span>
          </div>
          {hosted.map(m => (
            <div key={m.name} style={{display:'grid',gridTemplateColumns:'1fr 64px 80px 56px 56px 80px',padding:'11px 18px',borderTop:'1px solid var(--line)',alignItems:'center',gap:8}}>
              <div style={{display:'flex',alignItems:'center',gap:8}}>
                <Dot tone={m.status}/>
                <span style={{fontSize:12,fontFamily:'var(--font-mono)',color:'var(--ink)'}}>{m.name}</span>
              </div>
              <Tag tone="neutral">{m.role}</Tag>
              <span style={{fontSize:12,color:'var(--ink-3)',fontVariantNumeric:'tabular-nums'}}>{m.mem}</span>
              <span style={{fontSize:12,color:'var(--ok)',fontFamily:'var(--font-mono)'}}>{m.lat}</span>
              <span style={{fontSize:12,color:'var(--ink-3)',fontVariantNumeric:'tabular-nums'}}>{m.sessions||'—'}</span>
              <button style={{fontSize:11,color:m.status==='ok'?'var(--err)':'var(--ok)',background:'none',border:'1px solid',borderColor:m.status==='ok'?'var(--err)':'var(--ok)',borderRadius:6,padding:'4px 8px',cursor:'pointer',fontWeight:500}}>{m.status==='ok'?'Unload':'Load'}</button>
            </div>
          ))}
        </div>

        {/* Model browser */}
        <div style={{fontSize:16,fontWeight:600,color:'var(--ink)',letterSpacing:-0.3,marginBottom:14}}>Model Browser</div>
        <div style={{display:'flex',gap:8,marginBottom:16,flexWrap:'wrap'}}>
          {['All','ASR','MT','TTS','Recommended'].map(f=>(
            <button key={f} style={{fontSize:12,padding:'5px 12px',borderRadius:20,border:'1px solid var(--line)',background:f==='All'?'var(--ink)':'#fff',color:f==='All'?'#fff':'var(--ink-2)',cursor:'pointer',fontWeight:f==='All'?600:400}}>{f}</button>
          ))}
        </div>
        <div style={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:10}}>
          {catalog.map(m=>(
            <div key={m.name} style={{background:'#fff',border:'1px solid var(--line)',borderRadius:10,padding:'14px 16px'}}>
              <div style={{display:'flex',alignItems:'flex-start',gap:8,marginBottom:6}}>
                <div style={{flex:1}}>
                  <div style={{fontSize:12,fontFamily:'var(--font-mono)',fontWeight:600,color:'var(--ink)',lineHeight:1.3}}>{m.name}</div>
                  <div style={{fontSize:11,color:'var(--ink-3)',marginTop:3}}>{m.desc}</div>
                </div>
                {m.rec && <Tag tone="accent">★ Rec</Tag>}
              </div>
              <div style={{display:'flex',alignItems:'center',gap:8,marginTop:10}}>
                <span style={{fontSize:10,color:'var(--ink-4)',flex:1}}>{m.size}</span>
                <Tag tone="neutral">{m.role}</Tag>
                <button style={{fontSize:11,color:'var(--accent)',background:'var(--accent-2)',border:'none',padding:'5px 10px',borderRadius:6,cursor:'pointer',fontWeight:600}}>Load</button>
              </div>
            </div>
          ))}
        </div>
      </div>
    </DashShell>
  );
}

Object.assign(window, { DashModels });
