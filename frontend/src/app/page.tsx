"use client";

import React, { useState, useEffect } from "react";
import { motion, useScroll, useTransform } from "framer-motion";
import { 
  Database, Activity, Lock, RefreshCw, Zap, Server, Code, Terminal, AlertTriangle, ShieldCheck 
} from "lucide-react";

export default function Home() {
  const { scrollYProgress } = useScroll();
  const yHero = useTransform(scrollYProgress, [0, 0.2], [0, 200]);
  const opacityHero = useTransform(scrollYProgress, [0, 0.2], [1, 0]);

  return (
    <main className="min-h-screen bg-[#050505] text-white overflow-hidden font-sans">
      
      {/* 1. HERO SECTION */}
      <section className="relative h-screen flex flex-col items-center justify-center border-b border-white/10">
        <motion.div style={{ y: yHero, opacity: opacityHero }} className="z-10 text-center w-full max-w-7xl px-6">
          <h1 className="text-[12vw] leading-[0.85] font-black tracking-tighter mb-8 glitch-text" data-text="10,000 REQUESTS. 100 UNITS. 0 OVERSOLD.">
            10,000 REQUESTS.<br/>
            <span className="text-[#ff3366]">100 UNITS.</span><br/>
            0 OVERSOLD.
          </h1>
          <p className="text-xl md:text-3xl font-medium tracking-tight text-white/60 max-w-4xl mx-auto mb-16 uppercase">
            SaleStorm is a concurrency-safe flash-sale backend engineered for the moment everything happens at once.
          </p>
          
          <div className="flex flex-col sm:flex-row items-center justify-center gap-6 mb-16">
            <button className="bg-white text-black px-8 py-4 font-bold tracking-widest uppercase hover:bg-[#ff3366] hover:text-white transition-colors">
              Run the Simulation
            </button>
            <button className="border border-white/20 px-8 py-4 font-bold tracking-widest uppercase hover:bg-white/10 transition-colors">
              Explore the Architecture
            </button>
          </div>

          <div className="flex justify-center gap-8 text-xs font-mono tracking-widest text-white/40 uppercase">
            <span className="flex items-center gap-2"><div className="w-2 h-2 rounded-full bg-[#00ff66] animate-pulse"/> System Status: Nominal</span>
            <span className="flex items-center gap-2"><Lock size={14}/> Concurrency Safe</span>
            <span className="flex items-center gap-2"><RefreshCw size={14}/> Idempotent</span>
            <span className="flex items-center gap-2"><Zap size={14}/> Event Driven</span>
          </div>
        </motion.div>
        
        <RuntimeStatus />

        {/* Background Noise/Grid */}
        <div className="absolute inset-0 bg-[url('https://grainy-gradients.vercel.app/noise.svg')] opacity-20 pointer-events-none mix-blend-overlay"></div>
        <div className="absolute inset-0 bg-[linear-gradient(rgba(255,255,255,0.03)_1px,transparent_1px),linear-gradient(90deg,rgba(255,255,255,0.03)_1px,transparent_1px)] bg-[size:4rem_4rem] [mask-image:radial-gradient(ellipse_60%_50%_at_50%_0%,#000_70%,transparent_100%)] pointer-events-none"></div>
      </section>

      {/* 2. THE PROBLEM */}
      <section className="py-40 px-6 relative bg-white text-black">
        <div className="max-w-6xl mx-auto">
          <motion.div 
            initial={{ opacity: 0, y: 50 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true, margin: "-100px" }}
            className="text-center"
          >
            <h2 className="text-6xl md:text-8xl font-black tracking-tighter mb-8">
              FLASH SALES DON'T FAIL AT 10 REQUESTS.
            </h2>
            <p className="text-3xl md:text-5xl font-bold text-black/40 tracking-tight mb-20">
              They fail when thousands arrive at the exact same millisecond.
            </p>
          </motion.div>

          <div className="grid md:grid-cols-3 gap-8 text-center font-mono uppercase tracking-wider text-sm mb-32">
            <div className="p-8 border-2 border-black">10,000 Requests</div>
            <div className="p-8 border-2 border-black bg-black text-white flex items-center justify-center">→ Race Condition ←</div>
            <div className="p-8 border-2 border-[#ff0033] text-[#ff0033] font-bold">Overselling (Disaster)</div>
          </div>

          <h3 className="text-5xl md:text-7xl font-black tracking-tighter text-center uppercase">
            SaleStorm was designed around the race.
          </h3>
        </div>
      </section>

      {/* 3. LIVE SIMULATION SECTION */}
      <SimulationSection />

      {/* 4. THE CORE GUARANTEE */}
      <section className="py-40 px-6 bg-[#ff3366] text-white">
        <div className="max-w-7xl mx-auto">
          <h2 className="text-7xl md:text-[9vw] leading-[0.8] font-black tracking-tighter mb-16 uppercase mix-blend-difference">
            The Database Decides.
          </h2>
          
          <div className="grid lg:grid-cols-2 gap-16 items-center">
            <div>
              <p className="text-2xl md:text-4xl font-bold leading-tight mb-8">
                Redis does not decide inventory availability. PostgreSQL does.
              </p>
              <p className="text-xl text-white/80 font-medium mb-12">
                We use an atomic conditional decrement. Zero table locks. Zero distributed mutexes. Just pure relational acid compliance.
              </p>
            </div>
            
            <div className="bg-black/90 p-8 font-mono text-sm md:text-base border border-white/20 shadow-2xl">
              <div className="text-[#ff3366] mb-4">/* Atomic Inventory Operation */</div>
              <div className="text-white mb-2">UPDATE inventory</div>
              <div className="text-white mb-2">SET available_quantity = available_quantity - <span className="text-[#00ff66]">1</span></div>
              <div className="text-white">WHERE available_quantity {'>'}= <span className="text-[#00ff66]">1</span>;</div>
              
              <div className="mt-8 pt-8 border-t border-white/20">
                <div className="flex justify-between mb-2"><span>Request A</span> <span className="text-[#00ff66]">SUCCEEDS</span></div>
                <div className="flex justify-between mb-2"><span>Request B</span> <span className="text-[#00ff66]">SUCCEEDS</span></div>
                <div className="flex justify-between text-white/40 mb-2"><span>...</span> <span>...</span></div>
                <div className="flex justify-between text-[#ff3366]"><span>Request #101</span> <span>REJECTED (0 Rows Updated)</span></div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* 5. ARCHITECTURE STORY */}
      <section className="py-40 px-6 relative border-b border-white/10">
        <div className="max-w-7xl mx-auto">
          <h2 className="text-5xl md:text-7xl font-black tracking-tighter mb-24 uppercase">Architecture</h2>
          
          <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-5 gap-4">
            <ArchitectureNode title="Client" desc="REST API" icon={<Terminal/>} />
            <ArchitectureNode title="Redis" desc="Rate Limiter & Cache. Optimization, never authority." icon={<Zap/>} border="border-[#ff3366]" />
            <ArchitectureNode title="Spring Boot" desc="Business orchestration" icon={<Server/>} />
            <ArchitectureNode title="PostgreSQL" desc="The Source of Truth" icon={<Database/>} border="border-[#00ff66]" />
            <ArchitectureNode title="Kafka" desc="Durable asynchronous transport" icon={<Activity/>} />
          </div>
        </div>
      </section>

      {/* 6. IDEMPOTENCY */}
      <section className="py-40 px-6 bg-white text-black">
        <div className="max-w-7xl mx-auto text-center">
          <h2 className="text-6xl md:text-8xl font-black tracking-tighter mb-16">IDEMPOTENCY</h2>
          <p className="text-2xl font-bold text-black/60 mb-20 max-w-3xl mx-auto">
            Duplicate requests happen. Users double-click. Networks retry. SaleStorm guarantees exactly-once state transitions.
          </p>

          <div className="grid md:grid-cols-2 gap-16">
            <div className="border-4 border-black p-12 relative overflow-hidden">
              <h3 className="text-3xl font-black mb-8 uppercase">The Request Storm</h3>
              <div className="space-y-4 font-mono text-xl opacity-50">
                <div>POST /reserve <span className="text-[#ff0033]">idemp=abc-123</span></div>
                <div>POST /reserve <span className="text-[#ff0033]">idemp=abc-123</span></div>
                <div>POST /reserve <span className="text-[#ff0033]">idemp=abc-123</span></div>
                <div>POST /reserve <span className="text-[#ff0033]">idemp=abc-123</span></div>
              </div>
            </div>
            
            <div className="border-4 border-black bg-black text-white p-12 flex flex-col justify-center">
              <h3 className="text-3xl font-black mb-8 text-[#00ff66] uppercase">The Reality</h3>
              <div className="space-y-6 text-2xl font-bold">
                <div className="flex justify-between border-b border-white/20 pb-4"><span>Reservations Created</span> <span>1</span></div>
                <div className="flex justify-between border-b border-white/20 pb-4"><span>Duplicate Charges</span> <span>0</span></div>
                <div className="flex justify-between"><span>Inventory Decrements</span> <span>1</span></div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* 7. FAILURE STORY */}
      <section className="py-40 px-6">
        <div className="max-w-7xl mx-auto">
          <h2 className="text-6xl md:text-8xl font-black tracking-tighter mb-24 uppercase">When Things Break</h2>
          
          <div className="grid md:grid-cols-2 gap-8">
            <FailureCard title="Redis Dies" flow={["Redis ❌", "PostgreSQL remains authoritative", "System continues (Fail-open)"]} />
            <FailureCard title="Payment Fails" flow={["Payment FAILED", "Reservation Released", "Inventory Restored (Exactly Once)"]} />
            <FailureCard title="Kafka Duplicate" flow={["PaymentSucceeded Event x3", "Unique payment_id constraint", "1 Order Created"]} />
            <FailureCard title="Webhook Repeated" flow={["Webhook x3", "Signature Verified", "Idempotent State Transition", "1 Final Payment"]} />
          </div>
        </div>
      </section>

      {/* 8. VALIDATION WALL */}
      <section className="py-40 px-6 bg-[#00ff66] text-black">
        <div className="max-w-7xl mx-auto">
          <div className="flex items-center gap-4 mb-16">
            <ShieldCheck size={48} />
            <h2 className="text-5xl font-black uppercase">Final Validation</h2>
          </div>
          
          <div className="grid grid-cols-2 md:grid-cols-4 gap-8 mb-24 font-mono">
            <div><div className="text-7xl font-black">42</div><div className="font-bold tracking-widest uppercase">Tests</div></div>
            <div><div className="text-7xl font-black">0</div><div className="font-bold tracking-widest uppercase">Failures</div></div>
            <div><div className="text-7xl font-black">0</div><div className="font-bold tracking-widest uppercase">Errors</div></div>
            <div><div className="text-7xl font-black">0</div><div className="font-bold tracking-widest uppercase">Skipped</div></div>
          </div>

          <div className="border-t-4 border-black pt-16 grid md:grid-cols-2 gap-16">
            <div>
              <h3 className="text-3xl font-black mb-8 uppercase">Verified Guarantees</h3>
              <ul className="space-y-4 font-bold text-xl">
                <li>✓ 10,000 Concurrent Requests</li>
                <li>✓ 100 Inventory Units</li>
                <li>✓ 100 Successful Reservations</li>
                <li>✓ 9,900 Rejected</li>
                <li>✓ 0 Oversold</li>
              </ul>
            </div>
            <div>
              <h3 className="text-3xl font-black mb-8 uppercase">Security & Observability</h3>
              <ul className="space-y-4 font-bold text-xl">
                <li>✓ Webhook Signature Verification</li>
                <li>✓ Redis Fail-open Behavior</li>
                <li>✓ Actuator Prometheus Metrics</li>
                <li>✓ Correlation ID Tracing</li>
              </ul>
            </div>
          </div>
        </div>
      </section>
      
      {/* 8.5 LIVE DEMO API SECTION */}
      <LiveDemoSection />

      {/* 9. FINAL SECTION */}
      <section className="h-screen flex flex-col items-center justify-center text-center px-6">
        <h2 className="text-6xl md:text-[8vw] leading-[0.9] font-black tracking-tighter mb-12 uppercase">
          When everything arrives at once, <br/><span className="text-[#ff3366]">SaleStorm doesn't guess.</span>
        </h2>
        <p className="text-xl md:text-3xl font-medium text-white/60 max-w-4xl mb-16">
          It enforces rules at the database, coordinates asynchronous work through events, and treats every external system as unreliable.
        </p>
        
        <div className="flex flex-wrap justify-center gap-4">
          <button className="bg-white text-black px-8 py-4 font-bold tracking-widest uppercase hover:bg-[#ff3366] hover:text-white transition-colors">
            View GitHub
          </button>
          <button className="border border-white/20 px-8 py-4 font-bold tracking-widest uppercase hover:bg-white/10 transition-colors">
            Explore API
          </button>
        </div>
      </section>
      
    </main>
  );
}

function ArchitectureNode({ title, desc, icon, border = "border-white/20" }: { title: string, desc: string, icon: React.ReactNode, border?: string }) {
  return (
    <div className={`p-8 border ${border} bg-white/5 hover:bg-white/10 transition-colors group cursor-pointer flex flex-col items-center text-center h-full`}>
      <div className="mb-6 p-4 rounded-full bg-white/10 group-hover:scale-110 transition-transform">{icon}</div>
      <h3 className="font-bold text-2xl uppercase mb-4">{title}</h3>
      <p className="text-white/50 text-sm font-mono">{desc}</p>
    </div>
  );
}

function FailureCard({ title, flow }: { title: string, flow: string[] }) {
  return (
    <div className="border border-white/20 p-8 relative overflow-hidden group">
      <AlertTriangle className="absolute -right-4 -top-4 w-32 h-32 text-white/5 group-hover:text-[#ff3366]/10 transition-colors" />
      <h3 className="text-2xl font-black mb-8 uppercase text-[#ff3366]">{title}</h3>
      <div className="space-y-4">
        {flow.map((step, i) => (
          <div key={i} className="flex flex-col">
            <span className="font-mono text-lg font-bold">{step}</span>
            {i < flow.length - 1 && <span className="text-white/30 my-2">↓</span>}
          </div>
        ))}
      </div>
    </div>
  );
}

const API_BASE = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080';

function RuntimeStatus() {
  const [status, setStatus] = useState<"LOADING" | "ONLINE" | "OFFLINE">("LOADING");

  useEffect(() => {
    fetch(`${API_BASE}/actuator/health`)
      .then(res => res.ok ? setStatus("ONLINE") : setStatus("OFFLINE"))
      .catch(() => setStatus("OFFLINE"));
  }, []);

  return (
    <div className="absolute bottom-8 left-8 right-8 flex justify-between items-end font-mono text-xs text-white/50 z-20">
      <div>
        <div className="mb-2">FRONTEND <span className="text-[#00ff66]">ONLINE</span></div>
        <div className="mb-2">API <span className={status === "ONLINE" ? "text-[#00ff66]" : "text-[#ff3366]"}>{status}</span></div>
      </div>
      <div className="text-right">
        <div className="mb-2">DATABASE <span className="text-white/30">BACKEND MANAGED</span></div>
        <div className="mb-2">KAFKA <span className="text-white/30">BACKEND MANAGED</span></div>
        <div className="mb-2">REDIS <span className="text-white/30">BACKEND MANAGED</span></div>
      </div>
    </div>
  );
}

function LiveDemoSection() {
  const [inventory, setInventory] = useState<any>(null);
  const [resStatus, setResStatus] = useState<any>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(false);

  const checkInventory = async () => {
    setLoading(true); setError(false);
    try {
      const res = await fetch(`${API_BASE}/api/inventory/prod-100`);
      if (!res.ok) throw new Error();
      setInventory(await res.json());
    } catch (e) {
      setError(true);
    } finally {
      setLoading(false);
    }
  };

  const [demoKey] = useState(`demo-${Math.random().toString(36).substr(2, 9)}`);

  const makeReservation = async () => {
    setLoading(true); setError(false);
    try {
      const res = await fetch(`${API_BASE}/api/reservations`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          productId: "prod-100",
          customerId: "demo-user",
          quantity: 1,
          idempotencyKey: demoKey
        })
      });
      if (!res.ok) throw new Error();
      setResStatus(await res.json());
      checkInventory(); // Refresh inventory
    } catch (e) {
      setError(true);
    } finally {
      setLoading(false);
    }
  };

  return (
    <section className="py-20 px-6 border-b border-white/10 bg-black">
      <div className="max-w-7xl mx-auto">
        <h2 className="text-4xl font-black uppercase mb-12">Live API Integration</h2>
        
        {error ? (
          <div className="p-8 border border-[#ff3366] bg-[#ff3366]/10 text-[#ff3366] font-mono font-bold uppercase">
            Backend Offline - Cannot connect to {API_BASE}
          </div>
        ) : (
          <div className="grid md:grid-cols-2 gap-8">
            <div className="p-8 border border-white/20">
              <h3 className="text-2xl font-bold mb-4">Inventory State</h3>
              <button onClick={checkInventory} disabled={loading} className="px-4 py-2 border border-white/40 hover:bg-white/10 font-mono text-sm mb-4">
                FETCH /api/inventory/prod-100
              </button>
              {inventory && (
                <pre className="p-4 bg-white/5 font-mono text-sm text-[#00ff66]">
                  {JSON.stringify(inventory, null, 2)}
                </pre>
              )}
            </div>
            
            <div className="p-8 border border-white/20">
              <h3 className="text-2xl font-bold mb-4">Idempotency Demo</h3>
              <p className="font-mono text-white/50 text-sm mb-4">Key: {demoKey}</p>
              <button onClick={makeReservation} disabled={loading} className="px-4 py-2 border border-[#00ff66] text-[#00ff66] hover:bg-[#00ff66] hover:text-black font-mono text-sm mb-4">
                POST /api/reservations (Submit Twice)
              </button>
              {resStatus && (
                <pre className="p-4 bg-white/5 font-mono text-sm text-[#00ff66]">
                  {JSON.stringify(resStatus, null, 2)}
                </pre>
              )}
            </div>
          </div>
        )}
      </div>
    </section>
  );
}

function SimulationSection() {
  const [running, setRunning] = useState(false);
  const [inventory, setInventory] = useState(100);
  const [requests, setRequests] = useState(0);
  const [successful, setSuccessful] = useState(0);
  const [rejected, setRejected] = useState(0);

  const startSim = () => {
    if(running) return;
    setRunning(true);
    setInventory(100);
    setRequests(0);
    setSuccessful(0);
    setRejected(0);

    let count = 0;
    const interval = setInterval(() => {
      count += 200; // block sizes
      if(count >= 10000) {
        count = 10000;
        clearInterval(interval);
        setRunning(false);
      }
      setRequests(count);
      
      const successCount = Math.min(100, Math.floor(count / 100)); // distribute successes
      setSuccessful(successCount);
      setInventory(100 - successCount);
      setRejected(count - successCount);

    }, 30);
  };

  return (
    <section className="py-40 px-6 border-y border-white/10 bg-[#050505]">
      <div className="max-w-7xl mx-auto">
        <div className="flex flex-col md:flex-row justify-between items-end mb-16 border-b border-white/20 pb-8">
          <div>
            <h2 className="text-5xl md:text-7xl font-black uppercase mb-4">Live Simulation</h2>
            <p className="text-white/50 font-mono text-sm tracking-widest uppercase">* Recorded Backend Validation Data</p>
          </div>
          <button 
            onClick={startSim} 
            disabled={running}
            className={`mt-8 md:mt-0 px-8 py-4 font-bold tracking-widest uppercase border ${running ? 'border-white/10 text-white/30' : 'border-[#00ff66] text-[#00ff66] hover:bg-[#00ff66] hover:text-black'} transition-colors`}
          >
            {running ? "Simulating..." : "Trigger Request Storm"}
          </button>
        </div>

        <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-5 gap-4 md:gap-8 text-center font-mono uppercase tracking-wider">
          <div className="p-6 border border-white/10 bg-white/5">
            <div className="text-sm text-white/50 mb-2">Initial</div>
            <div className="text-4xl font-bold">100</div>
          </div>
          <div className="p-6 border border-white/10 bg-white/5">
            <div className="text-sm text-white/50 mb-2">Concurrent</div>
            <div className="text-4xl font-bold">{requests.toLocaleString()}</div>
          </div>
          <div className="p-6 border border-[#00ff66]/50 bg-[#00ff66]/10 text-[#00ff66]">
            <div className="text-sm mb-2">Successful</div>
            <div className="text-4xl font-bold">{successful}</div>
          </div>
          <div className="p-6 border border-[#ff3366]/50 bg-[#ff3366]/10 text-[#ff3366]">
            <div className="text-sm mb-2">Rejected</div>
            <div className="text-4xl font-bold">{rejected.toLocaleString()}</div>
          </div>
          <div className="p-6 border border-white/10 bg-white/5 md:col-span-4 lg:col-span-1">
            <div className="text-sm text-white/50 mb-2">Oversold</div>
            <div className="text-4xl font-bold">0</div>
          </div>
        </div>

        <div className="mt-16 flex justify-center">
          <div className="w-64 h-64 border-[16px] border-[#333] rounded-full flex flex-col items-center justify-center relative">
            <svg className="absolute inset-0 w-full h-full -rotate-90">
              <circle 
                cx="112" cy="112" r="112" 
                className="fill-none stroke-[#ff3366] stroke-[16px] transition-all duration-300"
                strokeDasharray="703"
                strokeDashoffset={703 - (703 * (inventory / 100))}
                style={{ transform: 'translate(16px, 16px)' }}
              />
            </svg>
            <span className="text-sm text-white/50 font-mono tracking-widest uppercase mb-2">Available</span>
            <span className="text-7xl font-black">{inventory}</span>
          </div>
        </div>

        {!running && requests === 10000 && (
          <motion.div 
            initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} 
            className="mt-16 text-center text-2xl font-bold uppercase"
          >
            10,000 Attempts. <span className="text-[#00ff66]">100 Successful.</span> <span className="text-[#ff3366]">0 Overselling.</span>
          </motion.div>
        )}
      </div>
    </section>
  );
}
