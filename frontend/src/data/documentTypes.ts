import {
  FileText,
  Home,
  Heart,
  Briefcase,
  Handshake,
  Shield,
  ScrollText,
  FileSignature,
  type LucideIcon,
} from 'lucide-react'

export type DocumentTypeId =
  | 'DEMAND_LETTER'
  | 'RENT_AGREEMENT'
  | 'LAND_PURCHASE'
  | 'PRENUPTIAL'
  | 'EMPLOYMENT_CONTRACT'
  | 'GENERAL_CONTRACT'
  | 'NDA'
  | 'POWER_OF_ATTORNEY'
  | 'AFFIDAVIT'
  | 'OTHER'

export interface DocumentTypeOption {
  id: DocumentTypeId
  label: string
  description: string
  icon: LucideIcon
  partyALabel: string
  partyBLabel: string
}

export const DOCUMENT_TYPES: DocumentTypeOption[] = [
  {
    id: 'DEMAND_LETTER',
    label: 'Demand letter',
    description: 'Formal pre-action notice for tenancy, contract, or debt disputes',
    icon: ScrollText,
    partyALabel: 'Your full name',
    partyBLabel: 'Recipient / counterparty',
  },
  {
    id: 'RENT_AGREEMENT',
    label: 'Rent / lease agreement',
    description: 'Residential tenancy between landlord and tenant',
    icon: Home,
    partyALabel: 'Landlord name',
    partyBLabel: 'Tenant name',
  },
  {
    id: 'LAND_PURCHASE',
    label: 'Land purchase agreement',
    description: 'Sale of land or property with title and completion terms',
    icon: FileSignature,
    partyALabel: 'Vendor / seller',
    partyBLabel: 'Purchaser / buyer',
  },
  {
    id: 'PRENUPTIAL',
    label: 'Prenuptial agreement',
    description: 'Financial and property arrangements before marriage',
    icon: Heart,
    partyALabel: 'Party A (spouse-to-be)',
    partyBLabel: 'Party B (spouse-to-be)',
  },
  {
    id: 'EMPLOYMENT_CONTRACT',
    label: 'Employment contract',
    description: 'Terms between employer and employee',
    icon: Briefcase,
    partyALabel: 'Employer',
    partyBLabel: 'Employee',
  },
  {
    id: 'GENERAL_CONTRACT',
    label: 'Contract agreement',
    description: 'General commercial or services contract',
    icon: Handshake,
    partyALabel: 'Party A',
    partyBLabel: 'Party B',
  },
  {
    id: 'NDA',
    label: 'Non-disclosure agreement',
    description: 'Protect confidential information between parties',
    icon: Shield,
    partyALabel: 'Disclosing party',
    partyBLabel: 'Receiving party',
  },
  {
    id: 'POWER_OF_ATTORNEY',
    label: 'Power of attorney',
    description: 'Authorize someone to act on your behalf',
    icon: FileText,
    partyALabel: 'Principal (grantor)',
    partyBLabel: 'Attorney-in-fact (agent)',
  },
  {
    id: 'AFFIDAVIT',
    label: 'Affidavit',
    description: 'Sworn written statement of facts',
    icon: FileText,
    partyALabel: 'Deponent name',
    partyBLabel: 'Optional second party',
  },
  {
    id: 'OTHER',
    label: 'Other legal document',
    description: 'Describe any other agreement or legal form you need',
    icon: FileText,
    partyALabel: 'Party A',
    partyBLabel: 'Party B',
  },
]
