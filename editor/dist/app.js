(() => {
  'use strict';
  const $ = id => document.getElementById(id), C = ProfileCore, catalog = ITEM_CATALOG;
  const byId = new Map(catalog.map(i=>[i.id,i])), known = new Set(byId.keys());
  const initial = C.validate(globalThis.PROFILE_SERVER_LAYOUT ?? DEFAULT_LAYOUT,known);
  const KEY = C.draftKey(initial,location.pathname);
  let state = initial, selected = 22, filter = 'favorites', undo = [], redo = [], drag = null, lastText = $('lore'), toastTimer;
  const essentials = ['player_head','emerald','diamond','gold_ingot','iron_ingot','diamond_sword','netherite_sword','diamond_pickaxe','netherite_pickaxe','diamond_chestplate','book','writable_book','enchanted_book','map','filled_map','clock','compass','recovery_compass','ender_chest','chest','crafting_table','experience_bottle','name_tag','barrier','redstone_torch','redstone','paper','totem_of_undying','ender_pearl','ender_eye','nether_star','apple','golden_apple','cooked_beef','grass_block','oak_log','stone','amethyst_shard','heart_of_the_sea','firework_rocket','gray_stained_glass_pane','black_stained_glass_pane','lime_stained_glass_pane','red_stained_glass_pane','sunflower','beacon','bundle','elytra'].map(x=>'minecraft:'+x);
  const sample = {'{player}':'Steve','{money}':'$1,250.00','{level}':'24','{health}':'20','{food}':'20','{x}':'128','{y}':'64','{z}':'-256','{online}':'12','{uuid}':'00000000-0000-0000-0000-000000000000'};
  function preview(text) { for (const [token,value] of Object.entries(sample)) text = text.split(token).join(value); return text.replace(/\{score:[^}]+}/g,'42'); }
  function toast(text) { $('toast').textContent=text; $('toast').hidden=false; clearTimeout(toastTimer); toastTimer=setTimeout(()=>$('toast').hidden=true,4500); }
  function persist() { try { localStorage.setItem(KEY,JSON.stringify(state)); $('save-status').textContent='Saved in this browser'; } catch { $('save-status').textContent='Export to save your changes'; } }
  function commit(candidate) {
    try {
      const checked = C.validate(candidate,known);
      if (JSON.stringify(checked) === JSON.stringify(state)) return true;
      undo.push(structuredClone(state)); if (undo.length>80) undo.shift(); redo=[]; state=checked;
      selected=Math.min(selected,state.rows*9-1); persist(); render(); return true;
    } catch(e) { toast(e.message); render(); return false; }
  }
  function edit(fn) { const candidate = structuredClone(state); fn(candidate); return commit(candidate); }
  function active() { return state.items.find(x=>x.slot===selected); }
  function editItem(fn) { edit(s=>{const entry=s.items.find(x=>x.slot===selected); if(entry) fn(entry);}); }
  function icon(id,element) {
    element.className = 'item-icon' + (element.id==='selected-icon'?' big':'');
    const found=byId.get(id);
    element.dataset.sprite = found?.sprite??'';
    if (found?.icon) element.style.backgroundImage=`url("${found.icon}")`;
    else {element.style.backgroundImage=''; element.classList.add('fallback');}
  }
  function makeIcon(id) { const e=document.createElement('span'); icon(id,e); return e; }
  function add(id,slot=selected) {
    const found=byId.get(id); if(!found) return toast('Choose an item from the Minecraft 26.2 library.');
    selected=slot;
    edit(s=>{
      s.items=s.items.filter(e=>e.slot!==slot);
      s.items.push({slot,item:id,count:1,name:'',lore:[],glint:false,action:{type:id==='minecraft:barrier'?'close':'none',value:''}});
    });
  }
  function renderCatalog() {
    const query=$('search').value.toLowerCase().trim();
    let items = filter==='favorites' && !query ? essentials.map(id=>byId.get(id)).filter(Boolean) : catalog;
    if(query) items=items.filter(i=>i.name.toLowerCase().includes(query)||i.id.includes(query));
    $('item-count').textContent=catalog.length.toLocaleString();
    $('catalog').replaceChildren();
    if(!items.length) {const e=document.createElement('p');e.className='catalog-empty';e.textContent='No items found. Try an item name or ID.';$('catalog').append(e);}
    for(const item of items) {
      const button=document.createElement('button');button.className='catalog-item';button.draggable=true;button.title=item.name;button.setAttribute('aria-label',`Place ${item.name}`);button.append(makeIcon(item.id));
      button.addEventListener('click',()=>add(item.id));
      button.addEventListener('dragstart',e=>{drag={item:item.id};e.dataTransfer.setData('text/plain',item.id);e.dataTransfer.effectAllowed='copy';hideTip();});
      button.addEventListener('dragend',()=>drag=null);
      $('catalog').append(button);
    }
  }
  function render() {
    $('menu-title').value=state.title;$('rows').value=state.rows;$('filler').value=state.filler??'';
    $('preview-title').textContent=preview(state.title).replace(/&[0-9a-fk-or]/gi,'');
    $('slot-count').textContent=`${state.rows*9} slots · ${state.items.length} items`;
    $('undo').disabled=!undo.length;$('redo').disabled=!redo.length;
    $('inventory').replaceChildren();
    for(let i=0;i<state.rows*9;i++) {
      const e=state.items.find(x=>x.slot===i), slot=document.createElement('button');
      slot.className='slot'+(selected===i?' selected':'')+(!e&&state.filler?' filler-slot':'');
      slot.dataset.slot=i; slot.setAttribute('aria-label',`Slot ${i+1}: ${e?byId.get(e.item)?.name??e.item:'empty'}`);slot.setAttribute('aria-pressed',String(selected===i));slot.draggable=!!e;
      if(e||state.filler) slot.append(makeIcon(e?.item??state.filler));
      if(e?.count>1) {const count=document.createElement('span');count.className='slot-count';count.textContent=e.count;slot.append(count);}
      slot.addEventListener('click',()=>{selected=i;hideTip();render();});
      slot.addEventListener('keydown',event=>{
        const movement={ArrowLeft:-1,ArrowRight:1,ArrowUp:-9,ArrowDown:9}[event.key];
        if(movement!==undefined) {event.preventDefault();selected=Math.max(0,Math.min(state.rows*9-1,i+movement));render();$('inventory').children[selected].focus();}
      });
      slot.addEventListener('dragstart',event=>{drag={from:i};event.dataTransfer.setData('text/plain',String(i));event.dataTransfer.effectAllowed='move';hideTip();});
      slot.addEventListener('dragend',()=>{drag=null;document.querySelectorAll('.drag-over').forEach(e=>e.classList.remove('drag-over'));});
      slot.addEventListener('dragover',event=>{if(drag){event.preventDefault();slot.classList.add('drag-over');}});
      slot.addEventListener('dragleave',()=>slot.classList.remove('drag-over'));
      slot.addEventListener('drop',event=>{event.preventDefault();slot.classList.remove('drag-over');if(!drag)return;selected=i;if(drag.item)add(drag.item,i);else commit(C.move(state,drag.from,i));drag=null;});
      if(e) {slot.addEventListener('pointerenter',event=>showTip(e,event));slot.addEventListener('pointermove',positionTip);slot.addEventListener('pointerleave',hideTip);}
      $('inventory').append(slot);
    }
    renderInspector();
  }
  function renderInspector() {
    $('selected-slot').textContent=`SLOT ${selected+1}`;
    const e=active();$('empty-inspector').hidden=!!e;$('properties').hidden=!e;if(!e)return;
    icon(e.item,$('selected-icon'));$('selected-name').textContent=byId.get(e.item)?.name??e.item;$('selected-id').textContent=e.item;
    $('item-id').value=e.item;$('amount').value=e.count;$('glint').checked=e.glint;$('display-name').value=e.name;$('lore').value=e.lore.join('\n');$('action').value=e.action.type;$('action-value').value=e.action.value;
    const isCommand=e.action.type==='command',isMessage=e.action.type==='message';
    $('action-value-label').hidden=!(isCommand||isMessage);
    $('action-value-label').firstChild.textContent=isCommand?'Command':'Message';
    $('action-value').placeholder=isCommand?'balance':'&aHello, {player}!';
    $('action-hint').textContent=isCommand?"Runs with the player's own permissions. No leading / needed.":e.action.type==='close'?'Closes the profile when clicked.':e.action.type==='refresh'?'Refreshes the live values in this menu.':isMessage?'Only the player who clicks sees this message.':'A display item with no click action.';
  }
  const colors=['#000','#00a','#0a0','#0aa','#a00','#a0a','#fa0','#aaa','#555','#55f','#5f5','#5ff','#f55','#f5f','#ff5','#fff'];
  function colored(text,element) {
    let color='#aaa',bold=false,italic=false,underline=false,strike=false;
    for(const part of preview(text).split(/(&[0-9a-fk-or])/gi)) {
      if(/^&[0-9a-fk-or]$/i.test(part)) {const c=part[1].toLowerCase(), index=parseInt(c,16);if(index>=0&&index<16){color=colors[index];bold=italic=underline=strike=false;}else if(c==='r'){color='#aaa';bold=italic=underline=strike=false;}else if(c==='l')bold=true;else if(c==='o')italic=true;else if(c==='n')underline=true;else if(c==='m')strike=true;continue;}
      const span=document.createElement('span');span.textContent=part;span.style.color=color;span.style.fontWeight=bold?'bold':'normal';span.style.fontStyle=italic?'italic':'normal';span.style.textDecoration=[underline?'underline':'',strike?'line-through':''].join(' ');element.append(span);
    }
  }
  function showTip(e,event) {
    $('tooltip').replaceChildren();const title=document.createElement('div');title.className='tooltip-name';colored(e.name||'&f'+(byId.get(e.item)?.name??e.item),title);$('tooltip').append(title);
    for(const line of e.lore){const div=document.createElement('div');colored(line||' ',div);$('tooltip').append(div);}
    $('tooltip').hidden=false;positionTip(event);
  }
  function positionTip(event) {const r=$('tooltip').getBoundingClientRect();$('tooltip').style.left=Math.max(8,Math.min(event.clientX+15,innerWidth-r.width-10))+'px';$('tooltip').style.top=Math.max(8,Math.min(event.clientY+10,innerHeight-r.height-10))+'px';}
  function hideTip() {$('tooltip').hidden=true;}
  function confirmChange(title,text,action) {const dialog=$('confirm-dialog');$('confirm-title').textContent=title;$('confirm-text').textContent=text;$('confirm-ok').onclick=()=>{dialog.close();action();};dialog.showModal();}
  function history(direction) {const from=direction==='undo'?undo:redo,to=direction==='undo'?redo:undo;if(!from.length)return;to.push(structuredClone(state));state=from.pop();selected=Math.min(selected,state.rows*9-1);persist();render();}
  function download() {const blob=new Blob([JSON.stringify(state,null,2)+'\n'],{type:'application/json'}),url=URL.createObjectURL(blob),a=document.createElement('a');a.href=url;a.download='profile.json';a.click();setTimeout(()=>URL.revokeObjectURL(url),1000);toast('Downloaded profile.json');}
  async function copy(text) {try {await navigator.clipboard.writeText(text);toast('Copied to clipboard');}catch {const input=document.createElement('textarea');input.value=text;(document.querySelector('dialog[open]')||document.body).append(input);input.select();const ok=document.execCommand('copy');input.remove();toast(ok?'Copied to clipboard':'Copy is unavailable here. Select the command and copy it manually.');}}
  $('menu-title').addEventListener('change',()=>edit(s=>s.title=$('menu-title').value));
  $('rows').addEventListener('change',()=>{const rows=Number($('rows').value);if(state.items.some(e=>e.slot>=rows*9)){const prior=state.rows;$('rows').value=prior;confirmChange('Make the inventory smaller?','Items in removed rows will be deleted. You can undo this change.',()=>edit(s=>{s.rows=rows;s.items=s.items.filter(e=>e.slot<rows*9);}));}else edit(s=>s.rows=rows);});
  $('filler').addEventListener('change',()=>edit(s=>s.filler=$('filler').value||null));
  $('search').addEventListener('input',renderCatalog);
  document.querySelectorAll('[data-filter]').forEach(button=>button.addEventListener('click',()=>{filter=button.dataset.filter;document.querySelectorAll('[data-filter]').forEach(x=>x.classList.toggle('active',x===button));renderCatalog();}));
  $('clear').onclick=()=>confirmChange('Start with an empty inventory?','All display items and background panes will be cleared. Undo is available.',()=>{edit(s=>{s.items=[];s.filler=null;});toast('Inventory cleared. Pick an item to start.');});
  $('template').onclick=()=>confirmChange('Restore the starter template?','This replaces your layout with the default profile. You can undo it.',()=>commit(DEFAULT_LAYOUT));
  $('undo').onclick=()=>history('undo');$('redo').onclick=()=>history('redo');
  $('item-id').onchange=()=>{const id=$('item-id').value.trim();editItem(e=>e.item=id.includes(':')?id:'minecraft:'+id);};
  $('amount').onchange=()=>editItem(e=>e.count=Number($('amount').value));
  $('glint').onchange=()=>editItem(e=>e.glint=$('glint').checked);
  $('display-name').onchange=()=>editItem(e=>e.name=$('display-name').value);
  $('lore').onchange=()=>editItem(e=>e.lore=$('lore').value===''?[]:$('lore').value.split('\n'));
  $('action').onchange=()=>editItem(e=>{e.action.type=$('action').value;if(e.action.type==='command'&&!e.action.value)e.action.value='balance';});
  $('action-value').onchange=()=>editItem(e=>e.action.value=$('action-value').value);
  $('remove').onclick=()=>edit(s=>s.items=s.items.filter(e=>e.slot!==selected));
  $('duplicate').onclick=()=>{const e=active();const empty=Array.from({length:state.rows*9},(_,i)=>i).find(i=>!state.items.some(e=>e.slot===i));if(empty===undefined)return toast('No empty slots.');const copied=structuredClone(e);copied.slot=empty;selected=empty;edit(s=>s.items.push(copied));};
  for(const id of ['lore','display-name','action-value']) $(id).addEventListener('focus',()=>lastText=$(id));
  function insertToken(token) {if(!active())return toast('Place an item first.');const start=lastText.selectionStart??lastText.value.length,end=lastText.selectionEnd??start;lastText.value=lastText.value.slice(0,start)+token+lastText.value.slice(end);lastText.dispatchEvent(new Event('change'));lastText.focus();lastText.setSelectionRange(start+token.length,start+token.length);}
  document.querySelectorAll('[data-token]').forEach(b=>b.onclick=()=>insertToken(b.dataset.token));
  for(const token of Object.keys(sample)){const b=document.createElement('button');b.textContent=token;b.onclick=()=>{$('help-dialog').close();insertToken(token);};$('token-reference').append(b);}
  $('more-tokens').onclick=$('help-button').onclick=()=>$('help-dialog').showModal();
  $('export-button').onclick=()=>{const code=C.encode(state),tooLong=code.length>C.MAX_CODE;$('export-code').value=tooLong?'This layout is too large for one command. Download profile.json below.':'/profile import '+code;$('copy-command').disabled=tooLong;$('code-warning').hidden=!tooLong;$('code-warning').textContent='Use the JSON file for layouts larger than 30,000 code characters.';$('export-dialog').showModal();};
  $('copy-command').onclick=()=>copy($('export-code').value);
  $('download-json').onclick=download;
  $('import-button').onclick=()=>$('import-file').click();
  $('import-file').onchange=async()=>{const file=$('import-file').files[0];$('import-file').value='';if(!file)return;try{if(file.size>C.MAX_BYTES)throw new Error('JSON file exceeds 128 KiB.');const candidate=C.validate(JSON.parse(await file.text()),known);confirmChange('Import this layout?',`${candidate.title} · ${candidate.rows} rows · ${candidate.items.length} items. This replaces the current layout; undo is available.`,()=>{commit(candidate);toast('Layout imported');});}catch(e){toast(e.message);}};
  document.querySelectorAll('[data-close]').forEach(button=>button.onclick=()=>button.closest('dialog').close());$('confirm-cancel').onclick=()=>$('confirm-dialog').close();
  document.addEventListener('keydown',event=>{if(/INPUT|TEXTAREA|SELECT/.test(event.target.tagName)||document.querySelector('dialog[open]'))return;if((event.ctrlKey||event.metaKey)&&['z','y'].includes(event.key.toLowerCase())){event.preventDefault();history(event.key.toLowerCase()==='y'||event.shiftKey?'redo':'undo');}if(event.key==='Delete'&&active())$('remove').click();});
  for(let i=0;i<27;i++){const slot=document.createElement('div');slot.className='slot';$('player-inventory').append(slot);}
  for(const item of catalog){const option=document.createElement('option');option.value=item.id;option.label=item.name;$('item-ids').append(option);}
  try {const shared=new URLSearchParams(location.hash.slice(1)).get('layout');if(shared){state=C.decode(shared,known);persist();historyReplace();toast('Loaded layout from your editor link');}else{const saved=localStorage.getItem(KEY);if(saved)state=C.validate(JSON.parse(saved),known);}} catch(e){toast('Could not load saved layout: '+e.message);}
  function historyReplace(){window.history.replaceState(null,'',location.pathname+location.search);}
  selected=Math.min(selected,state.rows*9-1);renderCatalog();render();
})();
