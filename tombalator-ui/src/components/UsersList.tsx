import './UsersList.css'

interface User {
  id: string
  username: string
}

interface UsersListProps {
  users: User[]
  currentUsername: string
}

function UsersList({ users, currentUsername }: UsersListProps) {
  return (
    <div className="users-list">
      <div className="users-list-header">
        <h3>Players ({users.length})</h3>
      </div>
      <div className="users-list-content">
        {users.map((user) => (
          <div
            key={user.id}
            className={`user-item ${
              user.username === currentUsername ? 'current-user' : ''
            }`}
          >
            <div className="user-avatar">
              {user.username.charAt(0).toUpperCase()}
            </div>
            <span className="user-name">
              {user.username}
              {user.username === currentUsername && ' (You)'}
            </span>
          </div>
        ))}
      </div>
    </div>
  )
}

export default UsersList

