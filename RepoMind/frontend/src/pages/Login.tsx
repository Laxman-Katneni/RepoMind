import { Github } from 'lucide-react'

export default function Login() {
  const handleGitHubLogin = () => {
    // Redirect to backend OAuth endpoint
    // Backend will handle OAuth flow and redirect back to frontend with session cookie
    window.location.href = 'http://localhost:8080/oauth2/authorization/github'
  }

  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center px-6">
      <div className="max-w-md w-full">
        <div className="bg-white rounded-3xl p-10 shadow-2xl border border-gray-200">
          <div className="text-center mb-8">
            <div className="w-16 h-16 bg-gray-900 rounded-2xl flex items-center justify-center mx-auto mb-4">
              <Github className="w-8 h-8 text-white" />
            </div>
            <h1 className="text-3xl font-bold text-gray-900 mb-2">
              Welcome to Repo Mind
            </h1>
            <p className="text-gray-600">
              Sign in with GitHub to get started
            </p>
          </div>

          <button
            onClick={handleGitHubLogin}
            className="w-full px-6 py-4 bg-gray-900 text-white rounded-xl hover:bg-gray-800 transition flex items-center justify-center gap-3 shadow-lg hover:shadow-xl font-semibold"
          >
            <Github className="w-6 h-6" />
            <span className="text-lg">Continue with GitHub</span>
          </button>

          <p className="text-gray-500 text-sm text-center mt-6">
            We'll never post to GitHub without your permission
          </p>
        </div>
      </div>
    </div>
  )
}
