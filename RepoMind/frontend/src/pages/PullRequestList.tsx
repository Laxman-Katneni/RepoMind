import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../api/axios'
import { Shield, GitPullRequest, ExternalLink, Loader, Play, FileText } from 'lucide-react'
import { motion } from 'framer-motion'

interface PullRequest {
  id: number
  number: number
  title: string
  author: string
  htmlUrl: string
  baseBranch: string
  headBranch: string
}

export default function PullRequestList() {
  const navigate = useNavigate()
  const [pullRequests, setPullRequests] = useState<PullRequest[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [repoName, setRepoName] = useState('')
  const [runningReview, setRunningReview] = useState<number | null>(null)

  useEffect(() => {
    const selectedRepoId = localStorage.getItem('selectedRepoId')
    const selectedRepoName = localStorage.getItem('selectedRepoName')

    if (!selectedRepoId) {
      navigate('/app/select-repo')
      return
    }

    setRepoName(selectedRepoName || 'Repository')
    fetchPullRequests(selectedRepoId)
  }, [navigate])

  const fetchPullRequests = async (repoId: string) => {
    try {
      setLoading(true)
      setError(null)
      const response = await api.get(`/api/repos/${repoId}/pull-requests`)
      setPullRequests(response.data)
    } catch (err: any) {
      console.error('Error fetching pull requests:', err)
      const errorMessage = err.response?.status === 401 
        ? 'Authentication required. Please login with GitHub.'
        : err.response?.data?.message || 'Failed to load pull requests.'
      setError(errorMessage)
    } finally {
      setLoading(false)
    }
  }

  const runAIReview = async (prId: number, prNumber: number) => {
    try {
      setRunningReview(prId)
      const response = await api.post(`/api/reviews/run/${prId}`)
      console.log('Review response:', response.data)
      alert(`AI Review for PR #${prNumber} completed! Check the results.`)
    } catch (err: any) {
      console.error('Review error:', err)
      const errorMsg = err.response?.data?.message || 'Failed to run AI review'
      alert(`Review failed: ${errorMsg}`)
    } finally {
      setRunningReview(null)
    }
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <Loader className="w-12 h-12 text-gray-900 animate-spin mx-auto mb-4" />
          <p className="text-gray-600 font-medium">Loading pull requests...</p>
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
            onClick={() => navigate('/app')}
            className="w-full px-4 py-3 bg-gray-900 text-white rounded-xl hover:bg-gray-800 transition shadow-sm font-semibold"
          >
            Back to Dashboard
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gray-50 px-6 py-12">
      <div className="max-w-6xl mx-auto">
        {/* Header */}
        <div className="flex items-center justify-between mb-8">
          <div>
            <h1 className="text-5xl font-bold text-gray-900 mb-2 tracking-tight">Pull Requests</h1>
            <p className="text-xl text-gray-600">{repoName}</p>
          </div>
          <button
            onClick={() => navigate('/app')}
            className="px-6 py-2.5 bg-gray-900 text-white rounded-lg hover:bg-gray-800 transition shadow-sm font-medium"
          >
            Back to Dashboard
          </button>
        </div>

        {/* PR List */}
        {pullRequests.length === 0 ? (
          <div className="bg-gray-900 rounded-2xl p-12 border border-gray-800 text-center shadow-sm">
            <GitPullRequest className="w-16 h-16 text-gray-500 mx-auto mb-4" />
            <h3 className="text-xl font-semibold text-white mb-2">No Pull Requests Found</h3>
            <p className="text-gray-400 mb-6">
              Click "Sync Data" on the Dashboard to fetch pull requests from GitHub.
            </p>
            <button
              onClick={() => navigate('/app')}
              className="px-6 py-3 bg-gray-800 text-white rounded-xl hover:bg-gray-700 transition font-semibold"
            >
              Go to Dashboard
            </button>
          </div>
        ) : (
          <div className="space-y-6">
            {pullRequests.map((pr, index) => (
              <motion.div
                key={pr.id}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: index * 0.05, duration: 0.6, ease: "easeOut" }}
                className="bg-gray-900 rounded-3xl p-8 border border-gray-800 hover:border-gray-700 transition-all duration-500 ease-out shadow-md hover:shadow-2xl hover:-translate-y-1"
              >
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <div className="flex items-center gap-3 mb-3">
                      <Shield className="w-6 h-6 text-white" />
                      <h3 className="text-xl font-semibold text-white">
                        #{pr.number} {pr.title}
                      </h3>
                    </div>
                    <div className="flex items-center gap-4 text-sm text-gray-400 mb-3">
                      <span>by {pr.author}</span>
                      <span>•</span>
                      <span>{pr.baseBranch} ← {pr.headBranch}</span>
                    </div>
                  </div>

                  <div className="flex items-center gap-3 ml-4">
                    <a
                      href={pr.htmlUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="p-2 text-gray-400 hover:text-white transition-colors duration-400"
                      title="View on GitHub"
                    >
                      <ExternalLink size={20} />
                    </a>
                    <button
                      onClick={() => navigate(`/app/pull-requests/${pr.id}/review`)}
                      className="flex items-center gap-2 px-5 py-2.5 bg-blue-600 text-white rounded-xl hover:bg-blue-700 transition-all duration-400 font-medium shadow-sm hover:shadow-md"
                    >
                      <FileText className="w-4 h-4" />
                      View Report
                    </button>
                    <button
                      onClick={() => runAIReview(pr.id, pr.number)}
                      disabled={runningReview === pr.id}
                      className="flex items-center gap-2 px-5 py-2.5 bg-green-600 text-white rounded-xl hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed transition-all duration-400 font-medium shadow-sm hover:shadow-md"
                    >
                      {runningReview === pr.id ? (
                        <>
                          <Loader className="w-4 h-4 animate-spin" />
                          Running...
                        </>
                      ) : (
                        <>
                          <Play className="w-4 h-4" />
                          Run AI Review
                        </>
                      )}
                    </button>
                  </div>
                </div>
              </motion.div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
