import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../api/axios'
import { Send, Bot, User, Loader } from 'lucide-react'
import { motion } from 'framer-motion'

interface Message {
  role: 'user' | 'assistant'
  content: string
}

export default function Chat() {
  const navigate = useNavigate()
  const [messages, setMessages] = useState<Message[]>([
    {
      role: 'assistant',
      content: "Hi! I'm Repo Mind AI. Ask me anything about your codebase, and I'll provide context-aware answers using RAG."
    }
  ])
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)
  const [repoId, setRepoId] = useState<string | null>(null)

  useEffect(() => {
    const selectedRepoId = localStorage.getItem('selectedRepoId')
    if (!selectedRepoId) {
      navigate('/app/select-repo')
      return
    }
    setRepoId(selectedRepoId)
  }, [navigate])

  const handleSend = async () => {
    if (!input.trim() || !repoId || loading) return

    const userMessage: Message = { role: 'user', content: input }
    setMessages(prev => [...prev, userMessage])
    setInput('')
    setLoading(true)

    try {
      const response = await api.post('/api/chat', {
        message: input,
        repoId: parseInt(repoId)
      })

      const aiMessage: Message = {
        role: 'assistant',
        content: response.data.answer
      }
      
      setMessages(prev => [...prev, aiMessage])
    } catch (err: any) {
      console.error('Error sending message:', err)
      
      if (err.response?.status === 401) {
        // Unauthorized - redirect to login
        navigate('/login')
        return
      }
      
      const errorMessage: Message = {
        role: 'assistant',
        content: `Sorry, I encountered an error: ${err.response?.data?.message || 'Unable to process your request. Please try again.'}`
      }
      
      setMessages(prev => [...prev, errorMessage])
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-slate-900 flex flex-col">
      {/* Header */}
      <header className="bg-slate-800 border-b border-slate-700 px-6 py-4">
        <div className="flex items-center gap-3">
          <Bot className="w-8 h-8 text-brand-400" />
          <div>
            <h1 className="text-xl font-bold text-white">Chat with Your Codebase</h1>
            <p className="text-sm text-slate-400">RAG-powered AI assistant</p>
          </div>
        </div>
      </header>

      {/* Messages */}
      <div className="flex-1 overflow-y-auto px-6 py-8">
        <div className="max-w-4xl mx-auto space-y-6">
          {messages.map((message, index) => (
            <motion.div
              key={index}
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.3 }}
              className={`flex gap-4 ${message.role === 'user' ? 'justify-end' : ''}`}
            >
              {message.role === 'assistant' && (
                <div className="w-10 h-10 rounded-full bg-brand-600 flex items-center justify-center flex-shrink-0">
                  <Bot className="w-6 h-6 text-white" />
                </div>
              )}
              
              <div
                className={`px-6 py-4 rounded-2xl max-w-2xl ${
                  message.role === 'user'
                    ? 'bg-brand-600 text-white'
                    : 'bg-slate-800 text-slate-100 border border-slate-700'
                }`}
              >
                <p className="whitespace-pre-wrap">{message.content}</p>
              </div>

              {message.role === 'user' && (
                <div className="w-10 h-10 rounded-full bg-slate-700 flex items-center justify-center flex-shrink-0">
                  <User className="w-6 h-6 text-slate-300" />
                </div>
              )}
            </motion.div>
          ))}
          
          {loading && (
            <div className="flex gap-4">
              <div className="w-10 h-10 rounded-full bg-brand-600 flex items-center justify-center flex-shrink-0">
                <Bot className="w-6 h-6 text-white" />
              </div>
              <div className="px-6 py-4 rounded-2xl bg-slate-800 border border-slate-700">
                <Loader className="w-5 h-5 text-brand-400 animate-spin" />
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Input */}
      <div className="bg-slate-800 border-t border-slate-700 px-6 py-4">
        <div className="max-w-4xl mx-auto">
          <div className="flex gap-3">
            <input
              type="text"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyPress={(e) => e.key === 'Enter' && !e.shiftKey && handleSend()}
              disabled={loading}
              placeholder="Ask about your codebase..."
              className="flex-1 px-4 py-3 bg-slate-900 text-white rounded-lg border border-slate-600 focus:border-brand-500 focus:outline-none disabled:opacity-50"
            />
            <button
              onClick={handleSend}
              disabled={loading || !input.trim()}
              className="px-6 py-3 bg-brand-600 text-white rounded-lg hover:bg-brand-700 transition flex items-center gap-2 font-semibold disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {loading ? <Loader className="w-5 h-5 animate-spin" /> : <Send className="w-5 h-5" />}
              Send
            </button>
          </div>
          <p className="text-xs text-slate-400 mt-2">
            Powered by RAG and GPT-4. Press Enter to send.
          </p>
        </div>
      </div>
    </div>
  )
}
