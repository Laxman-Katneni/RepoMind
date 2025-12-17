import { motion } from 'framer-motion'
import { Github, Shield, MessageSquare, Zap, CheckCircle, ArrowRight } from 'lucide-react'
import { useNavigate } from 'react-router-dom'

export default function Landing() {
  const navigate = useNavigate()

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-blue-900 to-slate-900">
      {/* Header */}
      <header className="container mx-auto px-6 py-6">
        <nav className="flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <Shield className="w-8 h-8 text-brand-400" />
            <span className="text-2xl font-bold text-white">Repo Mind</span>
          </div>
          <button
            onClick={() => navigate('/login')}
            className="px-4 py-2 bg-brand-600 text-white rounded-lg hover:bg-brand-700 transition"
          >
            Login
          </button>
        </nav>
      </header>

      {/* Hero Section */}
      <section className="container mx-auto px-6 py-20 text-center">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8 }}
        >
          <h1 className="text-6xl font-bold text-white mb-6">
            Repo Mind: AI-Powered
            <br />
            <span className="text-transparent bg-clip-text bg-gradient-to-r from-brand-400 to-blue-600">
              Code Security
            </span>
          </h1>
          <p className="text-xl text-slate-300 mb-8 max-w-2xl mx-auto">
            Automated pull request reviews with context-aware AI. Catch bugs, security issues, and code smells before they reach production.
          </p>
          
          <div className="flex gap-4 justify-center">
            <button
              onClick={() => navigate('/login')}
              className="px-8 py-4 bg-brand-600 text-white rounded-lg hover:bg-brand-700 transition flex items-center gap-2 text-lg font-semibold"
            >
              <Github className="w-5 h-5" />
              Login with GitHub
            </button>
            <button
              className="px-8 py-4 bg-slate-800 text-white rounded-lg hover:bg-slate-700 transition flex items-center gap-2 text-lg font-semibold"
            >
              <ArrowRight className="w-5 h-5" />
              View Demo
            </button>
          </div>
        </motion.div>
      </section>

      {/* Demo Screenshot */}
      <section className="container mx-auto px-6 py-12">
        <motion.div
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: 0.8, delay: 0.2 }}
          className="bg-slate-800 rounded-2xl p-8 shadow-2xl border border-slate-700"
        >
          <div className="aspect-video bg-gradient-to-br from-slate-700 to-slate-900 rounded-lg flex items-center justify-center">
            <div className="text-center">
              <Zap className="w-16 h-16 text-brand-400 mx-auto mb-4" />
              <p className="text-slate-400 text-lg">Interactive Demo Coming Soon</p>
            </div>
          </div>
        </motion.div>
      </section>

      {/* Features Grid */}
      <section className="container mx-auto px-6 py-20">
        <h2 className="text-4xl font-bold text-white text-center mb-12">
          Smart Code Reviews, Powered by AI
        </h2>
        
        <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-8">
          {features.map((feature, index) => (
            <motion.div
              key={feature.title}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.5, delay: index * 0.1 }}
              className="bg-slate-800 rounded-xl p-6 border border-slate-700 hover:border-brand-500 transition"
            >
              <feature.icon className="w-12 h-12 text-brand-400 mb-4" />
              <h3 className="text-xl font-semibold text-white mb-2">{feature.title}</h3>
              <p className="text-slate-300">{feature.description}</p>
            </motion.div>
          ))}
        </div>
      </section>

      {/* Footer */}
      <footer className="container mx-auto px-6 py-8 text-center text-slate-400 border-t border-slate-800">
        <p>© 2025 Repo Mind. Built with Spring Boot + React + AI.</p>
      </footer>
    </div>
  )
}

const features = [
  {
    icon: Shield,
    title: 'Context-Aware Reviews',
    description: 'AI analyzes your entire codebase to provide contextual feedback on PRs.'
  },
  {
    icon: MessageSquare,
    title: 'Chat with Codebase',
    description: 'Ask questions about your code and get instant AI-powered answers.'
  },
  {
    icon: CheckCircle,
    title: 'Automated Security Checks',
    description: 'Detect vulnerabilities and security issues before deployment.'
  },
  {
    icon: Zap,
    title: 'Instant Feedback',
    description: 'Get PR reviews in seconds, not hours. Speed up your development workflow.'
  },
  {
    icon: Github,
    title: 'GitHub Integration',
    description: 'Seamless integration with GitHub via webhooks and OAuth.'
  },
  {
    icon: MessageSquare,
    title: 'Smart Caching',
    description: 'Avoid duplicate reviews with intelligent commit-based caching.'
  }
]
