export function phoneTelHref(phone: string): string {
  const digits = phone.replace(/\D/g, '')
  if (!digits) return ''
  if (digits.startsWith('234')) return `tel:+${digits}`
  if (digits.startsWith('0')) return `tel:+234${digits.slice(1)}`
  return `tel:+${digits}`
}

export function socialHref(platform: string, value: string): string {
  const trimmed = value.trim()
  if (trimmed.startsWith('http://') || trimmed.startsWith('https://')) {
    return trimmed
  }
  const handle = trimmed.replace(/^@/, '')
  const p = platform.toLowerCase()
  if (p.includes('x') || p.includes('twitter')) {
    return `https://x.com/${handle}`
  }
  if (p.includes('facebook')) {
    return `https://www.facebook.com/${encodeURIComponent(trimmed)}`
  }
  if (p.includes('instagram')) {
    return `https://www.instagram.com/${handle}`
  }
  if (p.includes('linkedin')) {
    return `https://www.linkedin.com/company/${handle}`
  }
  if (p.includes('website') || p.includes('web')) {
    return trimmed.startsWith('www.') ? `https://${trimmed}` : `https://${handle}`
  }
  return `https://${trimmed}`
}

export function isExternalUrl(value: string): boolean {
  return /^https?:\/\//i.test(value.trim())
}
