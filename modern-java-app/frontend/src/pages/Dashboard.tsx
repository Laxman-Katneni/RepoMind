import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../api/axios'
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts'
import { Shield, GitPullRequest, MessageSquare, AlertTriangle, CheckCircle, Menu, Loader } from 'lucide-react'
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
  const [repoName, setRepoName] = useState('')

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
      const response = await api.get(`/api/repos/${repoId}/metrics`)
      setMetrics(response.data)
    } catch (err: any) {
      console.error('Error fetching metrics:', err)
      
      if (err.response?.status === 401) {
        // Unauthorized - redirect to login
        navigate('/login')
      }
    } finally {
      setLoading(false)
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
              <h1 className="text-2xl font-bold text-white">Dashboard</h1>
            </div>
            <div className="flex items-center gap-4">
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
              <p className="text-slate-400">Failed to load metrics</p>
            </div>
          )}
        </main>
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
