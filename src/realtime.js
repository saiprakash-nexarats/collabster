let activeSocket;

function frame(command,headers={},body=''){return `${command}\n${Object.entries(headers).map(([key,value])=>`${key}:${value}`).join('\n')}\n\n${body}\0`;}

export function parseRealtimeFrames(raw){return String(raw).replace(/\r\n/g,'\n').replace(/\r/g,'\n').split('\0').filter(Boolean).map(chunk=>{const [head,...parts]=chunk.split('\n\n');const [command,...lines]=head.split('\n');const headers=Object.fromEntries(lines.filter(Boolean).map(line=>{const index=line.indexOf(':');return index<0?[line,'']:[line.slice(0,index),line.slice(index+1)];}));const body=parts.join('\n\n');try{return {command,headers,body:body?JSON.parse(body):null};}catch{return {command,headers,body:null};}});}

export function sendRealtime(destination,body){if(activeSocket?.readyState!==WebSocket.OPEN)return false;activeSocket.send(frame('SEND',{destination,'content-type':'application/json'},JSON.stringify(body)));return true;}

export function getWebSocketUrl(){
 if(typeof window==='undefined')return'';
 const configured=import.meta.env?.VITE_WS_URL;
 if(configured)return configured;
 const apiUrl=import.meta.env?.VITE_API_URL;
 if(apiUrl&&/^https?:\/\//i.test(apiUrl)){
  try{
   const origin=new URL(apiUrl).origin;
   return origin.replace(/^http/i,'ws')+'/ws/community';
  }catch(_){}
 }
 const protocol=window.location.protocol==='https:'?'wss:':'ws:';
 return `${protocol}//${window.location.host}/ws/community`;
}

export function connectRealtime(accessToken,{onEvent,onState}={}){
 if(!accessToken||typeof WebSocket==='undefined')return()=>{};
 const url=getWebSocketUrl();
 if(!url)return()=>{};
 let socket;let closed=false;let retry;
 const open=()=>{if(closed)return;socket=new WebSocket(url);socket.onopen=()=>{activeSocket=socket;onState?.('connected');socket.send(frame('CONNECT',{Authorization:`Bearer ${accessToken}`,'accept-version':'1.2','heart-beat':'10000,10000'}));};socket.onmessage=event=>parseRealtimeFrames(event.data).forEach(packet=>{if(packet.command==='CONNECTED')socket.send(frame('SUBSCRIBE',{id:'collobster-updates',destination:'/user/queue/updates',ack:'auto'}));if(packet.command==='MESSAGE'&&packet.body)onEvent?.(packet.body);});socket.onclose=()=>{if(activeSocket===socket)activeSocket=undefined;if(!closed){onState?.('disconnected');retry=setTimeout(open,3000);}};socket.onerror=()=>socket.close();};open();return()=>{closed=true;clearTimeout(retry);if(socket?.readyState===WebSocket.OPEN)socket.send(frame('DISCONNECT'));socket?.close();};
}
