import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import api from '../api/axios'
import { Shield, FileText, AlertTriangle, Info, CheckCircle, ArrowLeft } from 'lucide-react'
import { motion } from 'framer-motion'

interface ReviewComment {
  id: number
  filePath: string
  lineNumber: number
  severity: string
  category: string
  body: string
  rationale: string
  suggestion: string
}

interface ReviewRun {
  id: number
  summary: string
  commentCount: number
  comments: ReviewComment[]
  createdAt: string
}

export default function ReviewResult() {
  const { prId } = useParams<{ prId: string }>()
  const navigate = useNavigate()
  const [review, setReview] = useState<ReviewRun | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!prId) {
      navigate('/app/pull-requests')
      return
    }

    fetchReview(prId)
  }, [prId, navigate])

  const fetchReview = async (prId: string) => {
    try {
      setLoading(true)
      setError(null)
      const response = await api.get(`/api/reviews/pr/${prId}`)
      
      if (response.data && response.data.length > 0) {
        // Get the most recent review (last in array)
        const latestReview = response.data[response.data.length - 1]
        setReview(latestReview)
      } else {
        setError('No review found for this pull request.')
      }
    } catch (err: any) {
      console.error('Error fetching review:', err)
      setError(err.response?.data?.message || 'Failed to load review.')
    } finally {
      setLoading(false)
    }
  }

  const getSeverityIcon = (severity: string) => {
    switch (severity.toLowerCase()) {
      case 'critical':
        return <AlertTriangle className="w-5 h-5 text-red-500" />
      case 'warning':
        return <AlertTriangle className="w-5 h-5 text-yellow-500" />
      case 'info':
        return <Info className="w-5 h-5 text-blue-500" />
      default:
        return <CheckCircle className="w-5 h-5 text-green-500" />
    }
  }

  const getSeverityBadgeClass = (severity: string) => {
    switch (severity.toLowerCase()) {
      case 'critical':
        return 'bg-red-500/20 text-red-400 border-red-500/50'
      case 'warning':
        return 'bg-yellow-500/20 text-yellow-400 border-yellow-500/50'
      case 'info':
        return 'bg-blue-500/20 text-blue-400 border-blue-500/50'
      default:
        return 'bg-green-500/20 text-green-400 border-green-500/50'
    }
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <Shield className="w-12 h-12 text-gray-900 animate-pulse mx-auto mb-4" />
          <p className="text-gray-600 font-medium">Loading review...</p>
        </div>
      </div>
    )
  }

  if (error || !review) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center px-6">
        <div className="max-w-md w-full bg-white rounded-2xl p-8 border border-gray-200 text-center shadow-lg">
          <AlertTriangle className="w-16 h-16 text-yellow-600 mx-auto mb-4" />
          <h2 className="text-xl font-bold text-gray-900 mb-4">No Review Found</h2>
          <p className="text-gray-600 mb-6">
            {error || 'No AI review has been run for this pull request yet.'}
          </p>
          <button
            onClick={() => navigate('/app/pull-requests')}
            className="w-full px-4 py-3 bg-gray-900 text-white rounded-xl hover:bg-gray-800 transition shadow-sm font-semibold"
          >
            Back to Pull Requests
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gray-50 px-6 py-12">
      <div className="max-w-5xl mx-auto">
        {/* Header */}
        <div className="mb-8">
          <button
            onClick={() => navigate('/app/pull-requests')}
            className="flex items-center gap-2 text-gray-600 hover:text-gray-900 mb-4 transition font-medium"
          >
            <ArrowLeft className="w-4 h-4" />
            Back to Pull Requests
          </button>
          <div className="flex items-center gap-4">
            <Shield className="w-10 h-10 text-gray-900" />
            <div>
              <h1 className="text-4xl font-bold text-gray-900 tracking-tight">AI Review Results</h1>
              <p className="text-gray-600">Pull Request #{prId}</p>
            </div>
          </div>
        </div>

        {/* Summary Card */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, ease: "easeOut" }}
          className="bg-gray-900 border border-gray-800 rounded-3xl p-8 mb-10 shadow-md hover:shadow-2xl transition-shadow duration-500 ease-out"
        >
          <div className="flex items-start gap-4">
            <FileText className="w-8 h-8 text-white flex-shrink-0 mt-1" />
            <div>
              <h2 className="text-2xl font-semibold text-white mb-3">Summary</h2>
              <p className="text-gray-300 leading-relaxed text-lg">{review.summary}</p>
              <div className="mt-5 flex items-center gap-4 text-sm text-gray-400">
                <span>{review.commentCount} comments</span>
                <span>•</span>
                <span>{new Date(review.createdAt).toLocaleString()}</span>
              </div>
            </div>
          </div>
        </motion.div>

        {/* Comments List */}
        <div className="space-y-6">
          <h2 className="text-3xl font-bold text-gray-900 mb-6">Comments ({review.comments.length})</h2>
          
          {review.comments.length === 0 ? (
            <div className="bg-gray-900 rounded-3xl p-12 border border-gray-800 text-center shadow-sm">
              <CheckCircle className="w-16 h-16 text-green-500 mx-auto mb-4" />
              <h3 className="text-xl font-semibold text-white mb-2">All Clear!</h3>
              <p className="text-gray-400">No issues found in this pull request.</p>
            </div>
          ) : (
            review.comments.map((comment, index) => (
              <motion.div
                key={comment.id}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: index * 0.05, duration: 0.6, ease: "easeOut" }}
                className="bg-gray-900 rounded-3xl p-8 border border-gray-800 shadow-md hover:shadow-2xl transition-all duration-500 ease-out"
              >
                <div className="flex items-start justify-between mb-5">
                  <div className="flex items-center gap-3">
                    {getSeverityIcon(comment.severity)}
                    <div>
                      <div className="flex items-center gap-2">
                        <FileText className="w-4 h-4 text-gray-400" />
                        <span className="text-white font-mono text-sm">
                          {comment.filePath}:{comment.lineNumber}
                        </span>
                      </div>
                      <span className="text-gray-400 text-sm">{comment.category}</span>
                    </div>
                  </div>
                  <span className={`px-3 py-1 rounded-full text-xs font-semibold border ${getSeverityBadgeClass(comment.severity)}`}>
                    {comment.severity}
                  </span>
                </div>

                <div className="space-y-4">
                  <div>
                    <h4 className="text-sm font-semibold text-gray-400 mb-2">AI Review</h4>
                    <p className="text-white whitespace-pre-wrap leading-relaxed">{comment.body}</p>
                  </div>

                  {comment.suggestion && (
                    <div>
                      <h4 className="text-sm font-semibold text-gray-400 mb-2">Suggestion</h4>
                      <p className="text-green-400 leading-relaxed">{comment.suggestion}</p>
                    </div>
                  )}
                </div>
              </motion.div>
            ))
          )}
        </div>
      </div>
    </div>
  )
}
