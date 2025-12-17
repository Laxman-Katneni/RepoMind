import axios from 'axios'

/**
 * Configured axios instance for API calls.
 * Uses cookies for authentication instead of Bearer tokens.
 */
const api = axios.create({
  baseURL: 'http://localhost:8080',
  withCredentials: true  // CRITICAL: Sends JSESSIONID cookie with every request
})

export default api
