import { jsPDF } from 'jspdf'

export function downloadDocumentPdf(title: string, content: string, fileBaseName: string) {
  const doc = new jsPDF({ unit: 'pt', format: 'a4' })
  const margin = 48
  const pageWidth = doc.internal.pageSize.getWidth()
  const pageHeight = doc.internal.pageSize.getHeight()
  const maxWidth = pageWidth - margin * 2
  const lineHeight = 14
  let y = margin

  const addLines = (lines: string[], fontSize: number, bold = false) => {
    doc.setFont('helvetica', bold ? 'bold' : 'normal')
    doc.setFontSize(fontSize)
    for (const line of lines) {
      if (y > pageHeight - margin) {
        doc.addPage()
        y = margin
      }
      doc.text(line, margin, y)
      y += lineHeight + (fontSize > 10 ? 4 : 0)
    }
  }

  addLines(doc.splitTextToSize(title, maxWidth) as string[], 14, true)
  y += 8
  addLines(doc.splitTextToSize(content, maxWidth) as string[], 10)

  const safeName = fileBaseName.replace(/[^\w\-]+/g, '_').slice(0, 80)
  doc.save(`${safeName}.pdf`)
}
