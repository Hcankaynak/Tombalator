import { createContext, useContext, useState, useEffect, ReactNode } from 'react'

interface AdminContextType {
  isAdmin: boolean
  checkAdminStatus: (apiKey: string) => Promise<void>
  clearAdmin: () => void
}

const AdminContext = createContext<AdminContextType | undefined>(undefined)

const ADMIN_API_KEY_STORAGE = 'admin_api_key'
// Use environment variable or fallback to relative URL (works with nginx proxy)
const API_BASE_URL = import.meta.env.VITE_API_URL || ''

export function AdminProvider({ children }: { children: ReactNode }) {
  const [isAdmin, setIsAdmin] = useState(false)

  useEffect(() => {
    // Check if admin API key exists in localStorage
    const savedApiKey = localStorage.getItem(ADMIN_API_KEY_STORAGE)
    if (savedApiKey) {
      checkAdminStatus(savedApiKey)
    }
  }, [])

  const checkAdminStatus = async (apiKey: string) => {
    try {
      const response = await fetch(`${API_BASE_URL}/api/admin/check`, {
        method: 'GET',
        headers: {
          'X-API-Key': apiKey,
        },
      })
      const data = await response.json()
      if (data.isAdmin) {
        setIsAdmin(true)
        localStorage.setItem(ADMIN_API_KEY_STORAGE, apiKey)
      } else {
        setIsAdmin(false)
        localStorage.removeItem(ADMIN_API_KEY_STORAGE)
      }
    } catch (error) {
      console.error('Error checking admin status:', error)
      setIsAdmin(false)
    }
  }

  const clearAdmin = () => {
    setIsAdmin(false)
    localStorage.removeItem(ADMIN_API_KEY_STORAGE)
  }

  return (
    <AdminContext.Provider value={{ isAdmin, checkAdminStatus, clearAdmin }}>
      {children}
    </AdminContext.Provider>
  )
}

export function useAdmin() {
  const context = useContext(AdminContext)
  if (context === undefined) {
    throw new Error('useAdmin must be used within an AdminProvider')
  }
  return context
}

