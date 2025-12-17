import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../api/axios'
import { GitBranch, ExternalLink, Loader } from 'lucide-react'
import { motion } from 'framer-motion'

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

  const selectRepository = (repo: Repository) => {
    localStorage.setItem('selectedRepoId', repo.id.toString())
    localStorage.setItem('selectedRepoName', repo.name)
    localStorage.setItem('selectedRepoOwner', repo.owner)
    navigate('/app')
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-slate-900 flex items-center justify-center">
        <div className="text-center">
          <Loader className="w-12 h-12 text-brand-400 animate-spin mx-auto mb-4" />
          <p className="text-slate-300">Loading repositories...</p>
        </div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="min-h-screen bg-slate-900 flex items-center justify-center px-6">
        <div className="max-w-md w-full bg-slate-800 rounded-xl p-8 border border-red-500">
          <h2 className="text-xl font-bold text-white mb-4">Error</h2>
          <p className="text-slate-300 mb-6">{error}</p>
          <button
            onClick={fetchRepositories}
            className="w-full px-4 py-2 bg-brand-600 text-white rounded-lg hover:bg-brand-700 transition"
          >
            Retry
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-blue-900 to-slate-900 px-6 py-12">
      <div className="max-w-6xl mx-auto">
        <div className="text-center mb-12">
          <h1 className="text-4xl font-bold text-white mb-4">
            Select a Repository
          </h1>
          <p className="text-xl text-slate-300">
            Choose a repository to analyze and chat with
          </p>
        </div>

        <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
          {repositories.map((repo, index) => (
            <motion.div
              key={repo.id}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.3, delay: index * 0.05 }}
              onClick={() => selectRepository(repo)}
              className="bg-slate-800 rounded-xl p-6 border border-slate-700 hover:border-brand-500 hover:scale-105 transition-transform cursor-pointer"
            >
              <div className="flex items-start justify-between mb-4">
                <GitBranch className="w-8 h-8 text-brand-400" />
                <a
                  href={repo.url}
                  target="_blank"
                  rel="noopener noreferrer"
                  onClick={(e) => e.stopPropagation()}
                  className="text-slate-400 hover:text-brand-400 transition"
                >
                  <ExternalLink className="w-5 h-5" />
                </a>
              </div>
              
              <h3 className="text-xl font-semibold text-white mb-2">
                {repo.name}
              </h3>
              
              <p className="text-slate-400 text-sm">
                {repo.owner}
              </p>
            </motion.div>
          ))}
        </div>

        {repositories.length === 0 && (
          <div className="text-center py-12">
            <p className="text-slate-400 text-lg">
              No repositories found. Make sure you have access to GitHub repositories.
            </p>
          </div>
        )}
      </div>
    </div>
  )
}
