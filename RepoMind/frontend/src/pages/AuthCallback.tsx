import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'

export default function AuthCallback() {
  const navigate = useNavigate()

  useEffect(() => {
    // Extract JWT from URL fragment (#token=...)
    const hash = window.location.hash.substring(1) // Remove #
    const params = new URLSearchParams(hash)
    const token = params.get('token')

    if (token) {
      // Store JWT in localStorage
      localStorage.setItem('jwt_token', token)
      
      // Redirect to repo selection
      navigate('/app/select-repo')
    } else {
      // No token found, redirect to login with error
      navigate('/login?error=oauth')
    }
  }, [navigate])

  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center">
      <div className="text-center">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-gray-900 mx-auto"></div>
        <p className="mt-4 text-gray-600">Completing authentication...</p>
      </div>
    </div>
  )
}
