import { Github } from 'lucide-react'

export default function Login() {
  const handleGitHubLogin = () => {
    // Redirect to backend OAuth endpoint
    // Backend will handle OAuth flow and redirect back to frontend with session cookie
    window.location.href = 'http://localhost:8080/oauth2/authorization/github'
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-blue-900 to-slate-900 flex items-center justify-center px-6">
      <div className="max-w-md w-full">
        <div className="bg-slate-800 rounded-2xl p-8 shadow-2xl border border-slate-700">
          <h1 className="text-3xl font-bold text-white text-center mb-2">
            Welcome to Repo Mind
          </h1>
          <p className="text-slate-300 text-center mb-8">
            Sign in with GitHub to get started
          </p>

          <button
            onClick={handleGitHubLogin}
            className="w-full px-6 py-4 bg-slate-900 text-white rounded-lg hover:bg-slate-950 transition flex items-center justify-center gap-3 border border-slate-600 hover:border-brand-500"
          >
            <Github className="w-6 h-6" />
            <span className="text-lg font-semibold">Continue with GitHub</span>
          </button>

          <p className="text-slate-400 text-sm text-center mt-6">
            We'll never post to GitHub without your permission
          </p>
        </div>
      </div>
    </div>
  )
}
