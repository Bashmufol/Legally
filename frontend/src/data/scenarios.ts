import type { LucideIcon } from 'lucide-react'
import {
  Shield,
  FileText,
  Scale,
  Briefcase,
  ShoppingBag,
  Users,
  Wallet,
  Handshake,
  ScrollText,
} from 'lucide-react'
import type { Scenario } from '../types'

export interface SituationCard {
  id: Scenario
  title: string
  desc: string
  icon: LucideIcon
}

export const SITUATIONS: SituationCard[] = [
  {
    id: 'police_interaction',
    title: 'Police interaction',
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
    title: 'Land & property',
    desc: 'Purchase disputes, title verification, registration',
    icon: Scale,
  },
  {
    id: 'employment',
    title: 'Employment',
    desc: 'Wrongful termination, unpaid wages, workplace harassment',
    icon: Briefcase,
  },
  {
    id: 'consumer',
    title: 'Consumer rights',
    desc: 'Faulty goods, scams, refund and warranty disputes',
    icon: ShoppingBag,
  },
  {
    id: 'family',
    title: 'Family & marriage',
    desc: 'Custody, divorce, domestic violence, maintenance',
    icon: Users,
  },
  {
    id: 'debt',
    title: 'Debt & loans',
    desc: 'Loan harassment, illegal interest, recovery threats',
    icon: Wallet,
  },
  {
    id: 'business_contract',
    title: 'Business contracts',
    desc: 'Breach of agreement, partnership and vendor disputes',
    icon: Handshake,
  },
  {
    id: 'inheritance',
    title: 'Inheritance',
    desc: 'Will contests, estate sharing, family property fights',
    icon: ScrollText,
  },
]

export const SCENARIO_LABELS: { id: Scenario; label: string }[] = [
  ...SITUATIONS.map((s) => ({ id: s.id, label: s.title })),
  { id: 'general', label: 'General' },
]

export interface ScenarioConsultHelpers {
  shortTitle: string
  intro: string
  textPlaceholder: string
  uploadHint: string
  emptyInputHint: string
  resultsPlaceholder: string
}

export const CONSULT_HELPERS: Record<Scenario, ScenarioConsultHelpers> = {
  police_interaction: {
    shortTitle: 'Police interaction',
    intro:
      'Explain what happened during the police interaction at a stop or checkpoint. You can describe it in writing, record your account, or upload video or photos of the encounter.',
    textPlaceholder:
      'e.g. Officers asked for my phone password at a checkpoint in Ilorin. I refused but they insisted…',
    uploadHint: 'Upload dashcam video, photos, or a written stop-and-search notice',
    emptyInputHint:
      'Describe the police encounter, record what happened, or upload video or images from the stop.',
    resultsPlaceholder:
      'Your rights, relevant laws, and who to contact will appear here after a police-interaction consultation.',
  },
  tenancy: {
    shortTitle: 'Rent & tenancy',
    intro:
      'Tell us about rent increases, eviction threats, deposit disputes, or lease breaches. Attach your tenancy agreement if you have one.',
    textPlaceholder:
      'e.g. My landlord raised rent by 40% without notice and threatened to change the locks…',
    uploadHint: 'Upload your tenancy agreement, rent receipts, or eviction notices',
    emptyInputHint:
      'Describe your tenancy issue, record your situation, or upload your lease and notices.',
    resultsPlaceholder:
      'Your tenancy rights, legal steps, and contacts will appear here.',
  },
  land: {
    shortTitle: 'Land & property',
    intro:
      'Describe a land purchase, sale, or dispute. Upload agreements of sale, survey plans, or title documents for verification guidance.',
    textPlaceholder:
      'e.g. I paid a deposit on land in Kwara but the seller cannot produce a valid C of O…',
    uploadHint: 'Upload agreement of sale, survey plan, title documents, or property photos',
    emptyInputHint:
      'Describe your land issue or upload sale documents and photos for analysis.',
    resultsPlaceholder:
      'Land law guidance, red flags, and registry contacts will appear here.',
  },
  employment: {
    shortTitle: 'Employment',
    intro:
      'Explain termination, unpaid wages, harassment, or contract issues at work. Upload your employment letter or payslips if helpful.',
    textPlaceholder:
      'e.g. I was dismissed without notice after refusing unsafe work. I have not been paid for two months…',
    uploadHint: 'Upload employment contract, termination letter, or payslips',
    emptyInputHint:
      'Describe your workplace issue or upload employment documents.',
    resultsPlaceholder:
      'Employment law analysis and next steps will appear here.',
  },
  consumer: {
    shortTitle: 'Consumer rights',
    intro:
      'Describe faulty goods, scams, refused refunds, or warranty problems. Upload receipts, chats, or product photos.',
    textPlaceholder:
      'e.g. I bought a phone online that never arrived and the seller blocked me…',
    uploadHint: 'Upload receipts, warranty cards, chat screenshots, or product photos',
    emptyInputHint:
      'Describe the consumer dispute or upload proof of purchase and messages.',
    resultsPlaceholder:
      'Consumer protection guidance and escalation steps will appear here.',
  },
  family: {
    shortTitle: 'Family & marriage',
    intro:
      'Describe custody, divorce, maintenance, domestic violence, or inheritance within the family. Share only what you are comfortable with.',
    textPlaceholder:
      'e.g. My spouse stopped paying child maintenance ordered by the court…',
    uploadHint: 'Upload court orders, marriage certificate, or relevant messages (redact sensitive info)',
    emptyInputHint:
      'Describe your family law issue or upload supporting documents.',
    resultsPlaceholder:
      'Family law information and support contacts will appear here.',
  },
  debt: {
    shortTitle: 'Debt & loans',
    intro:
      'Explain loan harassment, illegal interest, or threats from lenders or recovery agents.',
    textPlaceholder:
      'e.g. A loan app is calling my contacts and threatening me after I missed one payment…',
    uploadHint: 'Upload loan agreement, demand letters, or harassment screenshots',
    emptyInputHint:
      'Describe the debt or harassment situation or upload loan documents.',
    resultsPlaceholder:
      'Debt recovery law and protection steps will appear here.',
  },
  business_contract: {
    shortTitle: 'Business contracts',
    intro:
      'Describe a breach, unpaid invoice, partnership dispute, or vendor problem. Upload the contract if available.',
    textPlaceholder:
      'e.g. A client refused to pay after we delivered the goods per our agreement…',
    uploadHint: 'Upload the contract, invoices, delivery proof, or email threads',
    emptyInputHint:
      'Describe the contract dispute or upload the agreement and related files.',
    resultsPlaceholder:
      'Contract law analysis and remedies will appear here.',
  },
  inheritance: {
    shortTitle: 'Inheritance',
    intro:
      'Explain who died, whether there is a will, and any dispute over property or sharing. Mention the country if different from your location.',
    textPlaceholder:
      'e.g. My father died without a will. His brothers want to sell the family house without including us…',
    uploadHint: 'Upload will, death certificate, or family property documents',
    emptyInputHint:
      'Describe the inheritance dispute or upload the will and related documents.',
    resultsPlaceholder:
      'Inheritance law guidance and practical steps will appear here.',
  },
  general: {
    shortTitle: 'General',
    intro:
      'Describe your issue in writing, by voice, or with uploads. Mention a country or state in your message to use that jurisdiction.',
    textPlaceholder:
      'e.g. I need to understand my rights in this situation and what steps to take next…',
    uploadHint: 'Upload any document, image, audio, or video that explains your issue',
    emptyInputHint:
      'Add a description, voice recording, or file (or combine them).',
    resultsPlaceholder:
      'Your analysis will appear here: summary, law, steps, and contacts.',
  },
}

export function getConsultHelpers(scenario: Scenario): ScenarioConsultHelpers {
  return CONSULT_HELPERS[scenario] ?? CONSULT_HELPERS.general
}
