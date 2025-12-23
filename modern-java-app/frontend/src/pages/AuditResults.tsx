import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import api from '../api/axios'
import { ArrowLeft, Bug, FileCode, AlertTriangle, Info, Filter } from 'lucide-react'
import { motion } from 'framer-motion'

interface Finding {
  id: number
  filePath: string
  lineNumber: number | null
  severity: string
  category: string
  language: string
  title: string
  message: string
  suggestion: string
  codeSnippet: string | null
  createdAt: string
}

interface AuditStatus {
  auditId: number
  status: string
  totalFilesScanned: number
  filesWithIssues: number
  criticalCount: number
  warningCount: number
  infoCount: number
}

export default function AuditResults() {
  const { auditId } = useParams<{ auditId: string }>()
  const navigate = useNavigate()
  
  const [findings, setFindings] = useState<Finding[]>([])
  const [auditStatus, setAuditStatus] = useState<AuditStatus | null>(null)
  const [loading, setLoading] = useState(true)
  const [severityFilter, setSeverityFilter] = useState<string>('')
  const [categoryFilter, setCategoryFilter] = useState<string>('')

  useEffect(() => {
    if (auditId) {
      fetchAuditData()
    }
  }, [auditId, severityFilter, categoryFilter])

  const fetchAuditData = async () => {
    try {
      setLoading(true)
      
      // Fetch audit status
      const statusRes = await api.get(`/api/audits/${auditId}/status`)
      setAuditStatus(statusRes.data)
      
      // Fetch findings with filters
      const params = new URLSearchParams()
      if (severityFilter) params.append('severity', severityFilter)
      if (categoryFilter) params.append('category', categoryFilter)
      
      const findingsRes = await api.get(`/api/audits/${auditId}/findings?${params.toString()}`)
      setFindings(findingsRes.data.content || [])
    } catch (err: any) {
      console.error('Error fetching audit data:', err)
    } finally {
      setLoading(false)
    }
  }

  const getSeverityColor = (severity: string) => {
    switch (severity.toUpperCase()) {
      case 'CRITICAL': return 'text-red-400 bg-red-900/20 border-red-500/30'
      case 'WARNING': return 'text-yellow-400 bg-yellow-900/20 border-yellow-500/30'
      case 'INFO': return 'text-blue-400 bg-blue-900/20 border-blue-500/30'
      default: return 'text-slate-400 bg-slate-900/20 border-slate-500/30'
    }
  }

  const getSeverityIcon = (severity: string) => {
    switch (severity.toUpperCase()) {
      case 'CRITICAL': return <Bug className="w-5 h-5 text-red-400" />
      case 'WARNING': return <AlertTriangle className="w-5 h-5 text-yellow-400" />
      case 'INFO': return <Info className="w-5 h-5 text-blue-400" />
      default: return <FileCode className="w-5 h-5 text-slate-400" />
    }
  }

  return (
    <div className="min-h-screen bg-slate-900 p-6">
      {/* Header */}
      <div className="max-w-7xl mx-auto mb-8">
        <button
          onClick={() => navigate('/app')}
          className="flex items-center gap-2 text-slate-400 hover:text-white transition mb-6"
        >
          <ArrowLeft className="w-5 h-5" />
          Back to Dashboard
        </button>

        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold text-white mb-2">Audit Results</h1>
            <p className="text-slate-400">Audit ID: {auditId}</p>
          </div>
          
          {auditStatus && (
            <div className="grid grid-cols-3 gap-4">
              <div className="bg-slate-800 rounded-lg p-4 border border-slate-700 text-center">
                <div className="text-2xl font-bold text-red-400">{auditStatus.criticalCount}</div>
                <div className="text-sm text-slate-400">Critical</div>
              </div>
              <div className="bg-slate-800 rounded-lg p-4 border border-slate-700 text-center">
                <div className="text-2xl font-bold text-yellow-400">{auditStatus.warningCount}</div>
                <div className="text-sm text-slate-400">Warnings</div>
              </div>
              <div className="bg-slate-800 rounded-lg p-4 border border-slate-700 text-center">
                <div className="text-2xl font-bold text-blue-400">{auditStatus.infoCount}</div>
                <div className="text-sm text-slate-400">Info</div>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Filters */}
      <div className="max-w-7xl mx-auto mb-6">
        <div className="bg-slate-800 rounded-lg p-4 border border-slate-700">
          <div className="flex items-center gap-4">
            <Filter className="w-5 h-5 text-slate-400" />
            <select
              value={severityFilter}
              onChange={(e) => setSeverityFilter(e.target.value)}
              className="bg-slate-700 text-white rounded px-3 py-2 border border-slate-600 focus:outline-none focus:border-brand-500"
            >
              <option value="">All Severities</option>
              <option value="CRITICAL">Critical</option>
              <option value="WARNING">Warning</option>
              <option value="INFO">Info</option>
            </select>
            
            <select
              value={categoryFilter}
              onChange={(e) => setCategoryFilter(e.target.value)}
              className="bg-slate-700 text-white rounded px-3 py-2 border border-slate-600 focus:outline-none focus:border-brand-500"
            >
              <option value="">All Categories</option>
              <option value="SECURITY">Security</option>
              <option value="ARCHITECTURE">Architecture</option>
              <option value="BEST_PRACTICE">Best Practice</option>
            </select>

            {(severityFilter || categoryFilter) && (
              <button
                onClick={() => { setSeverityFilter(''); setCategoryFilter('') }}
                className="text-sm text-brand-400 hover:text-brand-300"
              >
                Clear Filters
              </button>
            )}
          </div>
        </div>
      </div>

      {/* Findings List */}
      <div className="max-w-7xl mx-auto">
        {loading ? (
          <div className="text-center py-20">
            <div className="text-slate-400">Loading findings...</div>
          </div>
        ) : findings.length === 0 ? (
          <div className="bg-slate-800 rounded-xl p-12 border border-slate-700 text-center">
            <Bug className="w-16 h-16 text-slate-600 mx-auto mb-4" />
            <h3 className="text-xl font-semibold text-white mb-2">No Findings</h3>
            <p className="text-slate-400">No issues found matching your filters.</p>
          </div>
        ) : (
          <div className="space-y-4">
            {findings.map((finding, index) => (
              <motion.div
                key={finding.id}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: index * 0.05 }}
                className="bg-slate-800 rounded-xl p-6 border border-slate-700"
              >
                <div className="flex items-start justify-between mb-4">
                  <div className="flex items-start gap-3">
                    {getSeverityIcon(finding.severity)}
                    <div>
                      <div className="flex items-center gap-3 mb-2">
                        <h3 className="text-lg font-semibold text-white">{finding.title}</h3>
                        <span className={`px-3 py-1 rounded-full text-xs font-semibold border ${getSeverityColor(finding.severity)}`}>
                          {finding.severity}
                        </span>
                        <span className="px-3 py-1 rounded-full text-xs font-semibold bg-slate-700 text-slate-300">
                          {finding.category}
                        </span>
                      </div>
                      <div className="flex items-center gap-2 text-sm text-slate-400">
                        <FileCode className="w-4 h-4" />
                        <span className="font-mono">{finding.filePath}</span>
                        {finding.lineNumber && (
                          <span className="text-slate-500">Line {finding.lineNumber}</span>
                        )}
                      </div>
                    </div>
                  </div>
                  <span className="text-xs text-slate-500">{finding.language}</span>
                </div>

                <div className="mb-4">
                  <p className="text-slate-300">{finding.message}</p>
                </div>

                {finding.codeSnippet && (
                  <div className="mb-4">
                    <div className="bg-slate-900 rounded-lg p-4 border border-slate-700">
                      <pre className="text-sm text-slate-300 font-mono overflow-x-auto">
                        {finding.codeSnippet}
                      </pre>
                    </div>
                  </div>
                )}

                <div className="bg-slate-900/50 rounded-lg p-4 border border-slate-700">
                  <h4 className="text-sm font-semibold text-brand-400 mb-2">💡 Suggestion</h4>
                  <p className="text-sm text-slate-300">{finding.suggestion}</p>
                </div>
              </motion.div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
