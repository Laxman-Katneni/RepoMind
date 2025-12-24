import { motion } from 'framer-motion'
import { Github, Shield, MessageSquare, Zap, CheckCircle, ArrowRight } from 'lucide-react'
import { useNavigate } from 'react-router-dom'

export default function Landing() {
  const navigate = useNavigate()

  return (
    <div className="min-h-screen bg-white">
      {/* Header */}
      <header className="container mx-auto px-6 py-6 border-b border-gray-100">
        <nav className="flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <Shield className="w-8 h-8 text-gray-900" />
            <span className="text-2xl font-bold text-gray-900">Repo Mind</span>
          </div>
          <button
            onClick={() => navigate('/login')}
            className="px-6 py-2.5 bg-gray-900 text-white rounded-lg hover:bg-gray-800 transition font-medium shadow-sm"
          >
            Login
          </button>
        </nav>
      </header>

      {/* Hero Section */}
      <section className="container mx-auto px-6 py-24 text-center">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8 }}
        >
          <h1 className="text-7xl font-bold text-gray-900 mb-6 tracking-tight">
            Repo Mind
            <br />
            <span className="text-transparent bg-clip-text bg-gradient-to-r from-gray-700 to-gray-900">
              AI-Powered Code Security
            </span>
          </h1>
          <p className="text-xl text-gray-600 mb-10 max-w-2xl mx-auto leading-relaxed">
            Automated pull request reviews with context-aware AI. Catch bugs, security issues, and code smells before they reach production.
          </p>
          
          <div className="flex gap-4 justify-center">
            <button
              onClick={() => navigate('/login')}
              className="px-8 py-4 bg-gray-900 text-white rounded-xl hover:bg-gray-800 transition flex items-center gap-3 text-lg font-semibold shadow-lg hover:shadow-xl"
            >
              <Github className="w-5 h-5" />
              Login with GitHub
            </button>
            <button
              className="px-8 py-4 bg-gray-100 text-gray-900 rounded-xl hover:bg-gray-200 transition flex items-center gap-3 text-lg font-semibold"
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
          className="bg-gray-900 rounded-3xl p-8 shadow-2xl"
        >
          <div className="aspect-video bg-gradient-to-br from-gray-800 to-gray-900 rounded-2xl flex items-center justify-center">
            <div className="text-center">
              <Zap className="w-16 h-16 text-white mx-auto mb-4" />
              <p className="text-gray-400 text-lg">Interactive Demo Coming Soon</p>
            </div>
          </div>
        </motion.div>
      </section>

      {/* Features Grid */}
      <section className="container mx-auto px-6 py-24">
        <h2 className="text-5xl font-bold text-gray-900 text-center mb-16 tracking-tight">
          Smart Code Reviews,
          <br />
          Powered by AI
        </h2>
        
        <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
          {features.map((feature, index) => (
            <motion.div
              key={feature.title}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.5, delay: index * 0.1 }}
              className="bg-gray-900 rounded-2xl p-8 hover:shadow-xl transition group"
            >
              <feature.icon className="w-12 h-12 text-white mb-4 group-hover:scale-110 transition" />
              <h3 className="text-xl font-semibold text-white mb-3">{feature.title}</h3>
              <p className="text-gray-400 leading-relaxed">{feature.description}</p>
            </motion.div>
          ))}
        </div>
      </section>

      {/* Footer */}
      <footer className="container mx-auto px-6 py-8 text-center text-gray-500 border-t border-gray-100">
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
