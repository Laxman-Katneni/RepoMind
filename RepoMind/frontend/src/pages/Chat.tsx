import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../api/axios'
import { Send, Bot, User, Loader, Menu, Shield, GitPullRequest, MessageSquare, BarChart, LogOut, ChevronDown } from 'lucide-react'
import { motion, AnimatePresence } from 'framer-motion'

interface Message {
  role: 'user' | 'assistant'
  content: string
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

export default function Chat() {
  const navigate = useNavigate()
  const [sidebarOpen, setSidebarOpen] = useState(true)
  const [messages, setMessages] = useState<Message[]>([
    {
      role: 'assistant',
      content: "Hi! I'm Repo Mind AI. Ask me anything about your codebase, and I'll provide context-aware answers using RAG."
    }
  ])
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)
  const [repoId, setRepoId] = useState<string | null>(null)
  const [repoName, setRepoName] = useState('')
  const [showUserMenu, setShowUserMenu] = useState(false)
  const [conversationId, setConversationId] = useState<string>(() => {
    const existing = sessionStorage.getItem('chatConversationId')
    if (existing) return existing
    const newId = crypto.randomUUID()
    sessionStorage.setItem('chatConversationId', newId)
    return newId
  })

  useEffect(() => {
    const selectedRepoId = localStorage.getItem('selectedRepoId')
    const selectedRepoName = localStorage.getItem('selectedRepoName')
    if (!selectedRepoId) {
      navigate('/app/select-repo')
      return
    }
    setRepoId(selectedRepoId)
    setRepoName(selectedRepoName || 'Repository')
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
        repoId: parseInt(repoId),
        conversationId: conversationId
      })

      const aiMessage: Message = {
        role: 'assistant',
        content: response.data.answer
      }
      
      setMessages(prev => [...prev, aiMessage])
    } catch (err: any) {
      console.error('Error sending message:', err)
      
      const errorText = err.response?.status === 401
        ? 'Error: Authentication required. Please login with GitHub.'
        : err.response?.data?.message || 'Error: Unable to reach AI service. Please check your connection.'
      
      const errorMessage: Message = {
        role: 'assistant',
        content: errorText
      }
      
      setMessages(prev => [...prev, errorMessage])
    } finally {
      setLoading(false)
    }
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
          <NavItem icon={GitPullRequest} label="Pull Requests" to="/app/pull-requests" active={false} />
          <NavItem icon={MessageSquare} label="Chat" to="/app/chat" active={true} />
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
        className={`flex-1 ${sidebarOpen ? 'ml-64' : 'ml-0'} flex flex-col`}
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
              <div className="flex items-center gap-3">
                <Bot className="w-8 h-8 text-gray-900" />
                <div>
                  <h1 className="text-2xl font-bold text-gray-900">Chat with Your Codebase</h1>
                  <p className="text-sm text-gray-600">RAG-powered AI assistant</p>
                </div>
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

        {/* Messages */}
        <div className="flex-1 overflow-y-auto px-6 py-8">
          <div className="max-w-4xl mx-auto space-y-6">
            <AnimatePresence>
              {messages.map((message, index) => (
                <motion.div
                  key={index}
                  initial={{ opacity: 0, y: 20, scale: 0.95 }}
                  animate={{ opacity: 1, y: 0, scale: 1 }}
                  transition={{ 
                    type: "spring",
                    stiffness: 200,
                    damping: 25,
                    delay: index * 0.08
                  }}
                  className={`flex gap-4 ${message.role === 'user' ? 'justify-end' : ''}`}
                >
                  {message.role === 'assistant' && (
                    <motion.div 
                      whileHover={{ scale: 1.1 }}
                      transition={{ duration: 0.4, ease: "easeOut" }}
                      className="w-10 h-10 rounded-full bg-gray-900 flex items-center justify-center flex-shrink-0 shadow-sm"
                    >
                      <Bot className="w-6 h-6 text-white" />
                    </motion.div>
                  )}
                  
                  <motion.div
                    whileHover={{ scale: 1.01 }}
                    transition={{ duration: 0.4, ease: "easeOut" }}
                    className={`px-6 py-4 rounded-3xl max-w-2xl shadow-md ${
                      message.role === 'user'
                        ? 'bg-gray-900 text-white'
                        : 'bg-white text-gray-900 border border-gray-200'
                    }`}
                  >
                    <p className="whitespace-pre-wrap leading-relaxed">{message.content}</p>
                  </motion.div>

                  {message.role === 'user' && (
                    <motion.div 
                      whileHover={{ scale: 1.1 }}
                      transition={{ duration: 0.4, ease: "easeOut" }}
                      className="w-10 h-10 rounded-full bg-gray-200 flex items-center justify-center flex-shrink-0"
                    >
                      <User className="w-6 h-6 text-gray-700" />
                    </motion.div>
                  )}
                </motion.div>
              ))}
            </AnimatePresence>
            
            {loading && (
              <motion.div 
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                className="flex gap-4"
              >
                <div className="w-10 h-10 rounded-full bg-gray-900 flex items-center justify-center flex-shrink-0 shadow-sm">
                  <Bot className="w-6 h-6 text-white" />
                </div>
                <div className="px-6 py-4 rounded-3xl bg-white border border-gray-200 shadow-md">
                  <Loader className="w-5 h-5 text-gray-900 animate-spin" />
                </div>
              </motion.div>
            )}
          </div>
        </div>

        {/* Input */}
        <div className="bg-white border-t border-gray-200 px-6 py-6 shadow-lg">
          <div className="max-w-4xl mx-auto">
            <div className="flex gap-3">
              <input
                type="text"
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyPress={(e) => e.key === 'Enter' && !e.shiftKey && handleSend()}
                disabled={loading}
                placeholder="Ask about your codebase..."
                className="flex-1 px-5 py-4 bg-gray-50 text-gray-900 rounded-2xl border border-gray-200 focus:border-gray-900 focus:outline-none disabled:opacity-50 transition-all duration-200 placeholder-gray-400"
              />
              <motion.button
                whileHover={{ scale: 1.05 }}
                whileTap={{ scale: 0.95 }}
                transition={{ duration: 0.4, ease: "easeOut" }}
                onClick={handleSend}
                disabled={loading || !input.trim()}
                className="px-8 py-4 bg-gray-900 text-white rounded-2xl hover:bg-gray-800 transition-all duration-400 flex items-center gap-2 font-semibold disabled:opacity-50 disabled:cursor-not-allowed shadow-md hover:shadow-xl"
              >
                {loading ? <Loader className="w-5 h-5 animate-spin" /> : <Send className="w-5 h-5" />}
                Send
              </motion.button>
            </div>
            <p className="text-xs text-gray-500 mt-3 text-center">
              Powered by RAG and GPT-4. Press Enter to send.
            </p>
          </div>
        </div>
      </motion.div>
    </div>
  )
}
