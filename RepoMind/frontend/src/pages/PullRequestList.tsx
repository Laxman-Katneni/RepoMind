import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import api from '../api/axios'
import { Shield, GitPullRequest, ExternalLink, Loader, Play, FileText, Menu, BarChart, MessageSquare, LogOut, User, ChevronDown } from 'lucide-react'
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

const NavItem = ({ icon: Icon, label, to, active = false }: any) => {
  const navigate = useNavigate()
  
  return (
    <motion.button
      whileHover={{ x: 4 }}
      whileTap={{ scale: 0.98 }}
      transition={{ duration: 0.4, ease: "easeOut" }}
      onClick={() => navigate(to)}
      className={`w-full flex items-center gap-3 px-4 py-2.5 rounded-lg font-medium transition-colors duration-400 ${
        active
          ? 'bg-gray-900 text-white shadow-sm'
          : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900'
      }`}
    >
      <Icon className="w-5 h-5" />
      <span>{label}</span>
    </motion.button>
  )
}

export default function PullRequestList() {
  const navigate = useNavigate()
  const [pullRequests, setPullRequests] = useState<PullRequest[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [repoName, setRepoName] = useState('')
  const [runningReview, setRunningReview] = useState<number | null>(null)
  const [sidebarOpen, setSidebarOpen] = useState(true)
  const [showUserMenu, setShowUserMenu] = useState(false)

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
      toast.success(`AI Review for PR #${prNumber} completed!`, {
        description: 'Check the results by clicking View Report'
      })
    } catch (err: any) {
      console.error('Review error:', err)
      const errorMsg = err.response?.data?.message || 'Failed to run AI review'
      toast.error('Review failed', {
        description: errorMsg
      })
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
    <div className="min-h-screen bg-gray-50 flex">
      {/* Sidebar */}
      <motion.aside
        initial={{ x: 0 }}
        animate={{ x: sidebarOpen ? 0 : -256 }}
        transition={{ type: "spring", stiffness: 150, damping: 25 }}
        className="w-64 bg-white border-r border-gray-200 p-6 fixed h-full z-10 shadow-sm"
      >
        <div className="flex items-center gap-3 mb-8">
          <Shield className="w-8 h-8 text-gray-900" />
          <h2 className="text-xl font-bold text-gray-900">Repo Mind</h2>
        </div>

        <nav className="space-y-2">
          <NavItem icon={BarChart} label="Dashboard" to="/app" active={false} />
          <NavItem icon={GitPullRequest} label="Pull Requests" to="/app/pull-requests" active={true} />
          <NavItem icon={MessageSquare} label="Chat" to="/app/chat" active={false} />
        </nav>

        <div className="mt-8 pt-8 border-t border-gray-200">
          <p className="text-xs text-gray-500 mb-2 font-medium">Current Repository</p>
          <p className="text-sm text-gray-900 font-semibold truncate">{repoName}</p>
          <button
            onClick={() => navigate('/app/select-repo')}
            className="mt-2 text-xs text-gray-900 hover:text-gray-700 font-semibold underline underline-offset-2 transition-colors duration-400"
          >
            Change Repository
          </button>
        </div>
      </motion.aside>

      {/* Main Content */}
      <motion.div 
        className={`flex-1 ${sidebarOpen ? 'ml-64' : 'ml-0'}`}
        initial={false}
        animate={{ marginLeft: sidebarOpen ? 256 : 0 }}
        transition={{ type: "spring", stiffness: 150, damping: 25 }}
      >
        {/* Top Bar */}
        <header className="bg-white border-b border-gray-200 px-6 py-4 shadow-sm">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-4">
              <motion.button
                whileHover={{ scale: 1.05 }}
                whileTap={{ scale: 0.95 }}
                transition={{ duration: 0.4, ease: "easeOut" }}
                onClick={() => setSidebarOpen(!sidebarOpen)}
                className="text-gray-400 hover:text-gray-900 transition-colors duration-400"
              >
                <Menu className="w-6 h-6" />
              </motion.button>
              <div>
                <h1 className="text-2xl font-bold text-gray-900">Pull Requests</h1>
                <p className="text-sm text-gray-600">{repoName}</p>
              </div>
            </div>
            
            {/* User Menu */}
            <div className="relative">
              <button
                onClick={() => setShowUserMenu(!showUserMenu)}
                className="flex items-center gap-2 px-3 py-2 bg-gray-900 text-white rounded-lg hover:bg-gray-800 transition-colors duration-400 shadow-sm"
              >
                <User className="w-4 h-4" />
                <ChevronDown className="w-3 h-3" />
              </button>
              
              {showUserMenu && (
                <motion.div
                  initial={{ opacity: 0, y: -10 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ duration: 0.2 }}
                  className="absolute right-0 mt-2 w-48 bg-white rounded-xl shadow-xl border border-gray-200 py-2 z-50"
                >
                  <button
                    onClick={() => {
                      localStorage.clear()
                      sessionStorage.clear()
                      navigate('/')
                    }}
                    className="w-full flex items-center gap-3 px-4 py-2.5 text-gray-700 hover:bg-gray-100 transition-colors duration-200"
                  >
                    <LogOut className="w-4 h-4" />
                    <span className="font-medium">Logout</span>
                  </button>
                </motion.div>
              )}
            </div>
          </div>
        </header>

        <div className="px-6 py-12">
          <div className="max-w-6xl mx-auto">
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
      </motion.div>
    </div>
  )
}
