import { motion } from 'framer-motion'
import { Github, Shield, MessageSquare, Zap, CheckCircle } from 'lucide-react'
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
              AI-Powered Code Intelligence
            </span>
          </h1>
          <p className="text-xl text-gray-600 mb-10 max-w-2xl mx-auto leading-relaxed">
            Comprehensive code audits with architectural insights, automated PR reviews, and intelligent chat for your GitHub repositories. Catch bugs and security issues before they reach production.
          </p>
          
          <button
            onClick={() => navigate('/login')}
            className="px-8 py-4 bg-gray-900 text-white rounded-xl hover:bg-gray-800 transition flex items-center gap-3 text-lg font-semibold shadow-lg hover:shadow-xl mx-auto"
          >
            <Github className="w-5 h-5" />
            Get Started with GitHub
          </button>
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
      <footer className="container mx-auto px-6 py-12 text-center border-t border-gray-100">
        {/* Logo */}
        <div className="mb-4">
          <a 
            href="https://linkedin.com/in/laxman-katneni" 
            target="_blank" 
            rel="noopener noreferrer"
            className="inline-block hover:opacity-70 transition"
          >
            <img src="/laxman-logo.png" alt="Laxman Katneni" className="h-12 mx-auto" />
          </a>
        </div>
        
        {/* Name */}
        <p className="text-gray-600 text-sm mb-3">
          Created by <span className="font-semibold text-gray-900">Laxman Katneni</span>
        </p>
        
        {/* Social Links */}
        <div className="flex items-center justify-center gap-4 mb-6">
          <a 
            href="https://linkedin.com/in/laxman-katneni" 
            target="_blank" 
            rel="noopener noreferrer"
            className="text-gray-600 hover:text-gray-900 transition"
            title="LinkedIn"
          >
            <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
              <path d="M19 0h-14c-2.761 0-5 2.239-5 5v14c0 2.761 2.239 5 5 5h14c2.762 0 5-2.239 5-5v-14c0-2.761-2.238-5-5-5zm-11 19h-3v-11h3v11zm-1.5-12.268c-.966 0-1.75-.79-1.75-1.764s.784-1.764 1.75-1.764 1.75.79 1.75 1.764-.783 1.764-1.75 1.764zm13.5 12.268h-3v-5.604c0-3.368-4-3.113-4 0v5.604h-3v-11h3v1.765c1.396-2.586 7-2.777 7 2.476v6.759z"/>
            </svg>
          </a>
          <a 
            href="https://github.com/Laxman-Katneni" 
            target="_blank" 
            rel="noopener noreferrer"
            className="text-gray-600 hover:text-gray-900 transition"
            title="GitHub"
          >
            <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
              <path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z"/>
            </svg>
          </a>
        </div>
        
        {/* Copyright */}
        <p className="text-gray-500 text-sm">© 2025 Repo Mind. All rights reserved.</p>
      </footer>
    </div>
  )
}

const features = [
  {
    icon: Shield,
    title: 'Advanced Code Audits',
    description: 'Deep security scanning with architectural insights and best practice recommendations, not just basic linting.'
  },
  {
    icon: CheckCircle,
    title: 'Automated PR Reviews',
    description: 'AI-powered pull request analysis with actionable feedback and intelligent code suggestions.'
  },
  {
    icon: MessageSquare,
    title: 'Intelligent Code Chat',
    description: 'Ask questions about your codebase and get instant, context-aware answers powered by AI.'
  },
  {
    icon: Zap,
    title: 'Smart Caching',
    description: 'Efficient review system with intelligent caching to avoid duplicate analysis and speed up workflows.'
  },
  {
    icon: Github,
    title: 'Seamless GitHub Integration',
    description: 'Easy setup with GitHub OAuth and webhook integration for automated workflows.'
  },
  {
    icon: Shield,
    title: 'Security-First Analysis',
    description: 'Proactively detect vulnerabilities, code smells, and potential security issues before deployment.'
  }
]
