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
