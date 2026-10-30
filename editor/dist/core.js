(function (root) {
  'use strict';
  const MAX_BYTES = 131072, MAX_CODE = 30000;
  const actions = new Set(['none','close','message','command','refresh']);
  function fail(message) { throw new Error(message); }
  function str(value, fallback, max, key) {
    if (value === undefined) return fallback;
    if (typeof value !== 'string' || value.length > max || value.includes('\0')) fail(`${key} must be text of at most ${max} characters.`);
    return value;
  }
  function int(value, fallback, min, max, key) {
    value ??= fallback;
    if (!Number.isInteger(value) || value < min || value > max) fail(`${key} must be a whole number from ${min} to ${max}.`);
    return value;
  }
  function item(value, known) {
    if (typeof value !== 'string' || !/^minecraft:[a-z0-9_./-]+$/.test(value) || value === 'minecraft:air' || value.length > 100) fail(`Invalid vanilla item: ${value}`);
    if (known && !known.has(value)) fail(`Unknown Minecraft 26.2 item: ${value}`);
    return value;
  }
  function validate(raw, known) {
    if (!raw || typeof raw !== 'object' || Array.isArray(raw)) fail('Choose a Profile Studio JSON layout.');
    if (new TextEncoder().encode(JSON.stringify(raw)).length > MAX_BYTES) fail('Layout exceeds 128 KiB.');
    const result = {schemaVersion:int(raw.schemaVersion,1,1,1,'Schema version'),title:str(raw.title,"{player}'s Profile",80,'Title'),rows:int(raw.rows,6,1,6,'Rows'),filler:raw.filler == null ? null : item(raw.filler,known),items:[]};
    if (!Array.isArray(raw.items) || raw.items.length > result.rows*9) fail('Invalid items list.');
    const slots = new Set();
    for (const e of raw.items) {
      if (!e || typeof e !== 'object') fail('Invalid item entry.');
      const slot = int(e.slot,-1,0,result.rows*9-1,'Slot');
      if (slots.has(slot)) fail(`Duplicate slot ${slot+1}.`); slots.add(slot);
      const lore = e.lore ?? [];
      if (!Array.isArray(lore) || lore.length > 16) fail('Use at most 16 hover text lines.');
      if (e.glint !== undefined && typeof e.glint !== 'boolean') fail('Glow must be true or false.');
      if (e.action !== undefined && (!e.action || typeof e.action !== 'object' || Array.isArray(e.action))) fail('Invalid item action.');
      const type = str(e.action?.type,'none',16,'Action'), value = str(e.action?.value,'',512,'Action value');
      if (!actions.has(type)) fail(`Unknown action: ${type}`);
      const command = value.trim().replace(/^\//,'');
      if (type === 'command' && (!command || /[\r\n]/.test(command))) fail('Enter one command, such as balance.');
      result.items.push({slot,item:item(e.item,known),count:int(e.count,1,1,64,'Amount'),name:str(e.name,'',160,'Display name'),lore:lore.map(x=>str(x,'',256,'Hover text line')),glint:e.glint??false,action:{type,value:type==='command'?command:value}});
    }
    result.items.sort((a,b)=>a.slot-b.slot);
    return result;
  }
  function encode(layout) {
    const bytes = new TextEncoder().encode(JSON.stringify(layout));
    let binary = ''; for (const byte of bytes) binary += String.fromCharCode(byte);
    return 'ps1:' + btoa(binary).replace(/\+/g,'-').replace(/\//g,'_').replace(/=+$/,'');
  }
  function decode(code, known) {
    if (typeof code !== 'string' || !code.startsWith('ps1:') || code.length > MAX_CODE) fail('Invalid or oversized PROFILE-lyn import code.');
    const body = code.slice(4);
    if (!/^[A-Za-z0-9_-]*$/.test(body)) fail('Invalid import code characters.');
    const binary = atob(body.replace(/-/g,'+').replace(/_/g,'/'));
    return validate(JSON.parse(new TextDecoder('utf-8',{fatal:true}).decode(Uint8Array.from(binary,c=>c.charCodeAt(0)))),known);
  }
  function move(layout, from, to) {
    const copy = structuredClone(layout), source = copy.items.find(e=>e.slot===from), target = copy.items.find(e=>e.slot===to);
    if (!source || from === to || to < 0 || to >= copy.rows*9) return copy;
    source.slot = to; if (target) target.slot = from;
    return copy;
  }
  const core = {validate,encode,decode,move,MAX_CODE,MAX_BYTES};
  root.ProfileCore = core;
  if (typeof module !== 'undefined' && module.exports) module.exports = core;
})(globalThis);
