import React from 'react';
import {createRoot} from 'react-dom/client';
import App from './App.jsx';
import {uuid} from './uuid';
import './styles.css';
import './workflows.css';

const cryptoApi=globalThis.crypto||{};
if(!cryptoApi.randomUUID){cryptoApi.randomUUID=uuid;}
if(!globalThis.crypto){globalThis.crypto=cryptoApi;}

class ErrorBoundary extends React.Component {
  state={error:false};
  static getDerivedStateFromError(){return {error:true};}
  render(){return this.state.error?<main className="fatal"><h1>Let's try that again.</h1><p>Something interrupted this view. Your saved changes are still on this device.</p><button onClick={()=>location.reload()}>Reload Collobster</button></main>:this.props.children;}
}
createRoot(document.getElementById('root')).render(<React.StrictMode><ErrorBoundary><App/></ErrorBoundary></React.StrictMode>);
