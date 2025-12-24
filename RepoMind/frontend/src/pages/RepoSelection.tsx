import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../api/axios'
import { GitBranch, ExternalLink, Loader } from 'lucide-react'

interface Repository {
  id: number
  owner: string
  name: string
  url: string
}

export default function RepoSelection() {
  const navigate = useNavigate()
  const [repositories, setRepositories] = useState<Repository[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    fetchRepositories()
  }, [])

  const fetchRepositories = async () => {
    try {
      setLoading(true)
      const response = await api.get('/api/repos')
      setRepositories(response.data)
      setError(null)
    } catch (err: any) {
      console.error('Error fetching repositories:', err)
      
      if (err.response?.status === 401) {
        // Unauthorized - redirect to login
        navigate('/login')
        return
      }
      
      setError(err.response?.data?.message || 'Failed to fetch repositories')
    } finally {
      setLoading(false)
    }
  }

  const handleRepoSelect = (repo: Repository) => {
    console.log('Card Clicked:', repo.name)
    
    if (!repo.id) {
      console.error('CRITICAL ERROR: Repo ID is missing', repo)
      alert(`Database Error: Repository '${repo.name}' has no ID. Check Backend logs.`)
      return
    }
    
    localStorage.setItem('selectedRepoId', repo.id.toString())
    localStorage.setItem('selectedRepoName', `${repo.owner}/${repo.name}`)
    console.log('Navigation to /app triggered')
    navigate('/app')
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <Loader className="w-12 h-12 text-gray-900 animate-spin mx-auto mb-4" />
          <p className="text-gray-600 font-medium">Loading repositories...</p>
        </div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center px-6">
        <div className="max-w-md w-full bg-white rounded-2xl p-8 border border-red-200 shadow-lg">
          <h2 className="text-xl font-bold text-gray-900 mb-4">Error</h2>
          <p className="text-gray-600 mb-6">{error}</p>
          <button
            onClick={fetchRepositories}
            className="w-full px-4 py-3 bg-gray-900 text-white rounded-xl hover:bg-gray-800 transition shadow-sm font-semibold"
          >
            Retry
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gray-50 px-6 py-12">
      <div className="max-w-6xl mx-auto">
        <div className="text-center mb-12">
          <h1 className="text-5xl font-bold text-gray-900 mb-4 tracking-tight">
            Select a Repository
          </h1>
          <p className="text-xl text-gray-600">
            Choose a repository to analyze and chat with
          </p>
        </div>

        <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
          {repositories.map((repo) => (
            <div
              key={repo.id}
              onClick={() => handleRepoSelect(repo)}
              className="bg-gray-900 p-8 rounded-3xl border border-gray-800 hover:border-gray-700 cursor-pointer transition-all duration-500 ease-out hover:shadow-2xl hover:-translate-y-1 shadow-md group relative"
            >
              {/* External Link */}
              <a
                href={repo.url}
                target="_blank"
                rel="noopener noreferrer"
                onClick={(e) => e.stopPropagation()}
                className="absolute top-5 right-5 text-gray-500 hover:text-white transition-colors duration-400"
              >
                <ExternalLink size={18} />
              </a>

              {/* Icon */}
              <div className="mb-5">
                <GitBranch className="w-10 h-10 text-white group-hover:scale-110 transition-transform duration-500 ease-out" />
              </div>

              {/* Repository Name */}
              <h3 className="text-2xl font-semibold text-white mb-3">
                {repo.name}
              </h3>

              {/* Owner */}
              <p className="text-gray-400 text-base font-medium">
                {repo.owner}
              </p>
            </div>
          ))}
        </div>

        {repositories.length === 0 && (
          <div className="text-center py-12">
            <p className="text-gray-500 text-lg">
              No repositories found. Make sure you have access to GitHub repositories.
            </p>
          </div>
        )}
      </div>
    </div>
  )
}
