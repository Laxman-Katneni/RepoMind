import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import api from '../api/axios'
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts'
import { Shield, GitPullRequest, MessageSquare, AlertTriangle, CheckCircle, Menu, Loader, RefreshCw, Bug, Clock, BarChart as BarChartIcon, LogOut, User, ChevronDown } from 'lucide-react'
import { motion } from 'framer-motion'

interface DashboardMetrics {
  totalReviews: number
  criticalIssuesCount: number
  averageReviewTime: number
  reviewsPerDay: { [key: string]: number }
}

export default function Dashboard() {
  const navigate = useNavigate()
  const [sidebarOpen, setSidebarOpen] = useState(true)
  const [metrics, setMetrics] = useState<DashboardMetrics | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [repoName, setRepoName] = useState('')
  const [syncing, setSyncing] = useState(false)
  const [indexing, setIndexing] = useState(false)
  
  // Initialize auditing state by checking localStorage immediately
  const getInitialAuditingState = () => {
    const selectedRepoId = localStorage.getItem('selectedRepoId')
    if (selectedRepoId) {
      const runningAuditId = localStorage.getItem(`runningAudit_${selectedRepoId}`)
      return !!runningAuditId // true if there's a running audit
    }
    return false
  }
  
  const [auditing, setAuditing] = useState(getInitialAuditingState())
  const [auditId, setAuditId] = useState<number | null>(null)
  const [auditStatus, setAuditStatus] = useState<any>(null)
  const [showAuditModal, setShowAuditModal] = useState(false)
  const [showUserMenu, setShowUserMenu] = useState(false)
  const [latestAudit, setLatestAudit] = useState<any>(null)
  const [loadingAudit, setLoadingAudit] = useState(true)

  useEffect(() => {
    const selectedRepoId = localStorage.getItem('selectedRepoId')
    const selectedRepoName = localStorage.getItem('selectedRepoName')

    if (!selectedRepoId) {
      navigate('/app/select-repo')
      return
    }

    setRepoName(selectedRepoName || 'Repository')
    fetchMetrics(selectedRepoId)
    fetchLatestAudit(selectedRepoId)
    checkRunningAudit(selectedRepoId)
  }, [navigate])

  const fetchMetrics = async (repoId: string) => {
    try {
      setLoading(true)
      setError(null)
      const response = await api.get(`/api/repos/${repoId}/metrics`)
      setMetrics(response.data)
    } catch (err: any) {
      console.error('Error fetching metrics:', err)
      
      // Set error state instead of redirecting
      const errorMessage = err.response?.status === 401 
        ? 'Authentication required. Please login with GitHub.'
        : err.response?.data?.message || 'Failed to load metrics. Please check your connection.'
      
      setError(errorMessage)
      setMetrics(null)
    } finally {
      setLoading(false)
    }
  }

  const fetchLatestAudit = async (repoId: string) => {
    try {
      setLoadingAudit(true)
      const response = await api.get(`/api/audits/latest/${repoId}`)
      setLatestAudit(response.data)
    } catch (err: any) {
      // 404 is expected if no audits exist
      if (err.response?.status !== 404) {
        console.error('Failed to fetch latest audit:', err)
      }
      setLatestAudit(null)
    } finally {
      setLoadingAudit(false)
    }
  }

  const checkRunningAudit = async (repoId: string) => {
    // Check if there's a running audit in localStorage
    const runningAuditId = localStorage.getItem(`runningAudit_${repoId}`)
    if (runningAuditId) {
      // Immediately set auditing to true to disable button while we verify
      setAuditing(true)
      setAuditId(parseInt(runningAuditId))
      
      try {
        const response = await api.get(`/api/audits/${runningAuditId}/status`)
        const status = response.data.status
        
        if (status === 'IN_PROGRESS' || status === 'PENDING') {
          // Keep auditing state as true
          console.log('Found running audit:', runningAuditId, 'Status:', status)
        } else {
          // Audit completed, clear the flag and re-enable button
          console.log('Audit completed, clearing flag:', runningAuditId, 'Status:', status)
          localStorage.removeItem(`runningAudit_${repoId}`)
          setAuditing(false)
          // Refresh latest audit to show the View Results button
          fetchLatestAudit(repoId)
        }
      } catch (error) {
        // Audit not found or error, clear the flag
        console.error('Error checking audit status:', error)
        localStorage.removeItem(`runningAudit_${repoId}`)
        setAuditing(false)
      }
    }
  }

  const syncData = async () => {
    const selectedRepoId = localStorage.getItem('selectedRepoId')
    if (!selectedRepoId) return

    try {
      setSyncing(true)
      const response = await api.post(`/api/repos/${selectedRepoId}/sync`)
      console.log('Sync response:', response.data)
      
      // Reload metrics after sync
      await fetchMetrics(selectedRepoId)
      
      toast.success(`Successfully synced ${response.data.count} pull requests!`)
    } catch (err: any) {
      console.error('Sync error:', err)
      const errorMsg = err.response?.data?.message || 'Failed to sync data'
      toast.error(`Sync failed: ${errorMsg}`)
    } finally {
      setSyncing(false)
    }
  }

  const indexRepository = async () => {
    const selectedRepoId = localStorage.getItem('selectedRepoId')
    if (!selectedRepoId) return

    try {
      setIndexing(true)
      setError(null)
      const response = await api.post(`/api/repos/${selectedRepoId}/index`)
      
      toast.success(response.data.message || 'Repository indexed successfully! You can now chat about the code.')
    } catch (err: any) {
      console.error('Error indexing repository:', err)
      const errorMsg = err.response?.data?.message || 'Failed to index repository'
      setError(errorMsg)
      toast.error('Error: ' + errorMsg)
    } finally {
      setIndexing(false)
    }
  }

  const startAudit = async () => {
    const selectedRepoId = localStorage.getItem('selectedRepoId')
    if (!selectedRepoId) return

    try {
      setAuditing(true)
      setError(null)
      const response = await api.post(`/api/audits/start/${selectedRepoId}`)
      
      const newAuditId = response.data.auditId
      setAuditId(newAuditId)
      setShowAuditModal(true)
      
      // Store running audit ID in localStorage to persist disabled state across navigation
      localStorage.setItem(`runningAudit_${selectedRepoId}`, newAuditId.toString())
      
      // Show toast notification
      toast.loading('🔍 Code audit started...', {
        id: 'audit-progress',
        duration: Infinity,
      })
      
      // Start polling for status
      pollAuditStatus(newAuditId)
    } catch (err: any) {
      console.error('Error starting audit:', err)
      const errorMsg = err.response?.data?.message || 'Failed to start audit'
      setError(errorMsg)
      toast.error('Error: ' + errorMsg)
      setAuditing(false)
    }
  }

  const pollAuditStatus = async (id: number) => {
    try {
      const response = await api.get(`/api/audits/${id}/status`)
      setAuditStatus(response.data)
      
      // Continue polling if not complete
      if (response.data.status === 'IN_PROGRESS' || response.data.status === 'QUEUED') {
        setTimeout(() => pollAuditStatus(id), 2000) // Poll every 2 seconds
      } else {
        setAuditing(false)
        
        // Dismiss loading toast
        toast.dismiss('audit-progress')
        
        // Show completion toast
        if (response.data.status === 'COMPLETED') {
          const totalIssues = (response.data.criticalCount || 0) + (response.data.warningCount || 0) + (response.data.infoCount || 0)
          toast.success(
            `✅ Audit complete! Found ${totalIssues} issue${totalIssues !== 1 ? 's' : ''} (${response.data.criticalCount || 0} critical, ${response.data.warningCount || 0} warnings)`,
            {
              action: {
                label: 'View Results',
                onClick: () => navigate(`/app/audit-results/${id}`)
              },
              duration: 10000,
            }
          )
        } else if (response.data.status === 'FAILED') {
          toast.error('❌ Audit failed: ' + (response.data.errorMessage || 'Unknown error'))
        }
      }
    } catch (err: any) {
      console.error('Error polling audit status:', err)
      setAuditing(false)
      toast.dismiss('audit-progress')
      toast.error('Failed to get audit status')
    }
  }

  const chartData = metrics?.reviewsPerDay
    ? Object.entries(metrics.reviewsPerDay).map(([name, reviews]) => ({ name, reviews }))
    : []

  return (
    <div className="min-h-screen bg-gray-50 flex">
      {/* Sidebar */}
      <motion.aside
        initial={{ x: -300 }}
        animate={{ x: sidebarOpen ? 0 : -300 }}
        className="w-64 bg-white border-r border-gray-200 p-6 fixed h-full shadow-sm"
      >
        <div className="flex items-center space-x-2 mb-8">
          <Shield className="w-8 h-8 text-gray-900" />
          <span className="text-xl font-bold text-gray-900">Repo Mind</span>
        </div>

        <nav className="space-y-2">
          <NavItem icon={BarChart} label="Dashboard" to="/app" active />
          <NavItem icon={GitPullRequest} label="Pull Requests" to="/app/pull-requests" />
          <NavItem icon={MessageSquare} label="Chat" to="/app/chat" />
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
      <div className={`flex-1 ${sidebarOpen ? 'ml-64' : 'ml-0'} transition-all`}>
        {/* Top Bar */}
        <header className="bg-white border-b border-gray-200 px-6 py-4 shadow-sm">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-4">
              <button
                onClick={() => setSidebarOpen(!sidebarOpen)}
                className="text-gray-400 hover:text-gray-900"
              >
                <Menu className="w-6 h-6" />
              </button>
              <h1 className="text-2xl font-bold text-gray-900">{repoName}</h1>
            </div>
            <div className="flex items-center gap-3">
              <button
                onClick={startAudit}
                disabled={auditing}
                className="flex items-center gap-2 px-4 py-2 bg-gray-900 text-white rounded-lg hover:bg-gray-800 disabled:opacity-50 disabled:cursor-not-allowed shadow-sm font-medium"
                title="Run AI code audit"
              >
                <Bug className={`w-4 h-4 ${auditing ? 'animate-pulse' : ''}`} />
                {auditing ? 'Auditing...' : 'Audit Code'}
              </button>
              
              {!loadingAudit && latestAudit && (
                <button
                  onClick={() => navigate(`/app/audit-results/${latestAudit.auditId}`)}
                  className="flex items-center gap-2 px-4 py-2 bg-gray-900 text-white rounded-lg hover:bg-gray-800 shadow-sm font-medium"
                  title="View latest audit results"
                >
                  <AlertTriangle className="w-4 h-4" />
                  View Audit Results
                </button>
              )}
              
              <button
                onClick={indexRepository}
                disabled={indexing}
                className="flex items-center gap-2 px-4 py-2 bg-gray-900 text-white rounded-lg hover:bg-gray-800 disabled:opacity-50 disabled:cursor-not-allowed shadow-sm font-medium"
                title="Index repository code for AI chat"
              >
                <Shield className={`w-4 h-4 ${indexing ? 'animate-spin' : ''}`} />
                {indexing ? 'Indexing...' : 'Index for Chat'}
              </button>
              <button
                onClick={syncData}
                disabled={syncing}
                className="flex items-center gap-2 px-4 py-2 bg-gray-900 text-white rounded-lg hover:bg-gray-800 disabled:opacity-50 disabled:cursor-not-allowed shadow-sm font-medium"
              >
                <RefreshCw className={`w-4 h-4 ${syncing ? 'animate-spin' : ''}`} />
                {syncing ? 'Syncing...' : 'Sync Data'}
              </button>
              
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
          </div>
        </header>

        {/* Content */}
        <main className="p-6">
          {loading ? (
            <div className="flex items-center justify-center py-20">
              <Loader className="w-12 h-12 text-brand-500 animate-spin" />
            </div>
          ) : error ? (
            <div className="flex items-center justify-center py-20">
              <div className="max-w-md w-full bg-white border border-red-200 rounded-2xl p-8 shadow-lg">
                <div className="flex items-center gap-3 mb-4">
                  <AlertTriangle className="w-8 h-8 text-red-500" />
                  <h2 className="text-xl font-bold text-gray-900">Connection Error</h2>
                </div>
                <p className="text-gray-600 mb-6">{error}</p>
                <button
                  onClick={() => {
                    const selectedRepoId = localStorage.getItem('selectedRepoId')
                    if (selectedRepoId) {
                      fetchMetrics(selectedRepoId)
                    }
                  }}
                  className="w-full px-4 py-3 bg-gray-900 text-white rounded-lg hover:bg-gray-800 shadow-sm font-semibold"
                >
                  Retry Connection
                </button>
              </div>
            </div>
          ) : metrics ? (
            <>
              {/* Stats Grid */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-10">
              <MetricCard
                title="Critical Issues"
                value={metrics.criticalIssuesCount.toString()}
                change="-3"
                icon={AlertTriangle}
                color="red"
              />
              <MetricCard
                title="PRs Reviewed"
                value={metrics.totalReviews.toString()}
                change="+8"
                icon={GitPullRequest}
                color="blue"
              />
              <MetricCard
                title="Auto-Approved"
                value={Math.floor(metrics.totalReviews * 0.66).toString()}
                change="+5"
                icon={CheckCircle}
                color="green"
              />
              <MetricCard
                title="Avg Review Time"
                value={`${metrics.averageReviewTime}s`}
                change="-0.8s"
                icon={Clock}
                color="purple"
              />
            </div>

              {/* Chart */}
              <div className="bg-gray-900 rounded-3xl p-8 border border-gray-800 shadow-sm hover:shadow-xl transition-shadow duration-500 ease-out">
                <h2 className="text-2xl font-semibold text-white mb-8">Review Activity</h2>
                {chartData.length > 0 ? (
                  <ResponsiveContainer width="100%" height={300}>
                    <BarChart data={chartData}>
                      <CartesianGrid strokeDasharray="3 3" stroke="#374151" />
                      <XAxis dataKey="name" stroke="#9ca3af" />
                      <YAxis stroke="#9ca3af" />
                      <Tooltip
                        contentStyle={{ backgroundColor: '#1f2937', border: '1px solid #374151', borderRadius: '12px' }}
                        labelStyle={{ color: '#fff' }}
                      />
                      <Bar dataKey="reviews" fill="#0ea5e9" radius={[8, 8, 0, 0]} />
                    </BarChart>
                  </ResponsiveContainer>
                ) : (
                  <p className="text-gray-400 text-center py-12">No review data available yet</p>
                )}
              </div>
            </>
          ) : (
            <div className="text-center py-20">
              <p className="text-slate-400">No metrics available</p>
            </div>
          )}
        </main>

        {/* Audit Progress Modal */}
        {showAuditModal && auditStatus && (
          <div className="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center z-50 p-4">
            <motion.div
              initial={{ scale: 0.95, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              className="bg-white rounded-3xl border border-gray-200 max-w-4xl w-full max-h-[90vh] overflow-hidden shadow-2xl"
            >
              {/* Header */}
              <div className="p-6 border-b border-gray-200 bg-gray-50">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <Bug className="w-8 h-8 text-gray-900" />
                    <h2 className="text-2xl font-bold text-gray-900">Code Audit</h2>
                  </div>
                  <button
                    onClick={() => setShowAuditModal(false)}
                    className="text-gray-400 hover:text-gray-900 transition"
                  >
                    <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                    </svg>
                  </button>
                </div>
              </div>

              {/* Content */}
              <div className="p-6 overflow-y-auto max-h-[calc(90vh-200px)]">
                {/* Status Badge */}
                <div className="mb-6">
                  <span className={`inline-flex items-center gap-2 px-4 py-2 rounded-full text-sm font-semibold ${
                    auditStatus.status === 'COMPLETED' ? 'bg-green-100 text-green-700 border border-green-200' :
                    auditStatus.status === 'FAILED' ? 'bg-red-100 text-red-700 border border-red-200' :
                    'bg-blue-100 text-blue-700 border border-blue-200'
                  }`}>
                    {auditStatus.status === 'IN_PROGRESS' && <Loader className="w-4 h-4 animate-spin" />}
                    {auditStatus.status}
                  </span>
                </div>

                {/* Progress Bar */}
                {(auditStatus.status === 'IN_PROGRESS' || auditStatus.status === 'QUEUED') && (
                  <div className="mb-6">
                    <div className="flex items-center justify-between mb-2">
                      <span className="text-sm text-gray-600 font-medium">Progress</span>
                      <span className="text-sm font-semibold text-gray-900">{auditStatus.progressPercentage || 0}%</span>
                    </div>
                    <div className="w-full bg-gray-200 rounded-full h-2.5 overflow-hidden">
                      <motion.div
                        className="h-full bg-gray-900"
                        initial={{ width: 0 }}
                        animate={{ width: `${auditStatus.progressPercentage || 0}%` }}
                        transition={{ duration: 0.5 }}
                      />
                    </div>
                    {auditStatus.currentFile && (
                      <div className="mt-3 space-y-2">
                        <p className="text-sm text-gray-600">
                          Analyzing: <span className="text-gray-900 font-mono font-medium">{auditStatus.currentFile}</span>
                        </p>
                        {/* Helpful waiting message */}
                        <div className="flex items-start gap-2 bg-blue-50 border border-blue-200 rounded-xl p-3">
                          <motion.div
                            animate={{ opacity: [0.5, 1, 0.5] }}
                            transition={{ duration: 2, repeat: Infinity }}
                            className="flex-shrink-0"
                          >
                            <Loader className="w-4 h-4 text-blue-600" />
                          </motion.div>
                          <div className="text-xs text-blue-900">
                            <p className="font-semibold mb-1">AI analysis in progress...</p>
                            <p className="text-blue-700">Each file may take 30-60 seconds to analyze. This is normal for AI-powered code auditing.</p>
                          </div>
                        </div>
                      </div>
                    )}
                  </div>
                )}

                {/* Statistics */}
                <div className="grid grid-cols-3 gap-4 mb-6">
                  <div className="bg-gray-50 rounded-xl p-4 border border-gray-200">
                    <div className="text-3xl font-bold text-red-600">{auditStatus.criticalCount || 0}</div>
                    <div className="text-sm text-gray-600 font-medium">Critical</div>
                  </div>
                  <div className="bg-gray-50 rounded-xl p-4 border border-gray-200">
                    <div className="text-3xl font-bold text-yellow-600">{auditStatus.warningCount || 0}</div>
                    <div className="text-sm text-gray-600 font-medium">Warnings</div>
                  </div>
                  <div className="bg-gray-50 rounded-xl p-4 border border-gray-200">
                    <div className="text-3xl font-bold text-blue-600">{auditStatus.infoCount || 0}</div>
                    <div className="text-sm text-gray-600 font-medium">Info</div>
                  </div>
                </div>

                {/* Files Scanned */}
                {auditStatus.totalFilesScanned > 0 && (
                  <div className="bg-gray-50 rounded-xl p-4 border border-gray-200 mb-6">
                    <div className="flex items-center justify-between">
                      <span className="text-gray-600">Files Scanned</span>
                      <span className="text-gray-900 font-semibold">{auditStatus.totalFilesScanned}</span>
                    </div>
                    <div className="flex items-center justify-between mt-2">
                      <span className="text-gray-600">Files with Issues</span>
                      <span className="text-gray-900 font-semibold">{auditStatus.filesWithIssues || 0}</span>
                    </div>
                  </div>
                )}

                {/* Error Message */}
                {auditStatus.errorMessage && (
                  <div className="bg-red-50 border border-red-200 rounded-xl p-4 mb-6">
                    <div className="flex items-start gap-3">
                      <AlertTriangle className="w-5 h-5 text-red-600 flex-shrink-0 mt-0.5" />
                      <div>
                        <h4 className="font-semibold text-red-900 mb-1">Error</h4>
                        <p className="text-sm text-red-700">{auditStatus.errorMessage}</p>
                      </div>
                    </div>
                  </div>
                )}

                {/* View Results Button */}
                {auditStatus.status === 'COMPLETED' && (
                  <button
                    onClick={() => {
                      // Navigate to results page with audit ID
                      navigate(`/app/audit-results/${auditId}`)
                    }}
                    className="w-full px-6 py-4 bg-gray-900 text-white rounded-xl hover:bg-gray-800 shadow-sm font-semibold text-lg"
                  >
                    View Detailed Results →
                  </button>
                )}
              </div>
            </motion.div>
          </div>
        )}
      </div>
    </div>

  )
}


function NavItem({ icon: Icon, label, active = false, to }: any) {
  const navigate = useNavigate()
  
  return (
    <button
      onClick={() => to && navigate(to)}
      className={`w-full flex items-center gap-3 px-4 py-2.5 rounded-lg font-medium ${
        active
          ? 'bg-gray-900 text-white shadow-sm'
          : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900'
      }`}
    >
      <Icon className="w-5 h-5" />
      <span>{label}</span>
    </button>
  )
}

function MetricCard({ title, value, change, icon: Icon, color }: any) {
  const colors = {
    red: 'text-red-400',
    blue: 'text-brand-400',
    green: 'text-green-400',
    purple: 'text-purple-400'
  }

  return (
    <div className="bg-gray-900 rounded-3xl p-8 border border-gray-800 shadow-md hover:shadow-2xl hover:-translate-y-1 transition-all duration-500 ease-out cursor-pointer group">
      <div className="flex items-center justify-between mb-5">
        <Icon className={`w-10 h-10 ${colors[color as keyof typeof colors]} group-hover:scale-110 transition-transform duration-500 ease-out`} />
        <span className="text-sm text-gray-400 font-medium">{change}</span>
      </div>
      <h3 className="text-4xl font-bold text-white mb-2">{value}</h3>
      <p className="text-gray-400 text-base">{title}</p>
    </div>
  )
}
