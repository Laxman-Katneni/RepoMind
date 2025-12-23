import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../api/axios'
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts'
import { Shield, GitPullRequest, MessageSquare, AlertTriangle, CheckCircle, Menu, Loader, RefreshCw, Bug } from 'lucide-react'
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
  const [auditing, setAuditing] = useState(false)
  const [auditId, setAuditId] = useState<number | null>(null)
  const [auditStatus, setAuditStatus] = useState<any>(null)
  const [showAuditModal, setShowAuditModal] = useState(false)

  useEffect(() => {
    const selectedRepoId = localStorage.getItem('selectedRepoId')
    const selectedRepoName = localStorage.getItem('selectedRepoName')

    if (!selectedRepoId) {
      navigate('/app/select-repo')
      return
    }

    setRepoName(selectedRepoName || 'Repository')
    fetchMetrics(selectedRepoId)
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

  const syncData = async () => {
    const selectedRepoId = localStorage.getItem('selectedRepoId')
    if (!selectedRepoId) return

    try {
      setSyncing(true)
      const response = await api.post(`/api/repos/${selectedRepoId}/sync`)
      console.log('Sync response:', response.data)
      
      // Reload metrics after sync
      await fetchMetrics(selectedRepoId)
      
      alert(`Successfully synced ${response.data.count} pull requests!`)
    } catch (err: any) {
      console.error('Sync error:', err)
      const errorMsg = err.response?.data?.message || 'Failed to sync data'
      alert(`Sync failed: ${errorMsg}`)
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
      
      alert(response.data.message || 'Repository indexed successfully! You can now chat about the code.')
    } catch (err: any) {
      console.error('Error indexing repository:', err)
      const errorMsg = err.response?.data?.message || 'Failed to index repository'
      setError(errorMsg)
      alert('Error: ' + errorMsg)
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
      
      // Start polling for status
      pollAuditStatus(newAuditId)
    } catch (err: any) {
      console.error('Error starting audit:', err)
      const errorMsg = err.response?.data?.message || 'Failed to start audit'
      setError(errorMsg)
      alert('Error: ' + errorMsg)
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
      }
    } catch (err: any) {
      console.error('Error polling audit status:', err)
      setAuditing(false)
    }
  }

  const chartData = metrics?.reviewsPerDay
    ? Object.entries(metrics.reviewsPerDay).map(([name, reviews]) => ({ name, reviews }))
    : []

  return (
    <div className="min-h-screen bg-slate-900 flex">
      {/* Sidebar */}
      <motion.aside
        initial={{ x: -300 }}
        animate={{ x: sidebarOpen ? 0 : -300 }}
        className="w-64 bg-slate-800 border-r border-slate-700 p-6 fixed h-full"
      >
        <div className="flex items-center space-x-2 mb-8">
          <Shield className="w-8 h-8 text-brand-400" />
          <span className="text-xl font-bold text-white">Repo Mind</span>
        </div>

        <nav className="space-y-2">
          <NavItem icon={BarChart} label="Dashboard" to="/app" active />
          <NavItem icon={GitPullRequest} label="Pull Requests" to="/app/pull-requests" />
          <NavItem icon={MessageSquare} label="Chat" to="/app/chat" />
        </nav>

        <div className="mt-8 pt-8 border-t border-slate-700">
          <p className="text-xs text-slate-400 mb-2">Current Repository</p>
          <p className="text-sm text-white font-semibold truncate">{repoName}</p>
          <button
            onClick={() => navigate('/app/select-repo')}
            className="mt-2 text-xs text-brand-400 hover:text-brand-300"
          >
            Change Repository
          </button>
        </div>
      </motion.aside>

      {/* Main Content */}
      <div className={`flex-1 ${sidebarOpen ? 'ml-64' : 'ml-0'} transition-all`}>
        {/* Top Bar */}
        <header className="bg-slate-800 border-b border-slate-700 px-6 py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-4">
              <button
                onClick={() => setSidebarOpen(!sidebarOpen)}
                className="text-slate-400 hover:text-white"
              >
                <Menu className="w-6 h-6" />
              </button>
              <h1 className="text-2xl font-bold text-white">{repoName}</h1>
            </div>
            <div className="flex items-center gap-4">
              <button
                onClick={startAudit}
                disabled={auditing}
                className="flex items-center gap-2 px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50 disabled:cursor-not-allowed transition"
                title="Run AI code audit"
              >
                <Bug className={`w-4 h-4 ${auditing ? 'animate-pulse' : ''}`} />
                {auditing ? 'Auditing...' : 'Audit Code'}
              </button>
              <button
                onClick={indexRepository}
                disabled={indexing}
                className="flex items-center gap-2 px-4 py-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700 disabled:opacity-50 disabled:cursor-not-allowed transition"
                title="Index repository code for AI chat"
              >
                <Shield className={`w-4 h-4 ${indexing ? 'animate-spin' : ''}`} />
                {indexing ? 'Indexing...' : 'Index for Chat'}
              </button>
              <button
                onClick={syncData}
                disabled={syncing}
                className="flex items-center gap-2 px-4 py-2 bg-brand-600 text-white rounded-lg hover:bg-brand-700 disabled:opacity-50 disabled:cursor-not-allowed transition"
              >
                <RefreshCw className={`w-4 h-4 ${syncing ? 'animate-spin' : ''}`} />
                {syncing ? 'Syncing...' : 'Sync Data'}
              </button>
              <div className="w-10 h-10 rounded-full bg-brand-600 flex items-center justify-center text-white font-semibold">
                U
              </div>
            </div>
          </div>
        </header>

        {/* Content */}
        <main className="p-6">
          {loading ? (
            <div className="flex items-center justify-center py-20">
              <Loader className="w-12 h-12 text-brand-400 animate-spin" />
            </div>
          ) : error ? (
            <div className="flex items-center justify-center py-20">
              <div className="max-w-md w-full bg-red-900/20 border border-red-500 rounded-xl p-8">
                <div className="flex items-center gap-3 mb-4">
                  <AlertTriangle className="w-8 h-8 text-red-400" />
                  <h2 className="text-xl font-bold text-white">Connection Error</h2>
                </div>
                <p className="text-slate-300 mb-6">{error}</p>
                <button
                  onClick={() => {
                    const selectedRepoId = localStorage.getItem('selectedRepoId')
                    if (selectedRepoId) {
                      fetchMetrics(selectedRepoId)
                    }
                  }}
                  className="w-full px-4 py-3 bg-brand-600 text-white rounded-lg hover:bg-brand-700 transition font-semibold"
                >
                  Retry Connection
                </button>
              </div>
            </div>
          ) : metrics ? (
            <>
              {/* Metrics Cards */}
              <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
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
                  icon={Shield}
                  color="purple"
                />
              </div>

              {/* Chart */}
              <div className="bg-slate-800 rounded-xl p-6 border border-slate-700">
                <h2 className="text-xl font-semibold text-white mb-6">Review Activity</h2>
                {chartData.length > 0 ? (
                  <ResponsiveContainer width="100%" height={300}>
                    <BarChart data={chartData}>
                      <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
                      <XAxis dataKey="name" stroke="#94a3b8" />
                      <YAxis stroke="#94a3b8" />
                      <Tooltip
                        contentStyle={{ backgroundColor: '#1e293b', border: '1px solid #334155' }}
                        labelStyle={{ color: '#fff' }}
                      />
                      <Bar dataKey="reviews" fill="#0ea5e9" radius={[8, 8, 0, 0]} />
                    </BarChart>
                  </ResponsiveContainer>
                ) : (
                  <p className="text-slate-400 text-center py-12">No review data available yet</p>
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
          <div className="fixed inset-0 bg-black/70 backdrop-blur-sm flex items-center justify-center z-50 p-4">
            <motion.div
              initial={{ scale: 0.95, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              className="bg-slate-800 rounded-2xl border border-slate-700 max-w-4xl w-full max-h-[90vh] overflow-hidden"
            >
              {/* Header */}
              <div className="p-6 border-b border-slate-700">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <Bug className="w-8 h-8 text-red-400" />
                    <h2 className="text-2xl font-bold text-white">Code Audit</h2>
                  </div>
                  <button
                    onClick={() => setShowAuditModal(false)}
                    className="text-slate-400 hover:text-white transition"
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
                    auditStatus.status === 'COMPLETED' ? 'bg-green-900/20 text-green-400 border border-green-500/30' :
                    auditStatus.status === 'FAILED' ? 'bg-red-900/20 text-red-400 border border-red-500/30' :
                    'bg-blue-900/20 text-blue-400 border border-blue-500/30'
                  }`}>
                    {auditStatus.status === 'IN_PROGRESS' && <Loader className="w-4 h-4 animate-spin" />}
                    {auditStatus.status}
                  </span>
                </div>

                {/* Progress Bar */}
                {(auditStatus.status === 'IN_PROGRESS' || auditStatus.status === 'QUEUED') && (
                  <div className="mb-6">
                    <div className="flex items-center justify-between mb-2">
                      <span className="text-sm text-slate-400">Progress</span>
                      <span className="text-sm font-semibold text-white">{auditStatus.progressPercentage || 0}%</span>
                    </div>
                    <div className="w-full bg-slate-700 rounded-full h-3 overflow-hidden">
                      <motion.div
                        className="h-full bg-gradient-to-r from-red-500 to-orange-500"
                        initial={{ width: 0 }}
                        animate={{ width: `${auditStatus.progressPercentage || 0}%` }}
                        transition={{ duration: 0.5 }}
                      />
                    </div>
                    {auditStatus.currentFile && (
                      <p className="text-sm text-slate-400 mt-2">
                        Analyzing: <span className="text-white font-mono">{auditStatus.currentFile}</span>
                      </p>
                    )}
                  </div>
                )}

                {/* Statistics */}
                <div className="grid grid-cols-3 gap-4 mb-6">
                  <div className="bg-slate-900/50 rounded-lg p-4 border border-slate-700">
                    <div className="text-3xl font-bold text-red-400">{auditStatus.criticalCount || 0}</div>
                    <div className="text-sm text-slate-400">Critical</div>
                  </div>
                  <div className="bg-slate-900/50 rounded-lg p-4 border border-slate-700">
                    <div className="text-3xl font-bold text-yellow-400">{auditStatus.warningCount || 0}</div>
                    <div className="text-sm text-slate-400">Warnings</div>
                  </div>
                  <div className="bg-slate-900/50 rounded-lg p-4 border border-slate-700">
                    <div className="text-3xl font-bold text-blue-400">{auditStatus.infoCount || 0}</div>
                    <div className="text-sm text-slate-400">Info</div>
                  </div>
                </div>

                {/* Files Scanned */}
                {auditStatus.totalFilesScanned > 0 && (
                  <div className="bg-slate-900/30 rounded-lg p-4 border border-slate-700 mb-6">
                    <div className="flex items-center justify-between">
                      <span className="text-slate-300">Files Scanned</span>
                      <span className="text-white font-semibold">{auditStatus.totalFilesScanned}</span>
                    </div>
                    <div className="flex items-center justify-between mt-2">
                      <span className="text-slate-300">Files with Issues</span>
                      <span className="text-white font-semibold">{auditStatus.filesWithIssues || 0}</span>
                    </div>
                  </div>
                )}

                {/* Error Message */}
                {auditStatus.errorMessage && (
                  <div className="bg-red-900/20 border border-red-500/30 rounded-lg p-4 mb-6">
                    <div className="flex items-start gap-3">
                      <AlertTriangle className="w-5 h-5 text-red-400 flex-shrink-0 mt-0.5" />
                      <div>
                        <h4 className="font-semibold text-red-400 mb-1">Error</h4>
                        <p className="text-sm text-red-300">{auditStatus.errorMessage}</p>
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
                    className="w-full px-6 py-4 bg-gradient-to-r from-red-600 to-orange-600 text-white rounded-lg hover:from-red-700 hover:to-orange-700 transition font-semibold text-lg"
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
      className={`w-full flex items-center gap-3 px-4 py-3 rounded-lg transition ${
        active
          ? 'bg-brand-600 text-white'
          : 'text-slate-300 hover:bg-slate-700 hover:text-white'
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
    <div className="bg-slate-800 rounded-xl p-6 border border-slate-700">
      <div className="flex items-center justify-between mb-4">
        <Icon className={`w-8 h-8 ${colors[color as keyof typeof colors]}`} />
        <span className="text-sm text-slate-400">{change}</span>
      </div>
      <h3 className="text-3xl font-bold text-white mb-1">{value}</h3>
      <p className="text-slate-400">{title}</p>
    </div>
  )
}
