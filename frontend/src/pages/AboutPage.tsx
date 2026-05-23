import { Link } from 'react-router-dom'

export default function AboutPage() {
  return (
    <div className="max-w-3xl mx-auto px-4 py-12">
      <h1 className="font-display text-3xl mb-6">About Legally</h1>

      <div className="prose prose-legally space-y-6 text-legally-navy/80 text-sm leading-relaxed">
        <p>
          <strong>Legally</strong> improves access to justice for people who cannot afford private
          lawyers anywhere in the world. The platform is designed to scale across countries with
          national, federal, and state-level law where data is available.
        </p>

        <h2 className="font-display text-xl text-legally-navy">What Legally is for</h2>
        <p>
          Imagine you are a tourist in a foreign country. You are about to sign a contract, pay a fine,
          or take an action on the street, and you are not sure whether it could land you in trouble or
          even jail. You do not have a local lawyer on speed dial. <strong>That is where Legally comes
          in.</strong> You describe what is happening, and Legally explains in plain English what the
          law in that place generally says, where it comes from, and what steps you can take next.
        </p>
        <p>
          Or picture this: you are stopped on the road by police, asked to unlock your phone, or
          pressured in a way that feels wrong. You do not have time to call a lawyer friend or relative.
          <strong> Legally is there in the moment.</strong> Type your situation, record a quick voice note,
          or upload a photo or video of what is going on. Legally helps you understand your legal stance
          and how to respond calmly and lawfully.
        </p>
        <p>
          The same idea applies at home: unfair rent hikes, shady land deals, workplace issues, or
          contracts you cannot afford to have a solicitor review. Legally lowers the barrier to knowing
          your rights when professional help is out of reach.
        </p>

        <h2 className="font-display text-xl text-legally-navy">Why use Legally</h2>
        <ul className="list-disc pl-5 space-y-2">
          <li>
            <strong>Available when lawyers are not</strong>: nights, weekends, roadside stops, or abroad.
          </li>
          <li>
            <strong>Plain language</strong>: no law degree required to understand the answer.
          </li>
          <li>
            <strong>Grounded citations</strong>: summaries tied to a curated legal corpus, not invented statutes.
          </li>
          <li>
            <strong>Works worldwide</strong>: your location is detected automatically; mention another country in your message or uploads to switch jurisdiction.
          </li>
          <li>
            <strong>Multimodal</strong>: text, voice, images, PDFs, and video evidence.
          </li>
          <li>
            <strong>Official contacts</strong>: curated public numbers and agencies when you need to escalate.
          </li>
          <li>
            <strong>Document drafting</strong>: generate agreements and letters for your jurisdiction, preview, and download as PDF.
          </li>
        </ul>

        <h2 className="font-display text-xl text-legally-navy">Features and how to use them</h2>

        <h3 className="font-semibold text-legally-navy mt-4">Legal consultation</h3>
        <p>
          Go to <Link to="/consult" className="text-legally-gold font-semibold hover:underline">Consult</Link>,
          pick a situation (police interaction, tenancy, land, employment, and more), and describe your case.
          Attach a lease, ID photo, voice memo, or video if helpful. Tap <strong>Get legal guidance</strong> to
          receive a summary, what the law says (with sources), practical steps, and contact cards. Allow
          location access so laws match where you are; say &quot;under Nigerian law&quot; (or any country) in your
          text to override.
        </p>

        <h3 className="font-semibold text-legally-navy mt-4">Draft legal documents</h3>
        <p>
          Open <Link to="/documents" className="text-legally-gold font-semibold hover:underline">Documents</Link>,
          choose a type (rent agreement, land purchase, prenup, employment contract, NDA, demand letter,
          etc.), describe the parties and terms, and generate a draft. Preview on screen, then{' '}
          <strong>Download PDF</strong> after you review. Have a lawyer sign off before you execute anything binding.
        </p>

        <h3 className="font-semibold text-legally-navy mt-4">Demand letters from a consultation</h3>
        <p>
          After a tenancy or contract-related consultation, if a demand letter is appropriate, use the
          button on your results to open a quick generator, preview the letter, and download it as PDF.
        </p>

        <h3 className="font-semibold text-legally-navy mt-4">Your history</h3>
        <p>
          Signed-in sessions (anonymous, in the background) can revisit recent consultations from the Consult page.
        </p>

        <h2 className="font-display text-xl text-legally-navy">Legal corpus</h2>
        <p>
          Answers are grounded in a curated legal corpus (constitutions, federal acts, and state or
          regional guidance). The app automatically detects your country and state from your device
          location and applies laws for that jurisdiction. If you mention a different country or state
          in your message, voice recording, or uploaded documents (any country), that location
          overrides device detection. Core coverage includes Nigerian federal and
          state sources plus international principles; additional country corpora can be added.
          Citations reference the corpus, not
          invented law.
        </p>

        <h2 className="font-display text-xl text-legally-navy">Limitations</h2>
        <p>
          Legally does not replace a licensed lawyer. Laws change; outcomes depend on facts. Contact
          numbers are curated from public sources only. Always verify critical steps with qualified
          counsel.
        </p>

        <p className="flex flex-wrap gap-4">
          <Link to="/consult" className="text-legally-gold font-semibold hover:underline">
            Start a consultation →
          </Link>
          <Link to="/documents" className="text-legally-gold font-semibold hover:underline">
            Draft a document →
          </Link>
        </p>
      </div>
    </div>
  )
}
