import { Link } from 'react-router-dom'

export default function AboutPage() {
  return (
    <div className="max-w-3xl mx-auto px-4 py-12">
      <h1 className="font-display text-3xl mb-6">About Legally</h1>

      <div className="prose prose-legally space-y-6 text-legally-navy/80 text-sm leading-relaxed">
        <p>
          <strong>Legally</strong> improves access to justice for people who cannot afford private
          lawyers — anywhere in the world. The platform is designed to scale across countries with
          national, federal, and state-level law where data is available.
        </p>

        <h2 className="font-display text-xl text-legally-navy">Google tools</h2>
        <ul className="list-disc pl-5 space-y-1">
          <li>
            <strong>Gemini API</strong> — multimodal analysis of text, images, PDFs, audio, and video
          </li>
          <li>
            <strong>PostgreSQL</strong> — consultation history, users, and upload metadata (Cloud SQL in your Firebase/GCP project)
          </li>
          <li>
            <strong>Firebase Anonymous Auth</strong> — secure sessions without signup or login forms
          </li>
          <li>
            <strong>Firebase Storage</strong> — evidence file uploads
          </li>
        </ul>

        <h2 className="font-display text-xl text-legally-navy">Security</h2>
        <p>
          The app signs you in anonymously in the background. Your Firebase ID token is sent with each API
          request so only your session can read your consultation history.
        </p>

        <h2 className="font-display text-xl text-legally-navy">Legal corpus</h2>
        <p>
          Answers are grounded in a curated legal corpus (constitutions, federal acts, and state or
          regional guidance). The app automatically detects your country and state from your device
          location and applies laws for that jurisdiction. If you mention a different country or state
          in your message, voice recording, or uploaded documents (any country), that location
          overrides device detection. The MVP includes Nigerian federal and
          state sources plus international principles; additional country corpora can be added.
          Citations reference the corpus — not
          invented law.
        </p>

        <h2 className="font-display text-xl text-legally-navy">Limitations</h2>
        <p>
          Legally does not replace a licensed lawyer. Laws change; outcomes depend on facts. Contact
          numbers are curated from public sources only. Always verify critical steps with qualified
          counsel.
        </p>

        <p>
          <Link to="/consult" className="text-legally-gold font-semibold hover:underline">
            Start a consultation →
          </Link>
        </p>
      </div>
    </div>
  )
}
