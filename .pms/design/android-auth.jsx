// Android: Login + Server Setup screens

function ScreenLogin() {
  return (
    <Phone noChrome bg="var(--bg)" title="Login">
      <div style={{display:'flex',flexDirection:'column',height:'100%',padding:'32px 24px 16px'}}>
        {/* Logo */}
        <div style={{display:'flex',flexDirection:'column',alignItems:'center',marginBottom:32}}>
          <div style={{width:56,height:56,borderRadius:16,background:'var(--accent)',display:'flex',alignItems:'center',justifyContent:'center',marginBottom:12}}>
            <div style={{width:28,height:28,color:'#fff'}}>{ICONS.globe}</div>
          </div>
          <div style={{fontSize:20,fontWeight:700,letterSpacing:-0.5,color:'var(--ink)',fontFamily:'var(--font-display)'}}>Language Partner</div>
          <div style={{fontSize:13,color:'var(--ink-3)',marginTop:4}}>Sign in to your account</div>
        </div>

        {/* Fields */}
        <div style={{display:'flex',flexDirection:'column',gap:12,marginBottom:20}}>
          <div>
            <div style={{fontSize:12,fontWeight:500,color:'var(--ink-2)',marginBottom:6}}>Email</div>
            <div style={{background:'var(--bg-2)',border:'1.5px solid var(--line-2)',borderRadius:10,padding:'12px 14px',fontSize:14,color:'var(--ink-4)'}}>you@example.com</div>
          </div>
          <div>
            <div style={{fontSize:12,fontWeight:500,color:'var(--ink-2)',marginBottom:6}}>Password</div>
            <div style={{background:'var(--bg-2)',border:'1.5px solid var(--accent)',borderRadius:10,padding:'12px 14px',fontSize:14,color:'var(--ink)',display:'flex',justifyContent:'space-between',alignItems:'center'}}>
              <span>••••••••</span>
              <div style={{width:16,height:16,color:'var(--ink-3)'}}>{ICONS.eye}</div>
            </div>
          </div>
        </div>

        <div style={{textAlign:'right',marginBottom:24}}>
          <span style={{fontSize:12,color:'var(--accent)'}}>Forgot password?</span>
        </div>

        <button style={{width:'100%',padding:'14px',background:'var(--accent)',border:'none',borderRadius:12,color:'#fff',fontSize:15,fontWeight:600,cursor:'pointer',letterSpacing:0.1}}>Sign In</button>

        <div style={{display:'flex',alignItems:'center',gap:10,margin:'20px 0'}}>
          <div style={{flex:1,height:1,background:'var(--line)'}}/>
          <span style={{fontSize:12,color:'var(--ink-4)'}}>or</span>
          <div style={{flex:1,height:1,background:'var(--line)'}}/>
        </div>

        <button style={{width:'100%',padding:'13px',background:'var(--bg-2)',border:'1.5px solid var(--line-2)',borderRadius:12,color:'var(--ink)',fontSize:14,fontWeight:500,cursor:'pointer',display:'flex',alignItems:'center',justifyContent:'center',gap:8}}>
          <div style={{width:16,height:16,color:'var(--ink-3)'}}>{ICONS.server}</div>
          Sign in with SSO
        </button>

        <div style={{marginTop:'auto',textAlign:'center',fontSize:12,color:'var(--ink-4)'}}>
          Need an account? <span style={{color:'var(--accent)'}}>Contact admin</span>
        </div>
      </div>
    </Phone>
  );
}

function ScreenServerSetup() {
  return (
    <Phone title="Server Setup" back bg="var(--bg)">
      <div style={{padding:'16px 18px',display:'flex',flexDirection:'column',gap:20}}>
        {/* URL field */}
        <div>
          <div style={{fontSize:13,fontWeight:600,color:'var(--ink)',marginBottom:8}}>Server URL</div>
          <div style={{background:'var(--bg-2)',border:'1.5px solid var(--accent)',borderRadius:10,padding:'12px 14px',display:'flex',alignItems:'center',gap:8}}>
            <span style={{fontSize:11,fontFamily:'var(--font-mono)',color:'var(--ink-3)'}}>https://</span>
            <span style={{fontSize:13,fontFamily:'var(--font-mono)',color:'var(--ink)',flex:1}}>partner.example.com</span>
          </div>
          <div style={{marginTop:6,fontSize:11,color:'var(--ink-4)'}}>e.g. https://your-server.com or 192.168.1.10:8080</div>
        </div>

        {/* Connection test */}
        <div style={{background:'var(--ok-bg)',border:'1px solid var(--ok)',borderRadius:10,padding:'12px 14px',display:'flex',gap:12,alignItems:'center'}}>
          <Dot tone="ok"/>
          <div>
            <div style={{fontSize:13,fontWeight:600,color:'var(--ok)'}}>Connected</div>
            <div style={{fontSize:11,color:'var(--ink-3)'}}>Server v2.4.1 · 42 ms latency</div>
          </div>
          <button style={{marginLeft:'auto',fontSize:12,color:'var(--accent)',background:'none',border:'none',cursor:'pointer',fontWeight:500}}>Re-test</button>
        </div>

        {/* Auth token */}
        <div>
          <div style={{fontSize:13,fontWeight:600,color:'var(--ink)',marginBottom:8}}>API Token <span style={{fontSize:11,color:'var(--ink-4)',fontWeight:400}}>(optional)</span></div>
          <div style={{background:'var(--bg-2)',border:'1.5px solid var(--line-2)',borderRadius:10,padding:'12px 14px',fontFamily:'var(--font-mono)',fontSize:12,color:'var(--ink-3)'}}>sk-••••••••••••••••2f9a</div>
        </div>

        {/* Recent servers */}
        <div>
          <div style={{fontSize:12,fontWeight:600,color:'var(--ink-3)',letterSpacing:0.3,textTransform:'uppercase',marginBottom:10}}>Recent</div>
          {['partner.corp.local','192.168.0.42:8080'].map(s => (
            <div key={s} style={{display:'flex',alignItems:'center',gap:10,padding:'10px 4px',borderBottom:'1px solid var(--line)'}}>
              <div style={{width:14,height:14,color:'var(--ink-3)'}}>{ICONS.server}</div>
              <span style={{fontSize:12,fontFamily:'var(--font-mono)',color:'var(--ink-2)',flex:1}}>{s}</span>
              <div style={{width:14,height:14,color:'var(--ink-3)'}}>{ICONS.chev}</div>
            </div>
          ))}
        </div>

        <button style={{width:'100%',padding:'14px',background:'var(--accent)',border:'none',borderRadius:12,color:'#fff',fontSize:15,fontWeight:600,cursor:'pointer'}}>Save & Connect</button>
      </div>
    </Phone>
  );
}

Object.assign(window, { ScreenLogin, ScreenServerSetup });
