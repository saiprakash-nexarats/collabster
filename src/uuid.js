export function uuid(cryptoApi=globalThis.crypto){
  if(cryptoApi?.randomUUID&&cryptoApi.randomUUID!==uuid)return cryptoApi.randomUUID();
  if(cryptoApi?.getRandomValues){
    const bytes=new Uint8Array(16);cryptoApi.getRandomValues(bytes);
    bytes[6]=(bytes[6]&15)|64;bytes[8]=(bytes[8]&63)|128;
    return [...bytes].map((value,index)=>[4,6,8,10].includes(index)?'-'+value.toString(16).padStart(2,'0'):value.toString(16).padStart(2,'0')).join('');
  }
  return `${Date.now().toString(16)}-${Math.random().toString(16).slice(2)}-${Math.random().toString(16).slice(2)}`;
}
