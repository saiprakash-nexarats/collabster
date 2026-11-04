import {useEffect,useState} from 'react';

// Security: Purge any legacy auth tokens stored insecurely in localStorage
try {
  if (typeof localStorage !== 'undefined') {
    localStorage.removeItem('collobster:authSession');
  }
} catch (e) {}

export function useStoredState(key,initial){
 const [value,setValue]=useState(()=>{try{const raw=localStorage.getItem('collobster:'+key);return raw===null?initial:JSON.parse(raw)}catch{return initial}});
 const [error,setError]=useState(false);
 useEffect(()=>{try{localStorage.setItem('collobster:'+key,JSON.stringify(value));setError(false)}catch{setError(true)}},[key,value]);
 return [value,setValue,error];
}

export function useSessionStoredState(key,initial){
 const [value,setValue]=useState(()=>{try{const raw=sessionStorage.getItem('collobster:'+key);return raw===null?initial:JSON.parse(raw)}catch{return initial}});
 const [error,setError]=useState(false);
 useEffect(()=>{try{sessionStorage.setItem('collobster:'+key,JSON.stringify(value));setError(false)}catch{setError(true)}},[key,value]);
 return [value,setValue,error];
}

export const initialProfile={name:'Ananya Sharma',headline:'Product designer & curious builder',location:'Bengaluru, India',bio:'Making useful things with good people. Currently exploring design systems, Flutter, and community-led products.',skills:['Design','Figma','React','CSS'],interests:['Design','Projects','Learning'],roles:['member'],onboarded:false};
