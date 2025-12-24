import { useEffect, useRef } from 'react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { toast } from 'sonner'
import { useNavigate, useLocation } from 'react-router-dom'

interface AuditNotification {
  auditId: number
  status: string
  message: string
  criticalCount: number
  warningCount: number
  infoCount: number
  totalIssues: number
}

export default function WebSocketListener() {
  const clientRef = useRef<Client | null>(null)
  const navigate = useNavigate()
  const location = useLocation()

  useEffect(() => {
    // Only connect if user is in the app (authenticated)
    if (!location.pathname.startsWith('/app')) {
      console.log('[WebSocket] Skipping connection - user not authenticated')
      return
    }

    console.log('[WebSocket] Initializing connection...')

    // Create WebSocket connection
    const client = new Client({
      webSocketFactory: () => {
        try {
          // Use current protocol and host for WebSocket connection
          return new SockJS('http://localhost:8080/ws')
        } catch (error) {
          console.error('[WebSocket] Failed to create SockJS:', error)
          throw error
        }
      },
      debug: (str) => {
        console.log('[WebSocket]', str)
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onWebSocketError: (error) => {
        console.error('[WebSocket] WebSocket error:', error)
      },
    })

    // Handle successful connection
    client.onConnect = () => {
      console.log('✅ WebSocket connected')

      // Subscribe to audit updates topic
      client.subscribe('/topic/audit-updates', (message) => {
        try {
          const notification: AuditNotification = JSON.parse(message.body)
          console.log('📨 Received audit notification:', notification)

          // Clear the running audit flag from localStorage
          const selectedRepoId = localStorage.getItem('selectedRepoId')
          if (selectedRepoId) {
            localStorage.removeItem(`runningAudit_${selectedRepoId}`)
          }

          if (notification.status === 'COMPLETED') {
            // Show success toast with action button
            toast.success(notification.message, {
              action: {
                label: 'View Results',
                onClick: () => navigate(`/app/audit-results/${notification.auditId}`)
              },
              duration: 10000,
            })
          } else if (notification.status === 'FAILED') {
            // Show error toast
            toast.error(notification.message, {
              duration: 8000,
            })
          }
        } catch (error) {
          console.error('[WebSocket] Failed to parse message:', error)
        }
      })
    }

    // Handle connection errors
    client.onStompError = (frame) => {
      console.error('❌ WebSocket STOMP error:', frame.headers['message'])
      console.error('Details:', frame.body)
    }

    // Handle disconnection
    client.onDisconnect = () => {
      console.log('🔌 WebSocket disconnected')
    }

    // Handle general web socket errors
    client.onWebSocketError = (event) => {
      console.error('❌ WebSocket connection error:', event)
    }

    // Activate the connection
    try {
      client.activate()
      clientRef.current = client
      console.log('[WebSocket] Activation initiated')
    } catch (error) {
      console.error('[WebSocket] Failed to activate:', error)
    }

    // Cleanup on unmount
    return () => {
      if (clientRef.current) {
        console.log('[WebSocket] Deactivating connection...')
        clientRef.current.deactivate()
        clientRef.current = null
      }
    }
  }, [navigate, location.pathname])

  // This component doesn't render anything
  return null
}
