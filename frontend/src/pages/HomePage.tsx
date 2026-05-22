import { Link } from 'react-router-dom'
import { ArrowRight, Shield, Scale, FileText } from 'lucide-react'

const scenarios = [
  {
    id: 'police_stop',
    title: 'Police stop',
    desc: 'Unlawful search, phone checks, harassment on the road',
    icon: Shield,
  },
  {
    id: 'tenancy',
    title: 'Rent & tenancy',
    desc: 'Illegal rent hikes, eviction threats, lease breaches',
    icon: FileText,
  },
  {
    id: 'land',
    title: 'Land purchase',
    desc: 'Verify documents, disputes, registration steps',
    icon: Scale,
  },
]

export default function HomePage() {
  return (
    <div>
      <section className="bg-legally-navy text-white">
        <div className="max-w-6xl mx-auto px-4 py-20 md:py-28">
          <p className="text-legally-gold text-sm font-semibold tracking-wide uppercase mb-4">
            SDG 16 · Peace, Justice & Strong Institutions
          </p>
          <h1 className="font-display text-4xl md:text-5xl lg:text-6xl leading-tight max-w-3xl">
            Know your rights. <span className="text-legally-gold">Act with confidence.</span>
          </h1>
          <p className="mt-6 text-lg text-white/80 max-w-2xl">
            Legally is an AI-powered legal advisor for Nigerians — grounded on the Constitution,
            federal law, and Kwara State guidance. Text, speak, or upload evidence and get plain-English
            answers with sources and official contacts.
          </p>
          <div className="mt-10 flex flex-wrap gap-4">
            <Link
              to="/consult"
              className="inline-flex items-center gap-2 rounded-lg bg-legally-gold text-legally-navy px-6 py-3 font-semibold hover:opacity-90"
            >
              Start consultation
              <ArrowRight className="w-5 h-5" />
            </Link>
            <Link
              to="/about"
              className="inline-flex items-center gap-2 rounded-lg border border-white/30 px-6 py-3 font-medium hover:bg-white/10"
            >
              How it works
            </Link>
          </div>
        </div>
      </section>

      <section className="max-w-6xl mx-auto px-4 py-16">
        <h2 className="font-display text-2xl mb-8 text-center">Choose your situation</h2>
        <div className="grid md:grid-cols-3 gap-6">
          {scenarios.map((s) => (
            <Link
              key={s.id}
              to={`/consult?scenario=${s.id}`}
              className="group rounded-2xl bg-white border border-legally-navy/10 p-6 shadow-sm hover:shadow-md hover:border-legally-gold/40 transition"
            >
              <s.icon className="w-10 h-10 text-legally-gold mb-4" />
              <h3 className="font-semibold text-lg">{s.title}</h3>
              <p className="mt-2 text-sm text-legally-navy/70">{s.desc}</p>
              <span className="mt-4 inline-flex items-center gap-1 text-sm font-medium text-legally-gold group-hover:gap-2 transition-all">
                Consult now <ArrowRight className="w-4 h-4" />
              </span>
            </Link>
          ))}
        </div>
      </section>

      <section className="bg-white border-y border-legally-navy/10">
        <div className="max-w-6xl mx-auto px-4 py-12 grid md:grid-cols-3 gap-8 text-center">
          <div>
            <p className="text-3xl font-display text-legally-gold">Gemini</p>
            <p className="text-sm mt-1 text-legally-navy/70">Multimodal AI analysis</p>
          </div>
          <div>
            <p className="text-3xl font-display text-legally-gold">Firebase</p>
            <p className="text-sm mt-1 text-legally-navy/70">Auth & secure uploads</p>
          </div>
          <div>
            <p className="text-3xl font-display text-legally-gold">PostgreSQL</p>
            <p className="text-sm mt-1 text-legally-navy/70">Consultation history</p>
          </div>
        </div>
      </section>
    </div>
  )
}
