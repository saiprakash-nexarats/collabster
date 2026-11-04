const env=import.meta.env||{};
const issuer=(env.VITE_OIDC_ISSUER||'').replace(/\/$/,'');
const clientId=env.VITE_OIDC_CLIENT_ID||'';
const authorizationEndpoint=env.VITE_OIDC_AUTHORIZATION_ENDPOINT||`${issuer}/authorize`;
const tokenEndpoint=env.VITE_OIDC_TOKEN_ENDPOINT||`${issuer}/oauth/token`;
const redirectUri=env.VITE_OIDC_REDIRECT_URI||(globalThis.location?`${location.origin}${location.pathname}`:'');
const storageKey='collobster:oidc:pending';

export const oidcConfigured=Boolean(issuer&&clientId&&env.VITE_OIDC_AUTHORIZATION_ENDPOINT&&env.VITE_OIDC_TOKEN_ENDPOINT);

function base64Url(bytes){return btoa(String.fromCharCode(...bytes)).replace(/\+/g,'-').replace(/\//g,'_').replace(/=+$/,'')}
function randomValue(){const bytes=new Uint8Array(32);crypto.getRandomValues(bytes);return base64Url(bytes)}
async function challenge(verifier){return base64Url(new Uint8Array(await crypto.subtle.digest('SHA-256',new TextEncoder().encode(verifier))))}

export async function beginOidcLogin(provider){
 if(!oidcConfigured)throw new Error('OIDC_NOT_CONFIGURED');
 const state=randomValue(),verifier=randomValue();
 sessionStorage.setItem(storageKey,JSON.stringify({state,verifier,provider,createdAt:Date.now()}));
 const query=new URLSearchParams({client_id:clientId,redirect_uri:redirectUri,response_type:'code',scope:'openid profile email',state,code_challenge:await challenge(verifier),code_challenge_method:'S256',provider_hint:provider.toLowerCase()});
 location.assign(`${authorizationEndpoint}?${query}`);
}

export function hasOidcCallback(){return new URLSearchParams(location.search).has('code')||new URLSearchParams(location.search).has('error')}

export async function finishOidcLogin(){
 const query=new URLSearchParams(location.search),error=query.get('error');
 if(error)throw new Error(query.get('error_description')||error);
 const code=query.get('code'),state=query.get('state');
 const raw=sessionStorage.getItem(storageKey);
 if(!code||!raw)throw new Error('OIDC_CALLBACK_INVALID');
 const pending=JSON.parse(raw);
 if(pending.state!==state||Date.now()-pending.createdAt>600000)throw new Error('OIDC_CALLBACK_INVALID');
 const response=await fetch(tokenEndpoint,{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:new URLSearchParams({grant_type:'authorization_code',code,client_id:clientId,redirect_uri:redirectUri,code_verifier:pending.verifier})});
 if(!response.ok)throw new Error('OIDC_TOKEN_EXCHANGE_FAILED');
 const tokens=await response.json();
 sessionStorage.removeItem(storageKey);
 history.replaceState({},'',location.pathname+location.hash);
 return {...tokens,provider:pending.provider,claims:decodeJwt(tokens.id_token)};
}

export function decodeJwt(token){try{const value=token.split('.')[1].replace(/-/g,'+').replace(/_/g,'/');const padded=value.padEnd(Math.ceil(value.length/4)*4,'=');const binary=atob(padded);const bytes=Uint8Array.from(binary,character=>character.charCodeAt(0));return JSON.parse(new TextDecoder('utf-8').decode(bytes))}catch{return {}}}
