// Android: Main translation screen + Language picker

function ScreenMain() {
  const msgs = [
    { id:1, spk:"A", color:"var(--spk-a)", lang:"EN", text:"Can we schedule the meeting for Thursday afternoon?", tx:"我们可以把会议安排在周四下午吗？", time:"0:04" },
    { id:2, spk:"B", color:"var(--spk-b)", lang:"ZH", text:"周四下午三点可以吗？", tx:"Can we do 3 PM Thursday?", time:"0:08" },
    { id:3, spk:"A", color:"var(--spk-a)", lang:"EN", text:"Perfect. I'll send the invite now.", tx:"好的，我现在发邀请。", time:"0:12" },
  ];

  return (
    <Phone title="Language Partner" action={<div style={{width:24,height:24}}>{ICONS.cog}</div>}>
      {/* Lang bar */}
      <div style={{display:'flex',alignItems:'center',gap:8,padding:'6px 16px 10px',borderBottom:'1px solid var(--line)'}}>
        <div style={{flex:1,background:'var(--bg-2)',borderRadius:8,padding:'7px 12px',fontSize:13,fontWeight:600,textAlign:'center',color:'var(--ink)'}}>English</div>
        <button style={{width:32,height:32,border:'none',background:'none',color:'var(--accent)',display:'flex',alignItems:'center',justifyContent:'center',cursor:'pointer'}}>
          <div style={{width:18,height:18}}>{ICONS.swap}</div>
        </button>
        <div style={{flex:1,background:'var(--bg-2)',borderRadius:8,padding:'7px 12px',fontSize:13,fontWeight:600,textAlign:'center',color:'var(--ink)'}}>普通话</div>
      </div>

      {/* Messages */}
      <div style={{flex:1,overflowY:'auto',padding:'12px 14px',display:'flex',flexDirection:'column',gap:14,height:440,overflowX:'hidden'}}>
        {msgs.map(m => (
          <div key={m.id} style={{display:'flex',flexDirection:'column',gap:4,alignItems:m.spk==='A'?'flex-end':'flex-start'}}>
            <SpeakerChip id={m.spk} color={m.color} small />
            <div style={{maxWidth:'82%',background:m.spk==='A'?'var(--accent-2)':'var(--bg-2)',borderRadius:m.spk==='A'?'14px 4px 14px 14px':'4px 14px 14px 14px',padding:'10px 13px'}}>
              <div style={{fontSize:13,color:'var(--ink)',lineHeight:1.5}}>{m.text}</div>
              <div style={{fontSize:11,color:'var(--ink-3)',marginTop:4,fontFamily:'var(--font-cjk)'}}>{m.tx}</div>
            </div>
            <div style={{fontSize:10,color:'var(--ink-4)',paddingHorizontal:2}}>{m.time}</div>
          </div>
        ))}

        {/* Live indicator */}
        <div style={{display:'flex',alignItems:'center',gap:10,padding:'10px 13px',background:'var(--ok-bg)',borderRadius:12,border:'1px solid var(--ok)'}}>
          <div style={{color:'var(--ok)',width:18,height:18,flexShrink:0}}>{ICONS.mic}</div>
          <Waveform active={true} bars={20} color="var(--ok)" height={22}/>
          <span style={{fontSize:11,color:'var(--ok)',fontWeight:500,marginLeft:'auto'}}>Listening…</span>
        </div>
      </div>

      {/* Bottom bar */}
      <div style={{padding:'10px 16px',display:'flex',gap:10,borderTop:'1px solid var(--line)'}}>
        <div style={{flex:1,background:'var(--bg-2)',borderRadius:24,height:44,display:'flex',alignItems:'center',paddingLeft:14,color:'var(--ink-4)',fontSize:13}}>Tap to type…</div>
        <button style={{width:44,height:44,borderRadius:22,background:'var(--accent)',border:'none',color:'#fff',display:'flex',alignItems:'center',justifyContent:'center',cursor:'pointer'}}>
          <div style={{width:20,height:20}}>{ICONS.mic}</div>
        </button>
      </div>
    </Phone>
  );
}

function ScreenLangPicker() {
  const langs = [
    {code:'EN',name:'English',native:'English'},
    {code:'ZH',name:'Mandarin',native:'普通话'},
    {code:'JA',name:'Japanese',native:'日本語'},
    {code:'ES',name:'Spanish',native:'Español'},
    {code:'FR',name:'French',native:'Français'},
    {code:'KO',name:'Korean',native:'한국어'},
    {code:'AR',name:'Arabic',native:'العربية'},
    {code:'DE',name:'German',native:'Deutsch'},
  ];
  return (
    <Phone title="Select Language" back bg="var(--bg)">
      <div style={{padding:'10px 14px 8px'}}>
        <div style={{background:'var(--bg-2)',borderRadius:10,display:'flex',alignItems:'center',padding:'9px 12px',gap:8,color:'var(--ink-3)'}}>
          <div style={{width:16,height:16}}>{ICONS.search}</div>
          <span style={{fontSize:13}}>Search language…</span>
        </div>
      </div>
      <div style={{padding:'0 14px',display:'flex',flexDirection:'column',gap:2}}>
        {langs.map((l,i) => (
          <div key={l.code} style={{display:'flex',alignItems:'center',padding:'12px 4px',borderBottom:'1px solid var(--line)',gap:12}}>
            <div style={{width:36,height:36,borderRadius:8,background:'var(--bg-2)',display:'flex',alignItems:'center',justifyContent:'center',fontSize:12,fontWeight:700,color:'var(--accent-ink)'}}>{l.code}</div>
            <div style={{flex:1}}>
              <div style={{fontSize:14,fontWeight:500,color:'var(--ink)'}}>{l.name}</div>
              <div style={{fontSize:12,color:'var(--ink-3)',fontFamily:'var(--font-cjk)'}}>{l.native}</div>
            </div>
            {i===0 && <div style={{width:18,height:18,color:'var(--ok)'}}>{ICONS.check}</div>}
          </div>
        ))}
      </div>
    </Phone>
  );
}

Object.assign(window, { ScreenMain, ScreenLangPicker });
