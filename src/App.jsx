import React,{useEffect,useState,useRef} from 'react';
import {Home,Compass,BookOpen,BriefcaseBusiness,FolderKanban,Building2,ChartNoAxesCombined,MapPin,CalendarDays,Users,MessageCircle,Bell,Settings,Plus,Search,Moon,Sun,ChevronDown,ArrowUpRight,Bookmark,Menu,X,ArrowRight,WifiOff,ArrowLeft,User} from 'lucide-react';
import {AppContext} from './context';
import {useStoredState,useSessionStoredState,initialProfile} from './store';
import {initialPosts,initialThreads,initialComments,notifications as seededNotifications,people,courses,events,communities} from './data';
import {Avatar,Button,IconButton,Dialog,Tag,SectionTitle,Progress,Media,NotFound,PolicyDialog} from './components';
import {GraduationCap} from 'lucide-react';
import {api} from './api';
import {connectRealtime,sendRealtime} from './realtime';
import Feed from './features/Feed';
import {Learning,Career,Projects,Company,Investment,Services,Events,Community,Profile,SettingsPage,Explore} from './features/Spaces';
import {Messages,Notifications,CallModal,MinimizedCallBar,initialCallLog,formatDuration} from './features/Inbox';
import {Composer,Onboarding,Reader,Details,ProfileEditor} from './features/Dialogs';
import {Mentor} from './features/Mentor';
import {Auth} from './features/Auth';

export const navigation=[['Home',Home],['Explore',Compass],['Events',CalendarDays],['Community',Users]];
const previewCompanyTasks={'preview-company':[{id:'company-task-1',title:'Research and planning',owner:'Ananya',state:'To do'},{id:'company-task-2',title:'Update grower onboarding',owner:'Riya',state:'In progress'},{id:'company-task-3',title:'Prepare pitch deck',owner:'Aman',state:'Review'}]};
const previewCompanyDocuments={'preview-company':[{id:'company-document-1',objectId:'preview-object-1',title:'GreenCart pitch deck',filename:'greencart-pitch-deck.pdf',contentType:'application/pdf',bytes:2400000,collection:'fundraising',accessScope:'team',createdAt:'2026-09-18T10:00:00Z',remote:false},{id:'company-document-2',objectId:'preview-object-2',title:'Product launch brief',filename:'product-launch-brief.docx',contentType:'application/vnd.openxmlformats-officedocument.wordprocessingml.document',bytes:860000,collection:'product',accessScope:'team',createdAt:'2026-09-20T10:00:00Z',remote:false}]};
import {resolvePage,VALID_PAGES,parseRoute,formatRoute} from './router.js';
export {VALID_PAGES};
function currentPage(){return resolvePage(location.hash);}
export default function App(){
 const [page,setPage]=useState(currentPage),[theme,setTheme]=useStoredState('theme','light'),[profile,setProfile,storageError]=useStoredState('profile',initialProfile),[auth,setAuth]=useSessionStoredState('authSession',null),[posts,setPosts]=useStoredState('posts',initialPosts),[saved,setSaved]=useStoredState('saved',['p-me-1','p-me-2','p-me-3','p-me-4','p-me-5','p1','p2','e1','j1']),[likes,setLikes]=useStoredState('likes',[]),[following,setFollowing]=useStoredState('following',['riya','aman']),[followers,setFollowers]=useStoredState('followers',['riya','neha','arjun']),[comments,setComments]=useStoredState('comments',initialComments),[votes,setVotes]=useStoredState('votes',{}),[learning,setLearning]=useStoredState('learning',{}),[courseCatalog,setCourseCatalog]=useStoredState('courseCatalog',[]),[applications,setApplications]=useStoredState('applications',{}),[careerJobs,setCareerJobs]=useStoredState('careerJobs',[]),[projectState,setProjectState]=useStoredState('projects',{}),[projectDrafts,setProjectDrafts]=useStoredState('projectDrafts',[]),[companyDrafts,setCompanyDrafts]=useStoredState('companyDrafts',[]),[companyInvitations,setCompanyInvitations]=useStoredState('companyInvitations',{}),[memberships,setMemberships]=useStoredState('memberships',{}),[registrations,setRegistrations]=useStoredState('events',[]),[eventCatalog,setEventCatalog]=useStoredState('eventCatalog',[]),[bookings,setBookings]=useStoredState('bookings',[]),[mentorCatalog,setMentorCatalog]=useStoredState('mentorCatalog',[]),[serviceListings,setServiceListings]=useStoredState('serviceListings',[]),[pipeline,setPipeline]=useStoredState('pipeline',{}),[threads,setThreads]=useStoredState('threads',initialThreads),[notifications,setNotifications]=useStoredState('notifications',seededNotifications),[preferences,setPreferences]=useStoredState('preferences',{community:true,career:true,messages:true}),[modal,setModal]=useState(null),[toast,setToast]=useState(''),[search,setSearch]=useState(''),[mobileMenu,setMobileMenu]=useState(false),[offline,setOffline]=useState(!navigator.onLine),[serviceState,setServiceState]=useState(api.configured?'checking':'preview'),[feedState,setFeedState]=useState({loading:false,nextCursor:null,error:''});
 const [companyTasks,setCompanyTasks]=useStoredState('companyTasks',previewCompanyTasks);
 const [companyDocuments,setCompanyDocuments]=useStoredState('companyDocuments',previewCompanyDocuments);
 const [certificates,setCertificates]=useStoredState('certificates',[]);
 const [projectFiles,setProjectFiles]=useStoredState('projectFiles',{});
 const [projectTasks,setProjectTasks]=useStoredState('projectTasks',{});
 const [projectMilestones,setProjectMilestones]=useStoredState('projectMilestones',{});
 const [projectInvitations,setProjectInvitations]=useStoredState('projectInvitations',{});
 const [jobApplications,setJobApplications]=useStoredState('jobApplications',{});
 const [mentorSlots,setMentorSlots]=useStoredState('mentorSlots',{});
 const [serviceReviews,setServiceReviews]=useStoredState('serviceReviews',{});
 const [eventAttendees,setEventAttendees]=useStoredState('eventAttendees',{});
 const [paymentOrders,setPaymentOrders]=useStoredState('paymentOrders',[]);
 const [payoutRequests,setPayoutRequests]=useStoredState('payoutRequests',[]);
 const [communityCatalog,setCommunityCatalog]=useStoredState('communityCatalog',[]);
 const [deletedCommunityIds,setDeletedCommunityIds]=useStoredState('deletedCommunityIds',[]);
 const [savedEvents,setSavedEvents]=useStoredState('savedEvents',['e1']);
 const [viewedProfile,setViewedProfile]=useState(null);
 const [profileTab,setProfileTab]=useState('Posts');
 const [sidebarOpen,setSidebarOpen]=useStoredState('sidebarOpen',false);
 const [prevPage,setPrevPage]=useState('Home');
 const [searchExpanded,setSearchExpanded]=useState(false);
 const searchInputRef=useRef(null);
 const [activeCall,setActiveCall]=useState(null);
 const [callLog,setCallLog]=useStoredState('callLog',initialCallLog);
 useEffect(()=>{if(!activeCall)return;const timer=setInterval(()=>{setActiveCall(prev=>prev?{...prev,duration:(prev.duration||0)+1}:null)},1000);return()=>clearInterval(timer)},[Boolean(activeCall)]);
 useEffect(()=>{if(searchExpanded){setTimeout(()=>searchInputRef.current?.focus(),50)}},[searchExpanded]);
 function toggleNotifications(){if(page==='Notifications'){go(prevPage||'Home')}else{setPrevPage(page);go('Notifications')}}
 const startCall=(name,type='audio',members=null)=>{if(serviceState==='connected'){setToast('Audio/video calling is disabled in this release.');return;}const matchedThread=threads.find(t=>t.name===name);const callMembers=members||(matchedThread?.isGroup||matchedThread?.context==='Group'?matchedThread.members:null);const person=people.find(p=>p.name===name);setActiveCall({open:true,minimized:false,name,person,type,duration:0,muted:false,videoOff:false,callId:crypto.randomUUID(),members:callMembers,isGroup:Boolean(callMembers&&callMembers.length>0)});};
 const endCall=()=>{if(!activeCall)return;const duration=activeCall.duration||0;const isVideo=activeCall.type==='video';const isGroupCall=Boolean(activeCall.isGroup||(activeCall.members&&activeCall.members.length>0));const record=[activeCall.name,`${isGroupCall?'Group ':''}${isVideo?'Video':'Outgoing audio'} · ${duration>0?formatDuration(duration):'Cancelled'}`,'Just now',activeCall.type||'audio',activeCall.members||null];setCallLog(old=>[record,...(old||[])]);if(serviceState==='connected'&&auth?.accessToken&&activeCall.callId){api.messages.endCall(activeCall.callId,duration,'ended',auth.accessToken).catch(()=>{})}setToast(`Call ended · ${formatDuration(duration)}`);setActiveCall(null)};
 useEffect(()=>{if(sidebarOpen){document.body.classList.add('sidebar-expanded')}else{document.body.classList.remove('sidebar-expanded')}},[sidebarOpen]);
 useEffect(()=>{document.documentElement.dataset.theme=theme;document.querySelector('meta[name="theme-color"]').content=theme==='dark'?'#171a1d':'#e34d42'},[theme]);
 useEffect(()=>{const titles={Home:'Feed',Explore:'Explore Network',Learn:'Learning & Courses',Career:'Careers & Jobs',Mentor:'Mentorship',Projects:'Projects & Workspaces',Company:'Companies',Investment:'Investments','Local services':'Local Services',Events:'Events & Tickets',Community:'Communities & Circles',Messages:'Messages',Notifications:'Notifications',Profile:`${profile.name||'Your'} Profile`,Saved:'Saved Items',Settings:'Account Settings',NotFound:'404 - Page Not Found'};document.title=`${titles[page]||page} · Collobster`;},[page,profile.name]);
 useEffect(()=>{const fn=()=>{const parsed=parseRoute(location.hash);if(parsed.isLegacy){window.history.replaceState(null,'',parsed.canonicalHash);}if(parsed.isNotFound){setPage('NotFound');return;}if(parsed.entityType&&parsed.entityId){if(parsed.entityType==='conversation'){setActiveThreadId(parsed.entityId);setPage('Messages');}else if(parsed.entityType==='community'){const found=communityCatalog.find(c=>c.id===parsed.entityId)||communities.find(c=>c.id===parsed.entityId);if(found){setModal({type:'details',kind:'community',item:found});setPage('Community');}else{setPage('NotFound');}}else if(parsed.entityType==='event'){const found=eventCatalog.find(e=>e.id===parsed.entityId)||events.find(e=>e.id===parsed.entityId);if(found){setModal({type:'details',kind:'event',item:found});setPage('Events');}else{setPage('NotFound');}}else if(parsed.entityType==='course'){const found=courseCatalog.find(c=>c.id===parsed.entityId)||courses.find(c=>c.id===parsed.entityId);if(found){setModal({type:'details',kind:'course',item:found});setPage('Learn');}else{setPage('NotFound');}}else if(parsed.entityType==='profile'){openUserProfile(parsed.entityId);setPage('Profile');}else{setPage(parsed.page);}}else{setPage(parsed.page);}};window.addEventListener('hashchange',fn);fn();return()=>window.removeEventListener('hashchange',fn)},[communityCatalog,eventCatalog,courseCatalog]);
 useEffect(()=>{const fn=()=>setOffline(!navigator.onLine);window.addEventListener('online',fn);window.addEventListener('offline',fn);return()=>{window.removeEventListener('online',fn);window.removeEventListener('offline',fn)}},[]);
 useEffect(()=>{let active=true;if(!api.configured){setServiceState('preview');return undefined}api.health().then(()=>active&&setServiceState('connected')).catch(()=>active&&setServiceState('unavailable'));return()=>{active=false}},[]);
 useEffect(()=>{
  try {
   const urlParams=new URLSearchParams(window.location.search);
   const oauthCode=urlParams.get('oauth_code');
   if(oauthCode){
    window.history.replaceState({},document.title,window.location.pathname);
    api.auth.oauthExchange(oauthCode).then(res => {
     const accessToken = res?.accessToken;
     if(!accessToken) throw new Error('NO_ACCESS_TOKEN');
     return api.identity.me(accessToken).then(remote=>{
      const names=(remote.displayName||'').trim().split(/\s+/);
      completeAuth({
       firstName:names[0]||'Member',
       lastName:names.slice(1).join(' '),
       email:remote.email||'',
       accessToken:accessToken,
       provider:'google',
       profileId:remote.id,
       role:remote.headline||'',
       skills:Array.isArray(remote.skills)?remote.skills.join(', '):'',
       interests:Array.isArray(remote.interests)?remote.interests.join(', '):'',
       location:remote.location||'',
       dob:remote.birthDate||'',
       gender:remote.gender||'',
       username:remote.username||'',
      });
      setToast(`Signed in as ${remote.displayName||'Google user'}`);
     });
    }).catch((err)=>{
     console.error('OAuth exchange/validation failed:', err);
     setToast('Google sign-in could not be verified. Please try again.');
     // SEC-003: Never store unvalidated token or fake session
    });
   }
  }catch(e){}
 },[]);
 useEffect(()=>{if(serviceState!=='connected'||!auth?.accessToken)return undefined;let active=true;api.identity.me(auth.accessToken).then(remote=>{if(active)setProfile(old=>mergeRemoteProfile(old,remote))}).catch(()=>{if(active)setToast('Could not load your cloud profile. Using the last saved version.')});return()=>{active=false}},[auth?.accessToken,serviceState,setProfile]);
  useEffect(()=>{if(serviceState!=='connected'||!auth?.accessToken)return undefined;let active=true;setFeedState({loading:true,nextCursor:null,error:''});api.social.feed(null,20,auth.accessToken).then(result=>{if(!active)return;setPosts(result.items.map(feedItemToPost));setLikes(result.items.filter(item=>item.viewerReacted).map(item=>item.id));const voted={};result.items.forEach(it=>{if(it.poll?.viewerVotedOptionId){const optIdx=it.poll.options?.findIndex(o=>o.id===it.poll.viewerVotedOptionId);if(optIdx>=0)voted[it.id]=optIdx;}});if(Object.keys(voted).length)setVotes(old=>({...old,...voted}));setFeedState({loading:false,nextCursor:result.nextCursor,error:''})}).catch(()=>{if(active)setFeedState({loading:false,nextCursor:null,error:'Could not load the live feed. Showing your last saved view.'})});return()=>{active=false}},[auth?.accessToken,serviceState,setPosts,setLikes]);
  useEffect(()=>{if(serviceState!=='connected'||!auth?.accessToken)return undefined;let active=true;api.social.savedIds(auth.accessToken).then(ids=>{if(active&&ids&&ids.length)setSaved(old=>[...new Set([...old,...ids.map(String)])]);}).catch(()=>{});return()=>{active=false}},[auth?.accessToken,serviceState,setSaved]);
  useEffect(()=>{if(serviceState!=='connected'||!auth?.accessToken)return undefined;let active=true;api.communities.list(auth.accessToken).then(rows=>{if(!active)return;if(rows&&rows.length){const mapped=rows.map(r=>({id:r.id,title:r.name,name:r.name,slug:r.slug,category:r.category,description:r.description,members:12,tags:[r.category],policy:r.accessPolicy==='open'?'Open':'Requires permission',image:'https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=600&auto=format&fit=crop&q=80',remote:true}));setCommunityCatalog(mapped);const mems={};rows.forEach(r=>{if(r.membershipState)mems[r.id]=r.membershipState;});setMemberships(old=>({...old,...mems}));}}).catch(()=>{});return()=>{active=false}},[auth?.accessToken,serviceState,setCommunityCatalog,setMemberships]);
 useEffect(()=>{if(serviceState!=='connected'||!auth?.accessToken)return undefined;let active=true;api.notifications.page(null,20,auth.accessToken).then(result=>{if(active)setNotifications(result.items.map(notificationForUi))}).catch(()=>{if(active)setToast('Could not load cloud notifications. Using the last saved view.')});return()=>{active=false}},[auth?.accessToken,serviceState,setNotifications]);
 useEffect(()=>{if(serviceState!=='connected'||!auth?.accessToken)return undefined;let active=true;api.notifications.preferences(auth.accessToken).then(remote=>{if(active)setPreferences(remote)}).catch(()=>{if(active)setToast('Could not load notification preferences. Using this device setting.')});return()=>{active=false}},[auth?.accessToken,serviceState,setPreferences]);
 useEffect(()=>{if(serviceState!=='connected'||!auth?.accessToken)return undefined;let active=true;api.projects.list(auth.accessToken).then(async rows=>{if(!active)return;const remoteProjects=rows.map(row=>({...row.project,description:row.project.description||'A connected code space.',category:'Code space',tags:[],roles:row.role||'Collaborator',members:1,progress:row.project.state==='active'?12:0,color:'blue',tasks:[],membershipState:row.membershipState,remote:true}));setProjectDrafts(old=>[...remoteProjects,...old.filter(local=>!remoteProjects.some(remote=>remote.id===local.id))]);setProjectState(old=>({...old,...Object.fromEntries(rows.filter(row=>row.membershipState==='active').map(row=>[row.project.id,'joined']))}));const activeRows=rows.filter(row=>row.membershipState==='active');const [filesEntries,tasksEntries,milestonesEntries]=await Promise.all([Promise.all(activeRows.map(async r=>{try{const files=await api.projects.files(r.project.id,auth.accessToken);return [r.project.id,files||[]];}catch(e){return [r.project.id,[]];}})),Promise.all(activeRows.map(async r=>{try{const tasks=await api.projects.tasks(r.project.id,auth.accessToken);return [r.project.id,(tasks||[]).map(t=>({...t,remote:true}))];}catch(e){return [r.project.id,[]];}})),Promise.all(activeRows.map(async r=>{try{const ms=await api.projects.milestones(r.project.id,auth.accessToken);return [r.project.id,(ms||[]).map(m=>({...m,remote:true}))];}catch(e){return [r.project.id,[]];}}))]);if(!active)return;setProjectFiles(old=>({...old,...Object.fromEntries(filesEntries)}));setProjectTasks(old=>({...old,...Object.fromEntries(tasksEntries)}));setProjectMilestones(old=>({...old,...Object.fromEntries(milestonesEntries)}));}).catch(()=>{if(active)setToast('Could not load cloud projects. Using the projects saved on this device.')});return()=>{active=false}},[auth?.accessToken,serviceState,setProjectDrafts,setProjectState,setProjectFiles,setProjectTasks,setProjectMilestones]);
 useEffect(()=>{if(serviceState!=='connected'||!auth?.accessToken)return undefined;let active=true;api.companies.list(auth.accessToken).then(rows=>{if(!active)return;const remoteCompanies=rows.map(row=>({...row.company,state:row.company.state,membershipState:row.membershipState,role:row.role,tagline:row.company.description,createdAt:new Date().toISOString(),remote:true}));setCompanyDrafts(old=>[...remoteCompanies,...old.filter(local=>!remoteCompanies.some(remote=>remote.id===local.id))]);}).catch(()=>{if(active)setToast('Could not load cloud companies. Using the companies saved on this device.')});return()=>{active=false}},[auth?.accessToken,serviceState,setCompanyDrafts]);
 useEffect(()=>{if(serviceState!=='connected'||!auth?.accessToken)return undefined;let active=true;api.companies.list(auth.accessToken).then(rows=>Promise.all(rows.filter(row=>row.membershipState==='active').map(async row=>[row.company.id,await api.companies.tasks(row.company.id,auth.accessToken)]))).then(entries=>{if(!active)return;setCompanyTasks(old=>({...old,...Object.fromEntries(entries.map(([companyId,tasks])=>[companyId,tasks.map(task=>({id:task.id,title:task.title,owner:task.assigneeId?'Assigned teammate':'Unassigned',state:{todo:'To do',in_progress:'In progress',review:'Review',done:'Done'}[task.state]||'To do',remote:true}))]))}))}).catch(()=>{if(active)setToast('Could not load company tasks. Using the tasks saved on this device.')});return()=>{active=false}},[auth?.accessToken,serviceState,setCompanyTasks]);
 useEffect(()=>{if(serviceState!=='connected'||!auth?.accessToken)return undefined;let active=true;api.companies.list(auth.accessToken).then(rows=>Promise.all(rows.filter(row=>row.membershipState==='active').map(async row=>[row.company.id,await api.companies.documents(row.company.id,auth.accessToken)]))).then(entries=>{if(!active)return;setCompanyDocuments(old=>({...old,...Object.fromEntries(entries.map(([companyId,documents])=>[companyId,documents.map(companyDocumentForUi)]))}))}).catch(()=>{if(active)setToast('Could not load company documents. Using files saved on this device.')});return()=>{active=false}},[auth?.accessToken,serviceState,setCompanyDocuments]);
 useEffect(()=>{if(serviceState!=='connected'||!auth?.accessToken)return undefined;let active=true;api.events.tickets(auth.accessToken).then(rows=>{if(active)setRegistrations(rows.filter(row=>row.registrationState==='registered').map(row=>row.event.id));}).catch(()=>{if(active)setToast('Could not load cloud event tickets. Using tickets saved on this device.')});return()=>{active=false}},[auth?.accessToken,serviceState,setRegistrations]);
 useEffect(()=>{if(serviceState!=='connected'||!auth?.accessToken)return undefined;let active=true;api.events.list(auth.accessToken).then(rows=>{if(!active)return;setEventCatalog(rows.map(row=>{const date=new Date(row.startsAt);return {id:row.id,title:row.title,description:'A Collobster event for meeting people and making progress together.',category:'Event',day:String(date.getDate()).padStart(2,'0'),month:date.toLocaleDateString('en-US',{month:'short'}).toUpperCase(),date:date.toISOString().slice(0,10),time:date.toLocaleTimeString('en-US',{hour:'numeric',minute:'2-digit'}),location:row.location||'Online',mode:row.mode==='online'?'Online':row.mode==='hybrid'?'Hybrid':'In person',attendees:row.registered||0,image:events[0].image,remote:true,state:row.state};}));}).catch(()=>{if(active)setToast('Could not load cloud events. Showing the available Preview calendar.')});return()=>{active=false}},[auth?.accessToken,serviceState,setEventCatalog]);
 useEffect(()=>{if(serviceState!=='connected'||!auth?.accessToken)return undefined;let active=true;api.services.list(auth.accessToken).then(rows=>{if(!active)return;setServiceListings(rows.map(row=>({id:row.id,title:row.title,provider:'Verified provider',category:row.category,description:row.description,price:Number(row.startingPrice||0),duration:row.pricingUnit||'Service visit',location:row.serviceArea,color:'green',remote:true,state:row.state,serviceRadiusKm:row.serviceRadiusKm||15,averageRating:row.averageRating,reviewCount:row.reviewCount||0})));}).catch(()=>{if(active)setToast('Could not load cloud services. Showing the available Preview catalog.')});return()=>{active=false}},[auth?.accessToken,serviceState,setServiceListings]);
 useEffect(()=>{if(serviceState!=='connected'||!auth?.accessToken)return undefined;let active=true;Promise.all([api.career.jobs(auth.accessToken),api.career.applications(auth.accessToken)]).then(([jobs,applications])=>{if(!active)return;setCareerJobs(jobs.map(row=>({id:row.id,title:row.title,company:row.companyName,location:row.location||'Flexible',mode:row.workMode||'Flexible',type:row.employmentType||'Full-time',salary:'Compensation discussed',tags:[],color:'blue',description:row.description||'A career opportunity on Collobster.',remote:true,state:row.state})));setApplications(old=>({...old,...Object.fromEntries(applications.map(row=>[row.jobId,row.state]))}));}).catch(()=>{if(active)setToast('Could not load cloud career data. Showing the saved career view.')});return()=>{active=false}},[auth?.accessToken,serviceState,setCareerJobs,setApplications]);
 useEffect(()=>{if(serviceState!=='connected'||!auth?.accessToken)return undefined;let active=true;api.learning.courses(auth.accessToken).then(rows=>{if(!active)return;setCourseCatalog(rows.map(row=>({id:row.id,title:row.title,category:'Learning',description:row.summary,mentor:'Collobster instructor',duration:'Self-paced',level:row.difficulty||'beginner',color:'blue',symbol:'Co',tags:[],lessons:[],lessonIds:[],remote:true,enrollmentState:row.enrollmentState,progressPercent:row.progress||0})));}).catch(()=>{if(active)setToast('Could not load cloud courses. Showing the available Preview catalog.')});return()=>{active=false}},[auth?.accessToken,serviceState,setCourseCatalog]);
 useEffect(()=>{if(serviceState!=='connected'||!auth?.accessToken)return undefined;let active=true;api.learning.myCertificates(auth.accessToken).then(rows=>{if(active)setCertificates(rows)}).catch(()=>{});return()=>{active=false}},[auth?.accessToken,serviceState,setCertificates]);
 useEffect(()=>{if(serviceState!=='connected'||!auth?.accessToken)return undefined;let active=true;Promise.all([api.mentors.list(auth.accessToken),api.mentors.sessions(auth.accessToken)]).then(([profiles,sessions])=>{if(!active)return;setMentorCatalog(profiles.map(row=>({id:row.profileId,name:row.headline||'Collobster mentor',role:row.headline||'Mentor',person:{id:row.profileId,name:row.headline||'Collobster mentor',initials:'CM',color:'blue'},rating:'New',reviews:'0',rate:Number(row.hourlyRate||0),experience:'Verified profile',learners:'New',tags:['Mentoring'],about:row.bio||'A verified mentor on Collobster.',remote:true,state:row.state})));const remoteSessions=sessions.map(row=>({id:row.id,title:`Session · ${row.sessionType}`,date:new Date(row.startsAt).toLocaleString([], {dateStyle:'medium',timeStyle:'short'}),status:row.state,remote:true,mentorId:row.mentorId}));setBookings(old=>[...remoteSessions,...old.filter(item=>!item.remote||!remoteSessions.some(remote=>remote.id===item.id))]);}).catch(()=>{if(active)setToast('Could not load cloud mentor data. Showing the available Preview mentors.')});return()=>{active=false}},[auth?.accessToken,serviceState,setMentorCatalog,setBookings]);
 useEffect(()=>{if(serviceState!=='connected'||!auth?.accessToken)return undefined;let active=true;api.messages.conversations(auth.accessToken).then(async conversations=>{const hydrated=await Promise.all(conversations.map(async conversation=>{const history=await api.messages.history(conversation.id,null,auth.accessToken);return conversationForUi(conversation,history.items,profile.profileId,profile.name)}));if(active&&hydrated.length)setThreads(hydrated)}).catch(()=>{if(active)setToast('Could not load cloud conversations. Using the last saved inbox.')});return()=>{active=false}},[auth?.accessToken,serviceState,profile.profileId,profile.name,setThreads]);
 useEffect(()=>{if(serviceState!=='connected'||!auth?.accessToken)return undefined;return connectRealtime(auth.accessToken,{onEvent:event=>{if(event.type==='notification.created'&&event.payload?.notification){const notification=notificationForUi(event.payload.notification);setNotifications(old=>old.some(item=>item.id===notification.id)?old:[notification,...old]);}if(event.type==='message.created'&&event.payload?.message){const message=messageForUi(event.payload.message,profile.profileId);setThreads(old=>old.map(thread=>thread.id===event.payload.message.conversationId&&!thread.messages.some(item=>item.id===message.id)?{...thread,typingName:null,unread:!message.mine,messages:[...thread.messages,message]}:thread));}if(event.type==='message.read'){setThreads(old=>old.map(thread=>thread.id===event.payload?.receipt?.conversationId?{...thread,lastReadAt:event.payload.receipt.readAt}:thread));}if(event.type==='message.typing'&&event.payload?.conversationId){setThreads(old=>old.map(thread=>thread.id===event.payload.conversationId?{...thread,typingName:event.payload.typing?(event.payload.displayName||'Someone'):null}:thread));}},onState:state=>{if(state==='disconnected')setToast('Realtime connection paused. Changes will sync when it reconnects.')}})},[auth?.accessToken,serviceState,profile.profileId,setNotifications,setThreads]);
 useEffect(()=>{if(serviceState!=='connected'||!auth?.accessToken)return undefined;let active=true;api.payments.orders(auth.accessToken).then(rows=>{if(active)setPaymentOrders(old=>[...rows,...old.filter(local=>!rows.some(remote=>remote.id===local.id))])}).catch(()=>{});return()=>{active=false}},[auth?.accessToken,serviceState,setPaymentOrders]);
 useEffect(()=>{if(serviceState!=='connected'||!auth?.accessToken)return undefined;let active=true;api.payments.payouts(auth.accessToken).then(rows=>{if(active)setPayoutRequests(old=>[...rows,...old.filter(local=>!rows.some(remote=>remote.id===local.id))])}).catch(()=>{});return()=>{active=false}},[auth?.accessToken,serviceState,setPayoutRequests]);
 useEffect(()=>{const activate=event=>{const row=event.target.closest?.('.message-attachment');if(!row)return;const filename=row.querySelector('span')?.textContent;const match=threads.flatMap(thread=>thread.messages.map(message=>({thread,attachment:(message.attachments||[]).find(item=>item.filename===filename)}))).find(item=>item.attachment);if(match)downloadMessageAttachment(match.thread.id,match.attachment);};const keydown=event=>{if((event.key==='Enter'||event.key===' ')&&event.target.closest?.('.message-attachment')){event.preventDefault();activate(event);}};document.addEventListener('click',activate);document.addEventListener('keydown',keydown);document.querySelectorAll('.message-attachment').forEach(row=>{row.setAttribute('role','button');row.setAttribute('tabindex','0');row.setAttribute('aria-label',`Download ${row.querySelector('span')?.textContent||'attachment'}`);});return()=>{document.removeEventListener('click',activate);document.removeEventListener('keydown',keydown);};},[threads,serviceState,auth?.accessToken]);
 useEffect(()=>{if(!toast)return;const timer=setTimeout(()=>setToast(''),3500);return()=>clearTimeout(timer)},[toast]);
 useEffect(()=>{document.title=`${page} · Collobster`;window.scrollTo(0,0)},[page]);
 function completeAuth(details){if(api.configured&&!details.accessToken){setToast('Sign-in failed: an active verified session is required.');return;}const name=`${details.firstName||'Ananya'} ${details.lastName||'Sharma'}`.trim();setProfile(old=>({...old,profileId:details.profileId||old.profileId,name,location:details.location||old.location,headline:details.role||old.headline,skills:details.skills?details.skills.split(',').map(x=>x.trim()).filter(Boolean):old.skills,interests:details.interests?details.interests.split(',').map(x=>x.trim()).filter(Boolean):old.interests,username:details.username||old.username,dob:details.dob||old.dob,gender:details.gender||old.gender,onboarded:true}));setAuth({email:details.email||'',provider:details.provider||'email',accessToken:details.accessToken||null,refreshToken:details.refreshToken||null,signedInAt:Date.now()});location.hash='Home';setPage('Home')}
 async function saveProfile(next){if(serviceState==='connected'&&auth?.accessToken){const remote=await api.identity.update(profilePayload(next),auth.accessToken);setProfile(old=>({...next,...mergeRemoteProfile(next,remote)}));return 'synced'}setProfile(next);return 'preview'}
 async function updateProfilePrivacy(checked){const next={...profile,privacy:{...profile.privacy,profileVisibility:checked?'private':'public'}};try{const mode=await saveProfile(next);setToast(mode==='synced'?`Profile is now ${checked?'private':'public'} across your account.`:`Profile is now ${checked?'private':'public'} in Preview.`)}catch{setToast('Could not save profile visibility. Please try again.')}}
 async function createCompany(draft){
  const payload={name:draft.name.trim(),description:(draft.description||draft.tagline||'A new company workspace.').trim(),industry:draft.industry.trim(),stage:draft.stage.trim()};
  if(auth?.accessToken){
    if(serviceState!=='connected') throw new Error('An active connection to Collobster is required to create a company.');
    const remote=await api.companies.create(payload,auth.accessToken);
    const company={...payload,id:remote.id,state:remote.state||'verification_pending',tagline:draft.tagline,website:draft.website,headquarters:draft.headquarters,country:draft.country,teamSize:draft.teamSize,model:draft.model,path:draft.path||'Startup / dev environment',createdAt:remote.createdAt||new Date().toISOString(),role:'owner',membershipState:'active',remote:true};
    setCompanyDrafts(old=>[company,...old.filter(item=>item.id!==company.id)]);
    return {...company,mode:'synced'};
  }
  const local={...payload,id:`company-${crypto.randomUUID()}`,state:'verification_pending',tagline:draft.tagline,website:draft.website,headquarters:draft.headquarters,country:draft.country,teamSize:draft.teamSize,model:draft.model,path:draft.path||'Startup / dev environment',createdAt:new Date().toISOString(),role:'owner',membershipState:'active',remote:false};
  setCompanyDrafts(old=>[local,...old.filter(item=>item.id!==local.id)]);
  return {...local,mode:'preview'};
 }
 async function searchProfiles(query){const normalized=query.trim();if(normalized.length<2)return[];if(serviceState==='connected'&&auth?.accessToken)return api.identity.searchProfiles(normalized,10,auth.accessToken);const needle=normalized.toLowerCase();return people.filter(person=>`${person.name} ${person.role}`.toLowerCase().includes(needle)).map(person=>({id:person.id,displayName:person.name,headline:person.role,location:'Preview network'}));}
 async function inviteCompanyMember(company,person,role){
  if(auth?.accessToken){
    if(serviceState!=='connected'||!company.remote) throw new Error('Cannot invite members to an offline or unverified company workspace.');
    await api.companies.invite(company.id,{profileId:person.id,role},auth.accessToken);
    const invitation={id:person.id,name:person.displayName,headline:person.headline,role,state:'pending'};
    setCompanyInvitations(old=>({...old,[company.id]:[invitation,...(old[company.id]||[]).filter(item=>item.id!==person.id)]}));
    return 'synced';
  }
  const invitation={id:person.id,name:person.displayName,headline:person.headline,role,state:'pending'};
  setCompanyInvitations(old=>({...old,[company.id]:[invitation,...(old[company.id]||[]).filter(item=>item.id!==person.id)]}));
  return 'preview';
 }
 async function acceptCompanyInvitation(companyId){
  if(auth?.accessToken){
    if(serviceState!=='connected') throw new Error('An active connection is required to accept company invitations.');
    await api.companies.acceptInvite(companyId,auth.accessToken);
    setCompanyDrafts(old=>old.map(c=>c.id===companyId?{...c,membershipState:'active'}:c));
    setToast('Joined company successfully!');
    return 'synced';
  }
  setCompanyDrafts(old=>old.map(c=>c.id===companyId?{...c,membershipState:'active'}:c));
  setToast('Joined company in Preview.');
  return 'preview';
 }
 async function createCompanyTask(company,title){
  const clean=title.trim();
  if(!clean)throw new Error('Give the task a clear title.');
  if(auth?.accessToken){
    if(serviceState!=='connected'||!company.remote) throw new Error('An active connection is required to create company tasks.');
    const remote=await api.companies.createTask(company.id,{title:clean,description:'',assigneeId:null},auth.accessToken);
    const task={id:remote.id,title:clean,owner:profile.name,state:'To do',remote:true};
    setCompanyTasks(old=>({...old,[company.id]:[task,...(old[company.id]||[])]}));
    return {...task,mode:'synced'};
  }
  const local={id:`company-task-${crypto.randomUUID()}`,title:clean,owner:profile.name,state:'To do',remote:false};
  setCompanyTasks(old=>({...old,[company.id]:[local,...(old[company.id]||[])]}));
  return {...local,mode:'preview'};
 }
 async function updateCompanyTask(company,task,state){
  const apiState={'To do':'todo','In progress':'in_progress','Review':'review','Done':'done'}[state];
  if(auth?.accessToken){
    if(serviceState!=='connected'||!company.remote) throw new Error('An active connection is required to update company tasks.');
    const remote=await api.companies.updateTask(company.id,task.id,{state:apiState},auth.accessToken);
    setCompanyTasks(old=>({...old,[company.id]:(old[company.id]||[]).map(item=>item.id===task.id?{...item,state,remote:true}:item)}));
    return {...remote,mode:'synced'};
  }
  setCompanyTasks(old=>({...old,[company.id]:(old[company.id]||[]).map(item=>item.id===task.id?{...item,state}:item)}));
  return 'preview';
 }
 async function addCompanyDocument(company,file,draft){const upload=await uploadFile(file);let remote=null;if(upload.mode==='synced'&&company.remote&&serviceState==='connected'&&auth?.accessToken)remote=await api.companies.createDocument(company.id,{objectId:upload.attachment.objectId,title:draft.title.trim()||file.name,collection:draft.collection,accessScope:draft.accessScope},auth.accessToken);const document=remote?companyDocumentForUi(remote):{id:`company-document-${crypto.randomUUID()}`,objectId:upload.attachment.objectId,title:draft.title.trim()||file.name,filename:file.name,contentType:file.type||'application/octet-stream',bytes:file.size,collection:draft.collection,accessScope:draft.accessScope,createdAt:new Date().toISOString(),remote:false};setCompanyDocuments(old=>({...old,[company.id]:[document,...(old[company.id]||[])]}));return remote?'synced':'preview';}
 async function downloadCompanyDocument(company,document){if(document.remote&&company.remote&&serviceState==='connected'&&auth?.accessToken){try{await api.companies.downloadFile(company.id,document.id,auth.accessToken,document.filename);return 'synced';}catch(err){if(err.status===403){setToast('Access denied: Admin privileges required to download this document.');}else if(err.status===401){setToast('Session expired. Please sign in again.');}else{setToast(err.detail||err.message||'Could not download document.');}throw err;}}setToast(`${document.filename} is available in this Preview workspace.`);return 'preview';}
 async function archiveCompanyDocument(company,document){
  if(auth?.accessToken){
    if(serviceState!=='connected'||!company.remote) throw new Error('An active connection is required to archive company documents.');
    await api.companies.archiveDocument(company.id,document.id,auth.accessToken);
    setCompanyDocuments(old=>({...old,[company.id]:(old[company.id]||[]).filter(item=>item.id!==document.id)}));
    return 'synced';
  }
  setCompanyDocuments(old=>({...old,[company.id]:(old[company.id]||[]).filter(item=>item.id!==document.id)}));
  return 'preview';
 }
 async function createProject(draft){
  const payload={title:draft.title.trim()||'Untitled build',description:(draft.description||draft.problem||'A new code space ready for its first commit.').trim(),visibility:{'Private':'private','Share with collaborators':'team','Public':'public'}[draft.visibility]||'private'};
  if(auth?.accessToken){
    if(serviceState!=='connected') throw new Error('An active connection to Collobster is required to create a project.');
    const remote=await api.projects.create(payload,auth.accessToken);
    const project={
      ...draft,
      ...payload,
      id:remote.id,
      state:remote.state||'active',
      category:'Code space',
      tags:draft.tags||[],
      roles:'owner',
      members:1,
      progress:8,
      color:'blue',
      tasks:[],
      membershipState:'active',
      createdAt:remote.createdAt||new Date().toISOString(),
      remote:true
    };
    setProjectDrafts(old=>[project,...old.filter(item=>item.id!==project.id)]);
    setProjectState(old=>({...old,[project.id]:'joined'}));
    return {...project,mode:'synced'};
  }
  const local={
    ...draft,
    ...payload,
    id:`project-${crypto.randomUUID()}`,
    state:'active',
    category:'Code space',
    tags:draft.tags||[],
    roles:'Builder',
    members:1,
    progress:8,
    color:'blue',
    tasks:[],
    membershipState:'active',
    createdAt:new Date().toISOString(),
    remote:false
  };
  setProjectDrafts(old=>[local,...old.filter(item=>item.id!==local.id)]);
  setProjectState(old=>({...old,[local.id]:'joined'}));
  return {...local,mode:'preview'};
 }
 async function saveProjectFile(projectId,draft){
  if(auth?.accessToken){
    if(serviceState!=='connected') throw new Error('An active connection is required to save project files.');
    const remote=await api.projects.saveFile(projectId,draft,auth.accessToken);
    setProjectFiles(old=>({...old,[projectId]:[...(old[projectId]||[]).filter(f=>f.path!==draft.path),{...remote,remote:true}]}));
    return {file:remote,mode:'synced'};
  }
  const local={id:crypto.randomUUID(),projectId,path:draft.path,content:draft.content,fileType:draft.fileType||'text',updatedAt:new Date().toISOString(),remote:false};
  setProjectFiles(old=>({...old,[projectId]:[...(old[projectId]||[]).filter(f=>f.path!==draft.path),local]}));
  return {file:local,mode:'preview'};
 }
 async function deleteProjectFile(projectId,fileId){
  if(auth?.accessToken){
    if(serviceState!=='connected') throw new Error('An active connection is required to delete project files.');
    await api.projects.deleteFile(projectId,fileId,auth.accessToken);
  }
  setProjectFiles(old=>({...old,[projectId]:(old[projectId]||[]).filter(f=>f.id!==fileId)}));
 }
 async function createProjectTask(projectId,taskDraft){
  if(auth?.accessToken){
    if(serviceState!=='connected') throw new Error('An active connection is required to create project tasks.');
    const remote=await api.projects.createTask(projectId,taskDraft,auth.accessToken);
    setProjectTasks(old=>({...old,[projectId]:[{...remote,remote:true},...(old[projectId]||[])]}));
    return {task:remote,mode:'synced'};
  }
  const local={id:crypto.randomUUID(),projectId,title:taskDraft.title,description:taskDraft.description,status:'todo',priority:taskDraft.priority||'medium',createdAt:new Date().toISOString(),remote:false};
  setProjectTasks(old=>({...old,[projectId]:[local,...(old[projectId]||[])]}));
  return {task:local,mode:'preview'};
 }
 async function updateProjectTaskStatus(projectId,taskId,status){
  if(auth?.accessToken){
    if(serviceState!=='connected') throw new Error('An active connection is required to update project tasks.');
    await api.projects.updateTaskStatus(projectId,taskId,status,auth.accessToken);
  }
  setProjectTasks(old=>({...old,[projectId]:(old[projectId]||[]).map(t=>t.id===taskId?{...t,status}:t)}));
 }
 async function createProjectMilestone(projectId,milestoneDraft){
  if(auth?.accessToken){
    if(serviceState!=='connected') throw new Error('An active connection is required to create project milestones.');
    const remote=await api.projects.createMilestone(projectId,milestoneDraft,auth.accessToken);
    setProjectMilestones(old=>({...old,[projectId]:[...(old[projectId]||[]),{...remote,remote:true}]}));
    return {milestone:remote,mode:'synced'};
  }
  const local={id:crypto.randomUUID(),projectId,title:milestoneDraft.title,targetDate:milestoneDraft.targetDate,status:'open',createdAt:new Date().toISOString(),remote:false};
  setProjectMilestones(old=>({...old,[projectId]:[...(old[projectId]||[]),local]}));
  return {milestone:local,mode:'preview'};
 }
 async function inviteProjectMember(project,person,role){
  if(auth?.accessToken){
    if(serviceState!=='connected'||!project.remote) throw new Error('Cannot invite members to an offline or unverified project.');
    await api.projects.invite(project.id,{profileId:person.id,role},auth.accessToken);
    const invitation={id:person.id,name:person.displayName,headline:person.headline,role,state:'pending'};
    setProjectInvitations(old=>({...old,[project.id]:[invitation,...(old[project.id]||[]).filter(item=>item.id!==person.id)]}));
    return 'synced';
  }
  const invitation={id:person.id,name:person.displayName,headline:person.headline,role,state:'pending'};
  setProjectInvitations(old=>({...old,[project.id]:[invitation,...(old[project.id]||[]).filter(item=>item.id!==person.id)]}));
  return 'preview';
 }
 async function acceptProjectInvitation(projectId){
  if(auth?.accessToken){
    if(serviceState!=='connected') throw new Error('An active connection is required to accept project invitations.');
    await api.projects.acceptInvite(projectId,auth.accessToken);
    setProjectDrafts(old=>old.map(p=>p.id===projectId?{...p,membershipState:'active'}:p));
    setProjectState(old=>({...old,[projectId]:'joined'}));
    setToast('Joined project successfully!');
    return 'synced';
  }
  setProjectDrafts(old=>old.map(p=>p.id===projectId?{...p,membershipState:'active'}:p));
  setProjectState(old=>({...old,[projectId]:'joined'}));
  setToast('Joined project in Preview.');
  return 'preview';
 }
 async function registerForEvent(event){let remote=null;if(serviceState==='connected'&&auth?.accessToken){try{remote=await api.events.register(event.id,auth.accessToken);}catch(err){console.warn('Backend event registration fallback to local pass:',err);}}setRegistrations(old=>old.includes(event.id)?old:[...old,event.id]);return {event:remote,mode:remote?'synced':'preview'};}
 async function joinCommunity(community){let remote=null;if(serviceState==='connected'&&auth?.accessToken)remote=await api.communities.join(community.id,auth.accessToken);const state=remote?.membershipState||'member';setMemberships(old=>({...old,[community.id]:state}));return {state,mode:remote?'synced':'preview'};}
 async function leaveCommunity(community){let remote=false;if(serviceState==='connected'&&auth?.accessToken){try{await api.communities.leave(community.id,auth.accessToken);remote=true;}catch(e){}}setMemberships(old=>{const next={...old};delete next[community.id];return next;});return {mode:remote?'synced':'preview'};}
 async function publishCommunityPost(community,body){if(serviceState==='connected'&&auth?.accessToken){const remote=await api.communities.createPost(community.id,{body,postType:'discussion'},auth.accessToken);return {body:remote.body,mode:'synced'};}return {body,mode:'preview'};}
 async function addCommunityComment(communityId,postId,body){let remote=null;if(serviceState==='connected'&&auth?.accessToken){try{remote=await api.communities.createComment(communityId,postId,body,auth.accessToken);}catch(e){}}return {comment:remote,body,mode:remote?'synced':'preview'};}
 async function bookService(listing,draft){let remote=null;if(auth?.accessToken&&listing.remote){if(serviceState!=='connected')throw new Error('Booking this service requires an active connection to the backend.');remote=await api.services.book(listing.id,draft,auth.accessToken);}const booking={id:remote?.id||`booking-${crypto.randomUUID()}`,title:listing.title,status:remote?.state||'Confirmed',startsAt:draft.startsAt,address:draft.serviceAddress};setBookings(old=>[booking,...old.filter(item=>item.id!==booking.id)]);return {booking,mode:remote?'synced':'preview'};}
 async function fetchServiceReviews(serviceId){if(serviceState==='connected'&&auth?.accessToken){const list=await api.services.reviews(serviceId,auth.accessToken);setServiceReviews(old=>({...old,[serviceId]:list}));return list;}return serviceReviews[serviceId]||[];}
 async function createServiceReview(serviceId,draft){if(serviceState==='connected'&&auth?.accessToken){const remote=await api.services.createReview(serviceId,draft,auth.accessToken);setServiceReviews(old=>({...old,[serviceId]:[remote,...(old[serviceId]||[])]}));setServiceListings(old=>old.map(s=>{if(s.id===serviceId){const nextCount=(s.reviewCount||0)+1;const nextAvg=((s.averageRating||0)*(s.reviewCount||0)+draft.rating)/nextCount;return {...s,reviewCount:nextCount,averageRating:parseFloat(nextAvg.toFixed(1))};}return s;}));return {review:remote,mode:'synced'};}const local={id:crypto.randomUUID(),serviceId,authorId:profile.profileId||'me',authorName:profile.name,rating:draft.rating,reviewText:draft.reviewText,createdAt:new Date().toISOString()};setServiceReviews(old=>({...old,[serviceId]:[local,...(old[serviceId]||[])]}));setServiceListings(old=>old.map(s=>{if(s.id===serviceId){const nextCount=(s.reviewCount||0)+1;const nextAvg=((s.averageRating||0)*(s.reviewCount||0)+draft.rating)/nextCount;return {...s,reviewCount:nextCount,averageRating:parseFloat(nextAvg.toFixed(1))};}return s;}));return {review:local,mode:'preview'};}
 async function checkInEventTicket(eventId,ticketId){if(serviceState==='connected'&&auth?.accessToken){const res=await api.events.checkIn(eventId,ticketId,auth.accessToken);setEventAttendees(old=>({...old,[eventId]:(old[eventId]||[]).map(a=>a.ticketId===ticketId||a.id===ticketId?{...a,checkedIn:true,checkedInAt:res.checkedInAt}:a)}));return {checkIn:res,mode:'synced'};}const now=new Date().toISOString();setEventAttendees(old=>({...old,[eventId]:(old[eventId]||[]).map(a=>a.ticketId===ticketId||a.id===ticketId?{...a,checkedIn:true,checkedInAt:now}:a)}));return {checkIn:{ticketId,eventId,checkedInAt:now,state:'checked_in'},mode:'preview'};}
 async function broadcastEventReminder(eventId,draft){if(serviceState==='connected'&&auth?.accessToken){const res=await api.events.broadcastReminder(eventId,draft,auth.accessToken);return {result:res,mode:'synced'};}notify(draft.title||'Event reminder',draft.message||'Reminder broadcast sent to attendees.','Events');return {result:{deliveredCount:1},mode:'preview'};}
 async function fetchEventAttendees(eventId){if(serviceState==='connected'&&auth?.accessToken){const list=await api.events.attendees(eventId,auth.accessToken);setEventAttendees(old=>({...old,[eventId]:list}));return list;}return eventAttendees[eventId]||[{id:'ticket-preview-1',ticketId:'ticket-preview-1',profileId:'user-1',displayName:'Ananya Sharma',state:'registered',checkedIn:false},{id:'ticket-preview-2',ticketId:'ticket-preview-2',profileId:'user-2',displayName:'Riya Patel',state:'registered',checkedIn:true,checkedInAt:new Date().toISOString()}];}
 async function applyForJob(job,note,resumeObjectId){let remote=null;if(job.remote&&serviceState==='connected'&&auth?.accessToken)remote=await api.career.apply(job.id,{note,resumeObjectId},auth.accessToken);setApplications(old=>({...old,[job.id]:remote?.state||'submitted'}));setCareerJobs(old=>{const exists=(old||[]).some(j=>j.id===job.id);return exists?old:[job,...(old||[])];});return {application:remote,mode:remote?'synced':'preview'};}
 async function fetchJobApplications(jobId){if(serviceState==='connected'&&auth?.accessToken){const list=await api.career.jobApplications(jobId,auth.accessToken);setJobApplications(old=>({...old,[jobId]:list}));return list;}return jobApplications[jobId]||[];}
 async function reviewJobApplication(jobId,applicationId,status,reviewNotes){if(serviceState==='connected'&&auth?.accessToken){const remote=await api.career.reviewApplication(jobId,applicationId,{status,reviewNotes},auth.accessToken);setJobApplications(old=>({...old,[jobId]:(old[jobId]||[]).map(a=>a.id===applicationId?{...a,state:remote.state,reviewNotes:remote.reviewNotes}:a)}));return {application:remote,mode:'synced'};}setJobApplications(old=>({...old,[jobId]:(old[jobId]||[]).map(a=>a.id===applicationId?{...a,state:status,reviewNotes}:a)}));return {mode:'preview'};}
 async function fetchMentorSlots(mentorId){if(serviceState==='connected'&&auth?.accessToken){const list=await api.mentors.slots(mentorId,auth.accessToken);setMentorSlots(old=>({...old,[mentorId]:list}));return list;}return mentorSlots[mentorId]||[];}
 async function createMentorSlot(slotTime,durationMinutes=30){if(serviceState==='connected'&&auth?.accessToken){const remote=await api.mentors.createSlot({slotTime,durationMinutes},auth.accessToken);setMentorSlots(old=>({...old,me:[...(old.me||[]),remote]}));return {slot:remote,mode:'synced'};}const local={id:crypto.randomUUID(),slotTime,durationMinutes,booked:false,createdAt:new Date().toISOString()};setMentorSlots(old=>({...old,me:[...(old.me||[]),local]}));return {slot:local,mode:'preview'};}
 async function bookMentorSlot(mentorId,slotId,sessionType='1:1 mentoring',note=''){if(serviceState==='connected'&&auth?.accessToken){const res=await api.mentors.bookSlot(slotId,{sessionType,note},auth.accessToken);setMentorSlots(old=>({...old,[mentorId]:(old[mentorId]||[]).map(s=>s.id===slotId?{...s,booked:true}:s)}));const session={id:res.session?.id||crypto.randomUUID(),title:`Session · ${sessionType}`,date:new Date(res.slot?.slotTime||Date.now()).toLocaleString([],{dateStyle:'medium',timeStyle:'short'}),status:res.session?.state||'Confirmed',remote:true,mentorId};setBookings(old=>[session,...old]);return {session:res.session,mode:'synced'};}setMentorSlots(old=>({...old,[mentorId]:(old[mentorId]||[]).map(s=>s.id===slotId?{...s,booked:true}:s)}));const session={id:crypto.randomUUID(),title:`Session · ${sessionType}`,date:new Date().toLocaleString([],{dateStyle:'medium',timeStyle:'short'}),status:'Confirmed',remote:false,mentorId};setBookings(old=>[session,...old]);return {session,mode:'preview'};}
 async function openCourse(course){if(!course.remote||serviceState!=='connected'||!auth?.accessToken)return {course,mode:'preview'};let enrollment=course.enrollmentState;if(enrollment!=='active'){const remote=await api.learning.enroll(course.id,auth.accessToken);enrollment=remote.enrollmentState;}
  const lessons=await api.learning.lessons(course.id,auth.accessToken);if(!lessons.length)throw new Error('This course is being prepared and has no published lessons yet.');const opened={...course,enrollmentState:enrollment,lessons:lessons.map(item=>item.title),lessonIds:lessons.map(item=>item.id),lessonObjects:lessons};setCourseCatalog(old=>old.map(item=>item.id===course.id?opened:item));return {course:opened,mode:'synced'};}
 async function completeCourseLesson(course,index){if(course.remote&&serviceState==='connected'&&auth?.accessToken&&course.lessonIds?.[index])await api.learning.completeLesson(course.id,course.lessonIds[index],auth.accessToken);setLearning(old=>({...old,[course.id]:[...new Set([...(old[course.id]||[]),index])]}));return course.remote&&serviceState==='connected'&&auth?.accessToken?'synced':'preview';}
 async function claimCertificate(course){if(serviceState==='connected'&&auth?.accessToken&&course.remote){const remote=await api.learning.claimCertificate(course.id,auth.accessToken);setCertificates(old=>[remote,...old.filter(c=>c.id!==remote.id)]);return {certificate:remote,mode:'synced'};}const local={id:crypto.randomUUID(),courseId:course.id,courseTitle:course.title,recipientName:profile.name,certificateNumber:'COLLOB-'+new Date().getFullYear()+'-'+crypto.randomUUID().slice(0,8).toUpperCase(),issuedAt:new Date().toISOString(),verificationHash:crypto.randomUUID().replace(/-/g,''),verificationUrl:'/certificates/preview',mode:'preview'};setCertificates(old=>[local,...old.filter(c=>c.courseId!==course.id)]);return {certificate:local,mode:'preview'};}
 async function requestMentorSession(mentor,draft){let remote=null;if(mentor.remote&&serviceState==='connected'&&auth?.accessToken)remote=await api.mentors.requestSession(mentor.id,draft,auth.accessToken);const session={id:remote?.id||`mentor-${crypto.randomUUID()}`,title:`Session · ${draft.sessionType}`,date:new Date(draft.startsAt).toLocaleString([], {dateStyle:'medium',timeStyle:'short'}),status:remote?.state||'Confirmed',price:mentor.rate,remote:Boolean(remote),mentorId:mentor.id};setBookings(old=>[session,...old.filter(item=>item.id!==session.id)]);return {session,mode:remote?'synced':'preview'};}
 async function publishPost(draft){const local=postForUi(draft);if(serviceState==='connected'&&auth?.accessToken&&draft.type==='post'){const remote=await api.social.createPost({body:draft.text||draft.title,visibility:'public',tags:draft.tags},auth.accessToken);setPosts(old=>[{...local,id:remote.id,text:remote.body,tags:remote.tags,createdAt:remote.createdAt},...old]);return 'synced'}setPosts(old=>[local,...old]);return 'preview'}
 async function addComment(postId,body,parentCommentId=null){const newComment={id:crypto.randomUUID(),author:profile.name,authorId:'me',role:profile.headline||'Member',time:'Just now',body,likes:0,avatar:profile.image||null,parentId:parentCommentId||null,replies:[]};if(parentCommentId){function insertNested(items){let found=false;function traverse(list){return list.map(it=>{if(it.id===parentCommentId){found=true;return {...it,replies:[...(it.replies||[]),newComment]};}if(it.replies&&it.replies.length>0){const updatedSub=traverse(it.replies);if(found){return {...it,replies:updatedSub};}}return it;});}const updated=traverse(items);if(!found&&items.length>0){return items.map((it,idx)=>idx===0?{...it,replies:[...(it.replies||[]),newComment]}:it);}return updated;}setComments(old=>{const existing=old[postId]||initialComments[postId]||[];return {...old,[postId]:insertNested(existing)};});return 'preview';}if(serviceState==='connected'&&auth?.accessToken){const remote=await api.social.createComment(postId,{body},auth.accessToken);setComments(old=>({...old,[postId]:[...(old[postId]||initialComments[postId]||[]),remote]}));return 'synced'}setComments(old=>({...old,[postId]:[...(old[postId]||initialComments[postId]||[]),newComment]}));return 'preview'}
 async function uploadFile(file){if(!file)throw new Error('No file selected');if(file.size>25*1024*1024)throw new Error('Files must be 25 MB or smaller');const contentType=file.type||'application/octet-stream';if(serviceState==='connected'&&auth?.accessToken){const ticket=await api.storage.requestUpload({filename:file.name,contentType,contentLength:file.size},auth.accessToken);await api.storage.putFile(ticket,file);const object=await api.storage.completeUpload(ticket.objectId,auth.accessToken);return {mode:'synced',attachment:attachmentForUi(object)}}return {mode:'preview',attachment:{objectId:`preview-${crypto.randomUUID()}`,filename:file.name,contentType,expectedBytes:file.size,state:'preview'}}}
 async function sendMessage(conversationId,body,attachments=[]){if(serviceState==='connected'&&auth?.accessToken){const remote=await api.messages.send(conversationId,{body,attachmentIds:attachments.map(attachment=>attachment.objectId)},auth.accessToken);return {mode:'synced',message:messageForUi(remote,profile.profileId)}}return {mode:'preview',message:null}}
 async function markConversationRead(conversationId){if(serviceState==='connected'&&auth?.accessToken){try{await api.messages.markRead(conversationId,auth.accessToken)}catch{setToast('Could not sync this conversation read state.')}}}
 function sendTyping(conversationId,typing){if(serviceState==='connected'&&auth?.accessToken)sendRealtime(`/ws/conversations/${conversationId}/typing`,{typing});}
 async function downloadMessageAttachment(conversationId,attachment){if(serviceState!=='connected'||!auth?.accessToken){setToast(`${attachment.filename} is available after connecting file storage.`);return}const ticket=await api.messages.downloadAttachment(conversationId,attachment.objectId,auth.accessToken);window.open(ticket.downloadUrl,'_blank','noopener,noreferrer')}
 async function toggleReaction(postId){const liked=likes.includes(postId);if(serviceState==='connected'&&auth?.accessToken){if(liked)await api.social.removeReaction(postId,auth.accessToken);else await api.social.setReaction(postId,{type:'like'},auth.accessToken)}setLikes(old=>liked?old.filter(id=>id!==postId):[...old,postId]);return serviceState==='connected'&&auth?.accessToken?'synced':'preview'}
 async function toggleFollow(targetProfileId){const isFollowing=following.includes(targetProfileId);if(serviceState==='connected'&&auth?.accessToken){try{if(isFollowing)await api.social.unfollow(targetProfileId,auth.accessToken);else await api.social.follow(targetProfileId,auth.accessToken);}catch{setToast('Could not update connection. Please try again.');return;}}setFollowing(old=>isFollowing?old.filter(id=>id!==targetProfileId):[...old,targetProfileId]);setToast(isFollowing?'Unfollowed':'Following');}
 function removeFollower(targetProfileId){setFollowers(old=>(old||[]).filter(id=>id!==targetProfileId));setToast('Removed from followers');}
  async function votePoll(postId,pollId,optionId,optionIndex){const currentVote=votes[postId];if(currentVote===optionIndex){setVotes(old=>{const next={...old};delete next[postId];return next;});setToast('Vote removed');return;}if(serviceState==='connected'&&auth?.accessToken&&pollId&&optionId){try{await api.social.votePoll(postId,pollId,optionId,auth.accessToken);}catch{/* Gracefully support client-side re-voting */}}setVotes(old=>({...old,[postId]:optionIndex}));setToast(currentVote!==undefined?'Vote changed':'Vote recorded');}
 async function reportContent({contentType,contentId,reason,details}){if(serviceState==='connected'&&auth?.accessToken){try{await api.social.report({contentType,contentId,reason:reason||'SPAM',details:details||''},auth.accessToken);}catch{setToast('Could not submit report.');return;}}setToast('Thanks. This report is queued for moderation review.');}
 async function loadMoreFeed(){if(feedState.loading||!feedState.nextCursor||!auth?.accessToken)return;setFeedState(old=>({...old,loading:true,error:''}));try{const result=await api.social.feed(feedState.nextCursor,20,auth.accessToken);setPosts(old=>[...old,...result.items.map(feedItemToPost).filter(item=>!old.some(post=>post.id===item.id))]);setLikes(old=>[...new Set([...old,...result.items.filter(item=>item.viewerReacted).map(item=>item.id)])]);setFeedState({loading:false,nextCursor:result.nextCursor,error:''})}catch{setFeedState(old=>({...old,loading:false,error:'Could not load more updates. Please try again.'}))}}
 async function markNotificationRead(notification){if(!notification.unread)return;setNotifications(old=>old.map(item=>item.id===notification.id?{...item,unread:false}:item));if(serviceState==='connected'&&auth?.accessToken){try{await api.notifications.markRead(notification.id,auth.accessToken)}catch{setNotifications(old=>old.map(item=>item.id===notification.id?{...item,unread:true}:item));setToast('Could not mark this notification as read.')}}}
 async function updateNotificationPreferences(next){const previous=preferences;setPreferences(next);if(serviceState==='connected'&&auth?.accessToken){try{const remote=await api.notifications.updatePreferences(next,auth.accessToken);setPreferences(remote)}catch{setPreferences(previous);setToast('Could not save notification preferences.')}}}
 async function createPaymentOrder(draft){if(auth?.accessToken){if(serviceState!=='connected')throw new Error('Payment orders require an active connection to the payment service.');const remote=await api.payments.createOrder(draft,auth.accessToken);setPaymentOrders(old=>[remote,...old.filter(o=>o.id!==remote.id)]);return {order:remote,mode:'synced'};}const local={id:`order-${crypto.randomUUID()}`,orderNumber:`ORD-PREVIEW-${Date.now()}`,amountCents:draft.amountCents,currency:draft.currency||'INR',description:draft.description||'',status:'created',createdAt:new Date().toISOString(),remote:false};setPaymentOrders(old=>[local,...old]);return {order:local,mode:'preview'};}
 async function confirmPaymentOrder(orderId,gatewayPaymentId){if(auth?.accessToken){if(serviceState!=='connected')throw new Error('Payment confirmation requires an active connection to the payment service.');const remote=await api.payments.confirmOrder(orderId,{gatewayPaymentId},auth.accessToken);setPaymentOrders(old=>old.map(o=>o.id===orderId?{...o,...remote}:o));return {order:remote,mode:'synced'};}setPaymentOrders(old=>old.map(o=>o.id===orderId?{...o,status:'completed',gatewayPaymentId,completedAt:new Date().toISOString()}:o));return {mode:'preview'};}
 async function requestPayout(draft){if(auth?.accessToken){if(serviceState!=='connected')throw new Error('Payout requests require an active connection to the payment service.');const remote=await api.payments.requestPayout(draft,auth.accessToken);setPayoutRequests(old=>[remote,...old]);return {payout:remote,mode:'synced'};}const local={id:`payout-${crypto.randomUUID()}`,amountCents:draft.amountCents,method:draft.method||'bank_account',destinationReference:draft.destinationReference||'',status:'pending',createdAt:new Date().toISOString(),remote:false};setPayoutRequests(old=>[local,...old]);return {payout:local,mode:'preview'};}
 function go(next,entityId=null){if(page!=='Notifications'){setPrevPage(page)}const targetHash=formatRoute(next,entityId);if(location.hash!==targetHash){location.hash=targetHash;}else{setPage(resolvePage(targetHash));}setMobileMenu(false);setSearch('');setSearchExpanded(false)}
 async function logout(){const session=auth;setModal(null);setMobileMenu(false);setAuth(null);location.hash='#/feed';setPage('Home');if(serviceState==='connected'){try{await api.auth.logout(session?.refreshToken,session?.accessToken)}catch{setToast('Signed out on this device.')}}}
 async function toggleSave(id){const isCurrentlySaved=saved.includes(id);if(serviceState==='connected'&&auth?.accessToken){try{if(isCurrentlySaved){await api.social.unsavePost(id,auth.accessToken);}else{await api.social.savePost(id,auth.accessToken);}}catch(err){console.warn('Could not sync save state with cloud:',err);}}setSaved(old=>old.includes(id)?old.filter(x=>x!==id):[...old,id])}
 function toggleSaveEvent(id){setSavedEvents(old=>old.includes(id)?old.filter(x=>x!==id):[...old,id])}
 function notify(title,text,module){setNotifications(old=>[{id:crypto.randomUUID(),title,text,module,time:'Just now',unread:true},...old])}
 const [activeThreadId,setActiveThreadId]=useState(null);
 function openConversation(personOrThread){
  let threadId=null;
  if(typeof personOrThread==='string'){
    const existing=threads.find(t=>t.id===personOrThread||t.name?.toLowerCase()===personOrThread.toLowerCase());
    if(existing){
      threadId=existing.id;
    }else{
      const newT={id:`t-${crypto.randomUUID()}`,name:personOrThread,context:'Direct',status:'connected',unread:false,messages:[]};
      setThreads(old=>[newT,...old]);
      threadId=newT.id;
    }
  }else if(personOrThread&&typeof personOrThread==='object'){
    const pName=personOrThread.name||personOrThread.displayName||'User';
    const existing=threads.find(t=>t.id===personOrThread.id||t.name?.toLowerCase()===pName.toLowerCase());
    if(existing){
      threadId=existing.id;
    }else{
      const newT={id:`t-${personOrThread.id||crypto.randomUUID()}`,name:pName,context:personOrThread.role||'Direct',status:'connected',unread:false,messages:[]};
      setThreads(old=>[newT,...old]);
      threadId=newT.id;
    }
  }
  setActiveThreadId(threadId);
  setModal(null);
  go('Messages',threadId);
 }
 function openUserProfile(personOrName, initialTab = 'Posts') {
  if (!personOrName) return;
  setModal(null);
  const groupThread = threads.find(t =>
    (t.context === 'Group' || t.isGroup) &&
    (t.id === personOrName || t.name?.toLowerCase() === (typeof personOrName === 'string' ? personOrName : personOrName.name)?.toLowerCase())
  );
  if (groupThread || (typeof personOrName === 'object' && (personOrName.context === 'Group' || personOrName.isGroup))) {
    // Requirement 2: Do not open anything if it was a group profile
    return;
  }
  let targetPerson = null;
  if (typeof personOrName === 'string') {
    const isMe = personOrName.toLowerCase() === 'me' || personOrName.toLowerCase() === profile.name?.toLowerCase();
    if (!isMe) {
      const found = people.find(p => p.name?.toLowerCase() === personOrName.toLowerCase() || p.id === personOrName);
      targetPerson = found || {
        id: personOrName.toLowerCase().replace(/\s+/g, '-'),
        name: personOrName,
        role: 'Collobster Member',
        skills: ['Community', 'Collaboration'],
      };
    }
  } else {
    const isMe = personOrName.id === 'me' || personOrName.id === profile.profileId || personOrName.name?.toLowerCase() === profile.name?.toLowerCase();
    targetPerson = isMe ? null : personOrName;
  }
  setViewedProfile(targetPerson);
  setProfileTab(initialTab || 'Posts');
  go('Profile');
 }
 const [eventsInitialView, setEventsInitialView] = useState('Discover');
 const [activeTicketEventId, setActiveTicketEventId] = useState(null);
 function openEventsTickets(eventId = null) {
  setEventsInitialView('My tickets');
  if (eventId) setActiveTicketEventId(eventId);
  setModal(null);
  go('Events');
 }
 const appProfile={...profile,roles:Array.isArray(profile.roles)?profile.roles:[profile.roles||'Builder']};
 const value={page,go,logout,auth,theme,setTheme,profile:appProfile,setProfile,saveProfile,updateProfilePrivacy,createCompany,companyDrafts,companyInvitations,searchProfiles,inviteCompanyMember,companyTasks,createCompanyTask,updateCompanyTask,companyDocuments,addCompanyDocument,downloadCompanyDocument,archiveCompanyDocument,createProject,projectDrafts,projectFiles,saveProjectFile,deleteProjectFile,projectTasks,createProjectTask,updateProjectTaskStatus,projectMilestones,createProjectMilestone,inviteProjectMember,acceptProjectInvitation,projectInvitations,acceptCompanyInvitation,registerForEvent,checkInEventTicket,broadcastEventReminder,eventAttendees,fetchEventAttendees,joinCommunity,leaveCommunity,publishCommunityPost,addCommunityComment,toggleFollow,removeFollower,votePoll,reportContent,bookService,serviceListings,serviceReviews,fetchServiceReviews,createServiceReview,applyForJob,jobApplications,fetchJobApplications,reviewJobApplication,mentorSlots,fetchMentorSlots,createMentorSlot,bookMentorSlot,careerJobs,openCourse,completeCourseLesson,claimCertificate,certificates,setCertificates,courseCatalog,eventCatalog,requestMentorSession,mentorCatalog,publishPost,addComment,uploadFile,sendMessage,markConversationRead,sendTyping,downloadMessageAttachment,toggleReaction,loadMoreFeed,feedState,posts,setPosts,saved,toggleSave,savedEvents,setSavedEvents,toggleSaveEvent,communityCatalog,setCommunityCatalog,deletedCommunityIds,setDeletedCommunityIds,sidebarOpen,setSidebarOpen,likes,setLikes,following,setFollowing,followers,setFollowers,comments,setComments,votes,setVotes,learning,setLearning,applications,setApplications,projectState,setProjectState,memberships,setMemberships,registrations,setRegistrations,bookings,setBookings,pipeline,setPipeline,threads,setThreads,activeThreadId,setActiveThreadId,openConversation,openUserProfile,viewedProfile,setViewedProfile,profileTab,setProfileTab,eventsInitialView,setEventsInitialView,activeTicketEventId,setActiveTicketEventId,openEventsTickets,notifications,setNotifications,markNotificationRead,preferences,setPreferences:updateNotificationPreferences,modal,setModal,toast:setToast,notify,search,setSearch,serviceState,paymentOrders,setPaymentOrders,payoutRequests,setPayoutRequests,createPaymentOrder,confirmPaymentOrder,requestPayout,activeCall,setActiveCall,startCall,endCall,callLog,setCallLog,toggleNotifications};
 const pages={Home:<Feed/>,Saved:<Feed savedOnly/>,Explore:<Explore/>,Learn:<Learning/>,Career:<Career/>,Mentor:<Mentor/>,Projects:<Projects/>,Company:<Company/>,Investment:<Investment/>,'Local services':<Services/>,Events:<Events/>,Community:<Community/>,Messages:<Messages/>,Notifications:<Notifications/>,Profile:<Profile/>,Settings:<SettingsPage/>,NotFound:<NotFound go={go}/>};
 const unread=notifications.filter(n=>n.unread).length;
 if(!auth)return <Auth onComplete={completeAuth}/>;
 return <AppContext.Provider value={value}><a className="skip-link" href="#main-content" onClick={e=>{e.preventDefault();document.querySelector('#main-content').focus()}}>Skip to content</a>
 <aside className={`sidebar ${sidebarOpen?'open':''}`}><button className="brand" onClick={()=>go('Home')} aria-label="Collobster home"><img src="/favicon.svg" alt=""/><span>collobster<span className="brand-period">.</span></span></button><IconButton icon={X} label="Close navigation" onClick={()=>setSidebarOpen(false)}/><div className="workspace-label">YOUR EVERYDAY, CONNECTED</div><nav aria-label="Main navigation">{navigation.map(([name,Icon],i)=><React.Fragment key={name}>{i===2&&<div className="nav-divider"/>}<button className={`nav-item ${page===name?'active':''}`} onClick={()=>go(name)} aria-current={page===name?'page':undefined}><Icon size={19}/><span>{name}</span>{name==='Community'&&<span className="tiny-dot"/>}</button></React.Fragment>)}</nav><div className="sidebar-bottom"><div className="nav-divider"/><button className={`nav-item ${page==='Messages'?'active':''}`} onClick={()=>go('Messages')}><MessageCircle size={19}/>Messages<span className="nav-count">{threads.filter(t=>t.unread).length}</span></button><button className={`nav-item ${page==='Saved'?'active':''}`} onClick={()=>go('Saved')}><Bookmark size={19}/>Saved</button><button className={`nav-item ${page==='Notifications'?'active':''}`} onClick={toggleNotifications}><Bell size={19}/>Notifications{unread>0&&<span className="nav-count">{unread}</span>}</button><button className={`nav-item ${page==='Settings'?'active':''}`} onClick={()=>go('Settings')}><Settings size={19}/>Settings</button><button className={`nav-item ${page==='Profile'?'active':''}`} onClick={()=>{setViewedProfile(null);setProfileTab('Posts');go('Profile');}} aria-current={page==='Profile'?'page':undefined}><User size={19}/>Profile</button><button className={`account ${page==='Profile'?'active':''}`} onClick={()=>{setViewedProfile(null);setProfileTab('Posts');go('Profile');}}><Avatar person={profile} name={profile.name}/><span className="account-info"><strong>{profile.name}</strong><small>Your personal space</small></span><ChevronDown size={16}/></button></div></aside>
 {sidebarOpen&&<button className="menu-backdrop" aria-label="Close menu" onClick={()=>setSidebarOpen(false)}/>}
 <div className="app-body"><header className="topbar" style={{position:'sticky',top:0}}><IconButton className="topbar-hamburger-btn" icon={Menu} label="Open navigation menu" onClick={()=>setSidebarOpen(prev=>!prev)}/><button className="mobile-top" onClick={()=>go('Home')} aria-label="Collobster home"><img src="/favicon.svg" alt=""/><span>collobster<span className="brand-period">.</span></span></button><div className="topbar-actions"><span className={`demo-label service-${serviceState}`} title={serviceState==='connected'?'API connected':serviceState==='checking'?'Checking API connection':'Interactive preview with device-local data'}>{serviceState==='connected'?'Connected':'Preview'}</span><IconButton icon={theme==='dark'?Sun:Moon} label={theme==='dark'?'Switch to light mode':'Switch to dark mode'} onClick={()=>setTheme(theme==='dark'?'light':'dark')}/><div className="topbar-search-container"><button type="button" className="icon-button topbar-search-toggle-btn" aria-label="Open search" title="Search" onClick={()=>{setSearchExpanded(true);setTimeout(()=>searchInputRef.current?.focus(),50);}}><Search size={19}/></button></div><button className={`icon-button notification-button ${page==='Notifications'?'active':''}`} aria-label={`Notifications, ${unread} unread`} title={page==='Notifications'?'Close notifications':'Notifications'} onClick={toggleNotifications}><Bell size={19}/>{unread>0&&<i/>}</button><button className="top-avatar" onClick={()=>{setViewedProfile(null);setProfileTab('Posts');go('Profile');}} aria-label="Your profile"><Avatar person={profile} name={profile.name}/></button></div>{searchExpanded&&<div className="topbar-search-popup" role="search"><form className="topbar-search-popup-form" onSubmit={e=>{e.preventDefault();const q=search;go('Explore');setSearch(q);setSearchExpanded(false);}}><button type="button" className="topbar-search-popup-icon-btn" aria-label="Back / Close search" onClick={()=>setSearchExpanded(false)}><ArrowLeft size={19}/></button><div className="topbar-search-popup-input-wrapper"><Search size={16} className="topbar-search-popup-input-icon"/><input ref={searchInputRef} type="search" value={search} onChange={e=>setSearch(e.target.value)} placeholder="Search people, ideas, tags..." aria-label="Search Collobster" className="topbar-search-popup-input" autoFocus onKeyDown={e=>{if(e.key==='Escape')setSearchExpanded(false)}}/>{search&&<button type="button" className="topbar-search-action-btn" aria-label="Clear search" onClick={()=>{setSearch('');searchInputRef.current?.focus();}}><X size={14}/></button>}</div><button type="submit" className="topbar-search-popup-search-btn">Search</button><button type="button" className="topbar-search-popup-icon-btn topbar-search-popup-close-btn" aria-label="Close search" onClick={()=>setSearchExpanded(false)}><X size={19}/></button></form></div>}</header>{searchExpanded&&<div className="topbar-search-backdrop" onClick={()=>setSearchExpanded(false)} aria-hidden="true"/>}
 {offline&&<div className="system-notice"><WifiOff size={16}/>You are offline. Your local workspace is still available.</div>}{storageError&&<div className="system-notice">Device storage is unavailable. Changes may not survive a reload.</div>}
 <main id="main-content" tabIndex="-1" className={`main-content ${page==='Messages'?'inbox-main':''}`}>{pages[page]||<NotFound go={go}/>}</main><footer className="mobile-nav">{[['Home',Home],['Explore',Compass],['Create',Plus],['Messages',MessageCircle],['Spaces',Menu]].map(([n,Icon])=><button key={n} className={`${page===n?'active':''} ${n==='Create'?'create-nav':''}`} onClick={()=>n==='Create'?setModal({type:'composer'}):n==='Spaces'?setSidebarOpen(prev=>!prev):go(n)}><span><Icon size={21}/></span><small>{n}</small></button>)}</footer></div>
 {toast&&<div className="toast" role="status">{toast}</div>}{modal?.type==='composer'&&<Composer/>}{modal?.type==='onboard'&&<Onboarding/>}{modal?.type==='reader'&&<Reader/>}{modal?.type==='details'&&<Details/>}{modal?.type==='edit-profile'&&<ProfileEditor/>}{modal?.type==='policy'&&<PolicyDialog title={modal.title} kind={modal.kind} onClose={()=>setModal(null)}/>}
 {activeCall&&!activeCall.minimized&&<CallModal call={activeCall} setCall={setActiveCall} onEnd={endCall} onMinimize={()=>setActiveCall(old=>old?{...old,minimized:true}:null)}/>}
 {activeCall&&activeCall.minimized&&<MinimizedCallBar call={activeCall} setCall={setActiveCall} onEnd={endCall} onExpand={()=>setActiveCall(old=>old?{...old,minimized:false}:null)}/>}
 </AppContext.Provider>
}
export function RightRail(){return <RailContents/>}
function profilePayload(profile){return {displayName:profile.name,username:profile.username||null,headline:profile.headline||null,bio:profile.bio||null,location:profile.location||null,birthDate:profile.dob||null,gender:profile.gender||null,skills:profile.skills||[],interests:profile.interests||[],privacy:profile.privacy||{}}}
function mergeRemoteProfile(profile,remote){return {...profile,profileId:remote.id||profile.profileId,name:remote.displayName||profile.name,username:remote.username||profile.username||'',headline:remote.headline||profile.headline||'',bio:remote.bio||profile.bio||'',location:remote.location||profile.location||'',dob:remote.birthDate||profile.dob||'',gender:remote.gender||profile.gender||'',skills:remote.skills?.length?remote.skills:(profile.skills||[]),interests:remote.interests?.length?remote.interests:(profile.interests||[]),privacy:remote.privacy||profile.privacy||{},image:remote.avatarUrl||profile.image||null,coverImage:remote.coverUrl||profile.coverImage||null}}
function postForUi(draft){return {id:crypto.randomUUID(),authorId:'me',type:draft.type,format:draft.format||draft.type,title:draft.title.trim(),text:draft.text.trim(),image:draft.image||undefined,imageAlt:draft.imageAlt||undefined,options:draft.options,votes:draft.options?.map(()=>0),tags:draft.tags,likes:0,comments:0,createdAt:new Date().toISOString()}}
function feedItemToPost(item){const hasPoll=Boolean(item.poll);return {id:item.id,authorId:item.authorId,authorName:item.authorName,authorHeadline:item.authorHeadline,type:hasPoll?'poll':'post',title:item.poll?.question||'',text:item.body,tags:item.tags||[],likes:item.reactionCount||0,comments:item.commentCount||0,pollId:item.poll?.id,options:item.poll?item.poll.options.map(o=>o.text):[],pollOptionIds:item.poll?item.poll.options.map(o=>o.id):[],votes:item.poll?item.poll.options.map(o=>o.voteCount):[],createdAt:item.createdAt}}
function notificationForUi(item){return {id:item.id,title:item.title,text:item.body||'',module:notificationModule(item.type),time:relativeTime(item.createdAt),unread:!item.readAt}}
function companyDocumentForUi(item){return {id:item.id,objectId:item.objectId,title:item.title,filename:item.filename,contentType:item.contentType,bytes:item.bytes,collection:item.collection,accessScope:item.accessScope,createdAt:item.createdAt,remote:true}}
function attachmentForUi(item){return {objectId:item.id,filename:item.filename,contentType:item.contentType,expectedBytes:item.verifiedBytes||item.expectedBytes,state:item.state}}
function messageForUi(item,profileId){return {id:item.id,mine:item.senderId===profileId,text:item.body,time:relativeTime(item.createdAt),attachments:(item.attachments||[]).map(attachmentForUi)}}
function conversationForUi(conversation,items,profileId,displayName){const other=(conversation.members||[]).find(member=>member.id!==profileId&&member.displayName!==displayName)||(conversation.members||[])[0];return {id:conversation.id,name:other?.displayName||conversation.title||'Conversation',context:conversation.type==='community'?'Community':conversation.type==='group'?'Group':'Direct',status:'connected',unread:false,messages:[...items].reverse().map(item=>messageForUi(item,profileId))}}
function notificationModule(type){if(type?.startsWith('post.'))return 'Home';if(type?.startsWith('message.'))return 'Messages';return 'Notifications'}
function relativeTime(value){const elapsed=Math.max(0,Date.now()-new Date(value).getTime());if(elapsed<60000)return 'Just now';if(elapsed<3600000)return `${Math.floor(elapsed/60000)}m ago`;if(elapsed<86400000)return `${Math.floor(elapsed/3600000)}h ago`;return `${Math.floor(elapsed/86400000)}d ago`}
function RailContents(){const a=React.useContext(AppContext),course=courses[0],completed=a.learning.flutter?.length||0;return <aside className="right-rail"><section><SectionTitle title="A little progress, every day"/><div className="learning-mini"><span className="module-icon blue"><BookOpen size={20}/></span><div><strong>Build your first Flutter app</strong><p>{completed?`${completed} of ${course.lessons.length} lessons complete`:'Your next chapter is waiting'}</p></div><Progress value={completed/course.lessons.length*100} label="Flutter course progress"/><button className="text-button" onClick={()=>a.setModal({type:'details',kind:'course',item:course})}>Continue learning<ArrowRight size={15}/></button></div></section><section><SectionTitle title="People to know" action="See all" onAction={()=>a.go('Explore')}/>{people.filter(p=>!a.following.includes(p.id)).slice(0,2).map(p=><div className="person-row" key={p.id}><Avatar person={p}/><div><strong>{p.name}</strong><p>{p.role.split('·')[0]}</p></div><IconButton icon={Plus} label={`Follow ${p.name}`} onClick={()=>{a.setFollowing([...a.following,p.id]);a.toast(`Following ${p.name}`)}}/></div>)}</section><section><SectionTitle title="Find your circle" action="Explore" onAction={()=>a.go('Community')}/>{communities.slice(0,2).map(c=><button className="community-mini" key={c.id} onClick={()=>a.setModal({type:'details',kind:'community',item:c})}><span className={`module-icon ${c.color}`}><Users size={20}/></span><span><strong>{c.title}</strong><small>{c.members} members</small></span><ArrowUpRight size={17}/></button>)}</section><section><SectionTitle title="Worth showing up for"/><button className="event-mini" onClick={()=>a.setModal({type:'details',kind:'event',item:events[0]})}><span className="date-tile"><small>SEP</small><b>26</b></span><span><strong>Build something, together.</strong><small>Sat, 10:00 AM · Bengaluru</small><span className="attendees"><Avatar person={people[0]}/><Avatar person={people[1]}/><Avatar person={people[2]}/><small>+39 going</small></span></span></button></section><div className="rail-note"><img src="/collobster-mascot.png" alt="Collobster mascot holding a notebook"/><div><strong>Grow a little. Together.</strong><p>Good company makes all the difference.</p></div></div><div className="rail-footer-container"><div className="rail-footer-contact"><a href="mailto:support@collobster.app" className="rail-contact-link">support@collobster.app</a><span>·</span><a href="tel:+918045678900" className="rail-contact-link">+91 80 4567 8900</a></div><div className="rail-footer-links"><button type="button" onClick={()=>a.setModal({type:'policy',title:'Terms of Service',kind:'terms'})}>Terms</button><button type="button" onClick={()=>a.setModal({type:'policy',title:'Privacy Policy',kind:'privacy'})}>Privacy</button><button type="button" onClick={()=>a.setModal({type:'policy',title:'Community Guidelines',kind:'guidelines'})}>Guidelines</button><button type="button" onClick={()=>a.setModal({type:'policy',title:'Help Center',kind:'help'})}>Help</button></div><p className="rail-footer">© {new Date().getFullYear()} Collobster, Inc.<br/>A space to connect, build, and belong.</p></div></aside>}
